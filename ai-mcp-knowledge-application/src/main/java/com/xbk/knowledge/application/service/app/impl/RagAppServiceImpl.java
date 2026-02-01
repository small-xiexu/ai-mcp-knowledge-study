package com.xbk.knowledge.application.service.app.impl;

import com.xbk.knowledge.application.service.app.RagAppService;
import com.xbk.knowledge.application.service.rag.RagTaskProcessor;
import com.xbk.knowledge.application.service.rag.RagVectorStoreService;
import com.xbk.knowledge.domain.model.entity.RagTask;
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
