package com.xbk.knowledge.application.service.app.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.application.service.app.RagAppService;
import com.xbk.knowledge.application.service.rag.RagTaskProcessor;
import com.xbk.knowledge.application.service.rag.RagVectorStoreService;
import com.xbk.knowledge.domain.rag.model.entity.RagTask;
import com.xbk.knowledge.domain.rag.model.valobj.FileProcessError;
import com.xbk.knowledge.domain.rag.adapter.repository.RagTaskRepository;
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
 * @author sxie
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

    /**
     * 查询全部 RAG 标签
     *
     * 为什么：前端展示标签列表用于筛选与选择
     * 入参：无
     * 出参：标签列表
     */
    @Override
    public List<String> listRagTags() {
        return ragVectorStoreService.listTags();
    }

    /**
     * 删除 RAG 标签
     *
     * 为什么：清理标签下的向量数据，释放存储与检索资源
     * 入参：标签名
     * 出参：是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRagTag(String ragTag) {
        int deleted = ragVectorStoreService.deleteByTag(ragTag);
        log.info("删除知识库标签: {}, 删除行数: {}", ragTag, deleted);
        return deleted >= 0;
    }

    /**
     * 统计标签向量数量
     *
     * 为什么：展示知识库规模，评估覆盖范围
     * 入参：标签名
     * 出参：向量数量
     */
    @Override
    public long countByRagTag(String ragTag) {
        return ragVectorStoreService.countByTag(ragTag);
    }

    /**
     * 同步上传文件
     *
     * 为什么：适用于小批量文件的即时入库
     * 入参：标签名、文件列表
     * 出参：是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean uploadFiles(String ragTag, List<MultipartFile> files) {
        if (!StringUtils.hasText(ragTag) || CollectionUtils.isEmpty(files)) {
            return false;
        }
        // 过滤空文件，避免后续处理异常
        List<MultipartFile> validFiles = files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .collect(Collectors.toList());
        if (validFiles.isEmpty()) {
            return false;
        }

        // 提前校验大小与格式，避免进入解析阶段后再失败
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
                // 切分文档并添加标签元数据，以便检索时聚合
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
     * 为什么：大文件或批量导入需要异步处理，避免阻塞接口
     * 入参：标签名、文件列表
     * 出参：任务 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String uploadFilesAsync(String ragTag, List<MultipartFile> files) {
        if (!StringUtils.hasText(ragTag) || CollectionUtils.isEmpty(files)) {
            throw new IllegalArgumentException("标签和文件不能为空");
        }

        // 过滤空文件，避免后续处理异常
        List<MultipartFile> validFiles = files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .collect(Collectors.toList());

        if (validFiles.isEmpty()) {
            throw new IllegalArgumentException("没有有效的文件");
        }

        // 提前校验大小与格式，避免进入异步任务后失败
        for (MultipartFile file : validFiles) {
            String originalName = file.getOriginalFilename();
            if (file.getSize() > MAX_FILE_SIZE_BYTES) {
                throw new IllegalArgumentException("单文件大小超过 30MB: " + originalName);
            }
            if (!isSupportedFile(originalName)) {
                throw new IllegalArgumentException("不支持的文件类型: " + originalName);
            }
        }

        // 落库任务用于进度跟踪与重试
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

        // 交由任务处理器异步执行，避免阻塞请求线程
        ragTaskProcessor.processFilesAsync(taskId, ragTag, validFiles);

        log.info("文件上传任务已创建，taskId: {}, 文件数: {}", taskId, validFiles.size());
        return taskId;
    }

    /**
     * 分析 Git 仓库
     *
     * 为什么：直接从仓库构建知识库，减少手动上传步骤
     * 入参：仓库地址、用户名、Token、标签名
     * 出参：任务 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String analyzeGitRepository(String repoUrl, String userName, String token, String ragTag) {
        String taskId = UUID.randomUUID().toString();
        String resolvedTag = StringUtils.hasText(ragTag) ? ragTag : resolveRepoName(repoUrl);

        // 落库任务用于进度跟踪与状态展示
        RagTask task = RagTask.builder()
                .taskId(taskId)
                .type("GIT")
                .status(RagTaskStatus.PENDING)
                .progress(0)
                .message("任务已提交")
                .ragTag(resolvedTag)
                .build();
        ragTaskRepository.create(task);

        // 交由任务处理器异步执行，避免阻塞请求线程
        ragTaskProcessor.processGitRepository(taskId, repoUrl, userName, token, resolvedTag);
        return taskId;
    }

    /**
     * 查询任务
     *
     * 为什么：支持前端轮询任务状态
     * 入参：任务 ID
     * 出参：任务详情
     */
    @Override
    public RagTask queryTask(String taskId) {
        return ragTaskRepository.findByTaskId(taskId);
    }

    /**
     * 取消任务
     *
     * 为什么：长任务可终止，节省计算资源
     * 入参：任务 ID
     * 出参：是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelTask(String taskId) {
        RagTask task = ragTaskRepository.findByTaskId(taskId);
        if (task == null) {
            return false;
        }
        // 更新任务状态并清理进度信息
        task.setStatus(RagTaskStatus.CANCELLED);
        task.setProgress(0);
        task.setMessage("任务已取消");
        ragTaskRepository.update(task);
        return true;
    }

    /**
     * 分页查询任务
     *
     * 为什么：任务记录可能较多，分页避免接口过大
     * 入参：偏移量、页大小
     * 出参：分页结果
     */
    @Override
    public PageResult<RagTask> queryTaskPage(int offset, int pageSize) {
        List<RagTask> tasks = ragTaskRepository.findPage(offset, pageSize);
        long total = ragTaskRepository.countAll();
        // 修正分页参数，避免非法值导致异常
        int safePageSize = pageSize > 0 ? pageSize : 10;
        int safeOffset = Math.max(offset, 0);
        int pageNum = safeOffset / safePageSize + 1;
        return PageResult.of(tasks, total, pageNum, safePageSize);
    }

    /**
     * 重试任务
     *
     * 为什么：允许失败任务基于失败详情再次执行
     * 入参：任务 ID
     * 出参：新任务 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String retryTask(String taskId) {
        RagTask task = ragTaskRepository.findByTaskId(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }

        // 限制可重试状态，避免错误重试
        if (task.getStatus() != RagTaskStatus.FAILED &&
                task.getStatus() != RagTaskStatus.COMPLETED) {
            throw new IllegalStateException("只有失败或部分成功的任务才能重试");
        }

        // 基于失败详情重试，避免盲目重复处理
        String errorDetails = task.getErrorDetails();
        if (!StringUtils.hasText(errorDetails)) {
            throw new IllegalStateException("任务没有失败详情，无法重试");
        }

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

        // 创建新的任务记录，保留重试链路
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

        /*
         * 约束：目前只创建任务记录，实际重试由外部机制触发
 */
        return newTaskId;
    }

    /**
     * 校验文件扩展名是否支持
     *
     * 为什么：限制解析器范围，避免不可控格式导致解析失败
     * 入参：文件名
     * 出参：是否支持
     */
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

    /**
     * 解析仓库名称作为默认标签
     *
     * 为什么：未传标签时使用仓库名保证任务可识别
     * 入参：仓库地址
     * 出参：解析后的标签名
     */
    private String resolveRepoName(String repoUrl) {
        if (!StringUtils.hasText(repoUrl)) {
            return "unknown";
        }
        String[] parts = repoUrl.split("/");
        String last = parts[parts.length - 1];
        return last.replace(".git", "");
    }
}
