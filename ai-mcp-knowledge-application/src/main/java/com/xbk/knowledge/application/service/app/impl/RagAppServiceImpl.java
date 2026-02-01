package com.xbk.knowledge.application.service.app.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.application.service.app.RagAppService;
import com.xbk.knowledge.application.service.rag.RagTaskProcessor;
import com.xbk.knowledge.application.service.rag.RagVectorStoreService;
import com.xbk.knowledge.domain.model.entity.RagTask;
import com.xbk.knowledge.domain.model.vo.FileProcessError;
import com.xbk.knowledge.domain.repository.RagTaskRepository;
import com.xbk.knowledge.types.common.PageResult;
import com.xbk.knowledge.types.enums.RagTaskStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * RAG 应用服务实现
 * 负责知识库管理与任务编排
 *
 * 职责：应用层用例实现，用于协调领域能力
 * @author xiexu
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagAppServiceImpl implements RagAppService {

    private static final long MAX_FILE_SIZE_BYTES = 30L * 1024 * 1024;
    private static final List<String> SUPPORTED_EXTENSIONS = Arrays.asList("pdf", "docx", "md", "txt", "sql");

    private final RagVectorStoreService ragVectorStoreService;
    private final RagTaskRepository ragTaskRepository;
    private final RagTaskProcessor ragTaskProcessor;
    private final TokenTextSplitter tokenTextSplitter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<String> listRagTags() {
        return ragVectorStoreService.listTags();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRagTag(String ragTag) {
        int deleted = ragVectorStoreService.deleteByTag(ragTag);
        log.info("删除知识库标签: {}, 删除行数: {}", ragTag, deleted);
        return deleted >= 0;
    }

    @Override
    public long countByRagTag(String ragTag) {
        return ragVectorStoreService.countByTag(ragTag);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean uploadFiles(String ragTag, List<MultipartFile> files) {
        if (!StringUtils.hasText(ragTag) || CollectionUtils.isEmpty(files)) {
            return false;
        }
        List<MultipartFile> validFiles = files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .collect(Collectors.toList());
        if (validFiles.isEmpty()) {
            return false;
        }

        for (MultipartFile file : validFiles) {
            String originalName = file.getOriginalFilename();
            if (file.getSize() > MAX_FILE_SIZE_BYTES) {
                throw new IllegalArgumentException("单文件大小超过 30MB: " + originalName);
            }
            if (!isSupportedFile(originalName)) {
                throw new IllegalArgumentException("不支持的文件类型: " + originalName);
            }
        }

        for (MultipartFile file : validFiles) {
            File tempFile = null;
            try {
                tempFile = File.createTempFile("rag-upload-", ".tmp");
                file.transferTo(tempFile);
                TikaDocumentReader reader = new TikaDocumentReader(tempFile.getAbsolutePath());
                List<Document> documents = reader.get();
                if (CollectionUtils.isEmpty(documents)) {
                    continue;
                }
                List<Document> splitDocuments = tokenTextSplitter.apply(documents);
                documents.forEach(doc -> doc.getMetadata().put("knowledge", ragTag));
                splitDocuments.forEach(doc -> doc.getMetadata().put("knowledge", ragTag));
                ragVectorStoreService.saveDocuments(splitDocuments);
            } catch (IOException e) {
                log.error("上传文件失败: {}", file.getOriginalFilename(), e);
                throw new RuntimeException("上传文件失败", e);
            } finally {
                if (tempFile != null && tempFile.exists()) {
                    boolean deleted = tempFile.delete();
                    if (!deleted) {
                        log.warn("临时文件删除失败: {}", tempFile.getAbsolutePath());
                    }
                }
            }
        }

        return true;
    }

    /**
     * 异步上传文件（支持进度跟踪）
     *
     * @param ragTag 知识库标签
     * @param files 文件列表
     * @return 任务 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String uploadFilesAsync(String ragTag, List<MultipartFile> files) {
        if (!StringUtils.hasText(ragTag) || CollectionUtils.isEmpty(files)) {
            throw new IllegalArgumentException("标签和文件不能为空");
        }

        // 1. 验证文件
        List<MultipartFile> validFiles = files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .collect(Collectors.toList());

        if (validFiles.isEmpty()) {
            throw new IllegalArgumentException("没有有效的文件");
        }

        for (MultipartFile file : validFiles) {
            String originalName = file.getOriginalFilename();
            if (file.getSize() > MAX_FILE_SIZE_BYTES) {
                throw new IllegalArgumentException("单文件大小超过 30MB: " + originalName);
            }
            if (!isSupportedFile(originalName)) {
                throw new IllegalArgumentException("不支持的文件类型: " + originalName);
            }
        }

        // 2. 创建任务
        String taskId = UUID.randomUUID().toString();
        RagTask task = RagTask.builder()
                .taskId(taskId)
                .type("FILE_UPLOAD")
                .status(RagTaskStatus.PENDING)
                .progress(0)
                .message("任务已提交，共 " + validFiles.size() + " 个文件")
                .ragTag(ragTag)
                .build();
        ragTaskRepository.create(task);

        // 3. 异步处理
        ragTaskProcessor.processFilesAsync(taskId, ragTag, validFiles);

        log.info("文件上传任务已创建，taskId: {}, 文件数: {}", taskId, validFiles.size());
        return taskId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String analyzeGitRepository(String repoUrl, String userName, String token, String ragTag) {
        String taskId = UUID.randomUUID().toString();
        String resolvedTag = StringUtils.hasText(ragTag) ? ragTag : resolveRepoName(repoUrl);

        RagTask task = RagTask.builder()
                .taskId(taskId)
                .type("GIT")
                .status(RagTaskStatus.PENDING)
                .progress(0)
                .message("任务已提交")
                .ragTag(resolvedTag)
                .build();
        ragTaskRepository.create(task);

        ragTaskProcessor.processGitRepository(taskId, repoUrl, userName, token, resolvedTag);
        return taskId;
    }

    @Override
    public RagTask queryTask(String taskId) {
        return ragTaskRepository.findByTaskId(taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelTask(String taskId) {
        RagTask task = ragTaskRepository.findByTaskId(taskId);
        if (task == null) {
            return false;
        }
        task.setStatus(RagTaskStatus.CANCELLED);
        task.setProgress(0);
        task.setMessage("任务已取消");
        ragTaskRepository.update(task);
        return true;
    }

    @Override
    public PageResult<RagTask> queryTaskPage(int offset, int pageSize) {
        List<RagTask> tasks = ragTaskRepository.findPage(offset, pageSize);
        long total = ragTaskRepository.countAll();
        int safePageSize = pageSize > 0 ? pageSize : 10;
        int safeOffset = Math.max(offset, 0);
        int pageNum = safeOffset / safePageSize + 1;
        return PageResult.of(tasks, total, pageNum, safePageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String retryTask(String taskId) {
        RagTask task = ragTaskRepository.findByTaskId(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }

        // 只有失败或部分成功的任务才能重试
        if (task.getStatus() != RagTaskStatus.FAILED &&
                task.getStatus() != RagTaskStatus.COMPLETED) {
            throw new IllegalStateException("只有失败或部分成功的任务才能重试");
        }

        // 检查是否有失败详情
        String errorDetails = task.getErrorDetails();
        if (!StringUtils.hasText(errorDetails)) {
            throw new IllegalStateException("任务没有失败详情，无法重试");
        }

        // 解析失败详情
        List<FileProcessError> errors;
        try {
            errors = objectMapper.readValue(errorDetails,
                    new TypeReference<List<FileProcessError>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("解析失败详情失败", e);
        }

        if (errors.isEmpty()) {
            throw new IllegalStateException("没有失败的文件需要重试");
        }

        // 创建新的重试任务
        String newTaskId = UUID.randomUUID().toString();
        int newRetryCount = (task.getRetryCount() == null ? 0 : task.getRetryCount()) + 1;

        RagTask newTask = RagTask.builder()
                .taskId(newTaskId)
                .type(task.getType())
                .status(RagTaskStatus.PENDING)
                .progress(0)
                .message("重试任务已提交，共 " + errors.size() + " 个文件")
                .ragTag(task.getRagTag())
                .retryCount(newRetryCount)
                .parentTaskId(taskId)
                .build();
        ragTaskRepository.create(newTask);

        log.info("任务重试已创建，原任务: {}, 新任务: {}, 失败文件数: {}, 重试次数: {}",
                taskId, newTaskId, errors.size(), newRetryCount);

        // 注意：这里简化处理，实际应该从失败详情中恢复文件并重试
        // 由于 MultipartFile 无法从错误详情中恢复，这里只是创建任务记录
        // 实际使用时需要配合定时任务或其他机制来处理

        return newTaskId;
    }

    private boolean isSupportedFile(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return false;
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return false;
        }
        String ext = fileName.substring(dot + 1).toLowerCase();
        return SUPPORTED_EXTENSIONS.contains(ext);
    }

    private String resolveRepoName(String repoUrl) {
        if (!StringUtils.hasText(repoUrl)) {
            return "unknown";
        }
        String[] parts = repoUrl.split("/");
        String last = parts[parts.length - 1];
        return last.replace(".git", "");
    }
}
