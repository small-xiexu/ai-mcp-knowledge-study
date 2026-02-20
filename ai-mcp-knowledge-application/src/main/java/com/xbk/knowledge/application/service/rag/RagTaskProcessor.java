package com.xbk.knowledge.application.service.rag;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbk.knowledge.domain.rag.model.entity.RagTask;
import com.xbk.knowledge.domain.rag.model.valobj.FileProcessError;
import com.xbk.knowledge.domain.rag.adapter.repository.RagTaskRepository;
import com.xbk.knowledge.types.enums.RagTaskStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.PathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RAG 任务处理器
 * 负责异步处理 Git 仓库解析任务
 *
 * 职责：异步任务执行与进度更新
 * @author sxie
 */
@Slf4j
@Component
public class RagTaskProcessor {

    private static final int MAX_FILE_BYTES = 1024 * 1024;

    private final RagVectorStoreService ragVectorStoreService;
    private final RagTaskRepository ragTaskRepository;
    private final TokenTextSplitter tokenTextSplitter;
    private final ThreadPoolTaskExecutor ragTaskExecutor;
    private final ObjectMapper objectMapper;

    public RagTaskProcessor(
            RagVectorStoreService ragVectorStoreService,
            RagTaskRepository ragTaskRepository,
            TokenTextSplitter tokenTextSplitter,
            @Qualifier("ragTaskExecutor") ThreadPoolTaskExecutor ragTaskExecutor) {
        this.ragVectorStoreService = ragVectorStoreService;
        this.ragTaskRepository = ragTaskRepository;
        this.tokenTextSplitter = tokenTextSplitter;
        this.ragTaskExecutor = ragTaskExecutor;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }

    /**
     * 异步处理 Git 仓库任务
     *
     * @param taskId 任务ID
     * @param repoUrl 仓库地址
     * @param userName 用户名
     * @param token 访问令牌
     * @param ragTag 标签
     */
    @Async
    public void processGitRepository(String taskId, String repoUrl, String userName, String token, String ragTag) {
        String localPath = "./git-cloned-repo/" + UUID.randomUUID();
        Git git = null;
        try {
            updateTask(taskId, RagTaskStatus.PROCESSING, 5, "正在连接远程仓库...");

            File repoDir = new File(localPath);
            if (repoDir.exists()) {
                deleteDirectory(repoDir);
            }
            updateTask(taskId, RagTaskStatus.PROCESSING, 10, "正在克隆代码...");

            if (isCancelled(taskId)) {
                updateTask(taskId, RagTaskStatus.CANCELLED, 0, "任务已取消");
                return;
            }

            CloneCommand cloneCommand = Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(repoDir);
            if (StringUtils.hasText(userName) || StringUtils.hasText(token)) {
                cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(userName, token));
            }
            git = cloneCommand.call();

            updateTask(taskId, RagTaskStatus.PROCESSING, 30, "克隆完成，开始扫描文件...");

            AtomicInteger totalFiles = new AtomicInteger(0);
            Files.walkFileTree(repoDir.toPath(), new SimpleFileVisitor<Path>() {
                /**
                 * visitFile。
                 *
                 * @param file 参数
                 * @param attrs 参数
                 * @return 返回结果
                 */
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (isCancelled(taskId)) {
                        return FileVisitResult.TERMINATE;
                    }
                    if (isValidFile(file)) {
                        totalFiles.incrementAndGet();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            if (isCancelled(taskId)) {
                updateTask(taskId, RagTaskStatus.CANCELLED, 0, "任务已取消");
                return;
            }

            int total = totalFiles.get() > 0 ? totalFiles.get() : 1;
            updateTask(taskId, RagTaskStatus.PROCESSING, 35, "扫描完成，共 " + total + " 个文件，开始解析...");

            AtomicInteger current = new AtomicInteger(0);
            Files.walkFileTree(repoDir.toPath(), new SimpleFileVisitor<Path>() {
                /**
                 * 逐文件解析并写入向量库。
                 *
                 * @param file 文件路径
                 * @param attrs 文件属性
                 * @return 遍历控制结果
                 * @throws IOException IO 异常
                 */
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (isCancelled(taskId)) {
                        return FileVisitResult.TERMINATE;
                    }
                    if (!isValidFile(file)) {
                        return FileVisitResult.CONTINUE;
                    }
                    int c = current.incrementAndGet();
                    int progress = 35 + (int) ((c * 60.0) / total);
                    if (c % 5 == 0 || progress % 10 == 0) {
                        updateTask(taskId, RagTaskStatus.PROCESSING, progress, "正在解析: " + file.getFileName());
                    }

                    try {
                        TikaDocumentReader reader = new TikaDocumentReader(new PathResource(file));
                        List<Document> documents = reader.get();
                        if (CollectionUtils.isEmpty(documents)) {
                            return FileVisitResult.CONTINUE;
                        }
                        List<Document> splitDocuments = tokenTextSplitter.apply(documents);
                        documents.forEach(doc -> doc.getMetadata().put("knowledge", ragTag));
                        splitDocuments.forEach(doc -> doc.getMetadata().put("knowledge", ragTag));
                        ragVectorStoreService.saveDocuments(splitDocuments);
                    } catch (Exception e) {
                        log.error("解析文件失败: {}", file, e);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            if (isCancelled(taskId)) {
                updateTask(taskId, RagTaskStatus.CANCELLED, 0, "任务已取消");
                return;
            }

            updateTask(taskId, RagTaskStatus.COMPLETED, 100, "分析完成");
        } catch (Exception e) {
            log.error("Git 仓库解析失败, taskId: {}", taskId, e);
            updateTask(taskId, RagTaskStatus.FAILED, 0, "任务失败: " + e.getMessage());
        } finally {
            if (git != null) {
                git.close();
            }
            try {
                deleteDirectory(new File(localPath));
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 异步处理文件上传任务（支持并行）
     *
     * @param taskId 任务 ID
     * @param ragTag 知识库标签
     * @param files 文件列表
     */
    @Async
    public void processFilesAsync(String taskId, String ragTag, List<MultipartFile> files) {
        try {
            updateTask(taskId, RagTaskStatus.PROCESSING, 5, "开始处理文件...");

            int totalFiles = files.size();
            AtomicInteger processedFiles = new AtomicInteger(0);
            AtomicInteger failedFiles = new AtomicInteger(0);

            // 记录失败的文件
            List<FileProcessError> errors = Collections.synchronizedList(new ArrayList<>());

            // 并行处理文件
            List<CompletableFuture<Void>> futures = files.stream()
                    .map(file -> CompletableFuture.runAsync(() -> {
                        try {
                            // 检查是否取消
                            if (isCancelled(taskId)) {
                                return;
                            }

                            // 处理文件（带重试）
                            processFileWithRetry(file, ragTag);

                            // 更新进度（节流：每 10 个文件或进度变化 10% 时更新）
                            int processed = processedFiles.incrementAndGet();
                            int progress = 5 + (int) ((processed * 90.0) / totalFiles);
                            if (processed % 10 == 0 || progress % 10 == 0) {
                                updateTask(taskId, RagTaskStatus.PROCESSING, progress,
                                        String.format("已处理: %d/%d", processed, totalFiles));
                            }

                        } catch (Exception e) {
                            // 记录失败信息
                            int failed = failedFiles.incrementAndGet();
                            errors.add(FileProcessError.builder()
                                    .fileName(file.getOriginalFilename())
                                    .errorMessage(e.getMessage())
                                    .stackTrace(getStackTrace(e))
                                    .occurredAt(LocalDateTime.now())
                                    .retryCount(3)
                                    .build());

                            log.error("文件处理失败（已重试 3 次）: {}", file.getOriginalFilename(), e);
                        }
                    }, ragTaskExecutor))
                    .toList();

            // 等待所有任务完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // 检查是否取消
            if (isCancelled(taskId)) {
                updateTask(taskId, RagTaskStatus.CANCELLED, 0, "任务已取消");
                return;
            }

            // 更新最终状态
            int processed = processedFiles.get();
            int failed = failedFiles.get();

            if (failed == 0) {
                // 全部成功
                updateTask(taskId, RagTaskStatus.COMPLETED, 100,
                        String.format("处理完成，成功: %d", processed));
            } else if (processed > 0) {
                // 部分成功
                updateTask(taskId, RagTaskStatus.COMPLETED, 100,
                        String.format("处理完成，成功: %d, 失败: %d", processed, failed));

                // 保存失败详情
                saveFailureDetails(taskId, errors);
            } else {
                // 全部失败
                updateTask(taskId, RagTaskStatus.FAILED, 0,
                        String.format("处理失败，失败: %d", failed));

                // 保存失败详情
                saveFailureDetails(taskId, errors);
            }

        } catch (Exception e) {
            log.error("文件上传任务失败, taskId: {}", taskId, e);
            updateTask(taskId, RagTaskStatus.FAILED, 0, "任务失败: " + e.getMessage());
        }
    }

    /**
     * 处理单个文件（支持自动重试）
     *
     * @param file 文件
     * @param ragTag 标签
     * @throws IOException 处理失败
     */
    private void processFileWithRetry(MultipartFile file, String ragTag) throws IOException {
        int maxRetries = 3;
        int retryCount = 0;
        Exception lastException = null;

        while (retryCount < maxRetries) {
            try {
                // 处理文件
                processFile(file, ragTag);

                // 成功，记录日志
                if (retryCount > 0) {
                    log.info("文件处理成功: {}, 重试次数: {}",
                            file.getOriginalFilename(), retryCount);
                }
                return;

            } catch (Exception e) {
                lastException = e;
                retryCount++;

                if (retryCount < maxRetries) {
                    // 计算退避时间（指数退避：2s、4s、8s）
                    long backoffMs = (long) Math.pow(2, retryCount) * 1000;

                    log.warn("文件处理失败，将在 {} 秒后重试 {}/{}: {}",
                            backoffMs / 1000, retryCount, maxRetries,
                            file.getOriginalFilename(), e.getMessage());

                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("重试被中断", ie);
                    }
                } else {
                    log.error("文件处理失败，已达到最大重试次数 {}: {}",
                            maxRetries, file.getOriginalFilename(), e.getMessage());
                }
            }
        }

        // 所有重试都失败
        throw new RuntimeException(
                String.format("文件处理失败，已重试 %d 次: %s",
                        maxRetries, file.getOriginalFilename()),
                lastException
        );
    }

    /**
     * 处理单个文件
     */
    private void processFile(MultipartFile file, String ragTag) throws IOException {
        File tempFile = null;
        try {
            // 创建临时文件
            tempFile = File.createTempFile("rag-upload-", ".tmp");
            file.transferTo(tempFile);

            // 解析文档
            TikaDocumentReader reader = new TikaDocumentReader(tempFile.getAbsolutePath());
            List<Document> documents = reader.get();

            if (CollectionUtils.isEmpty(documents)) {
                log.warn("文件解析结果为空: {}", file.getOriginalFilename());
                return;
            }

            // 分块
            List<Document> splitDocuments = tokenTextSplitter.apply(documents);

            // 添加元数据
            documents.forEach(doc -> doc.getMetadata().put("knowledge", ragTag));
            splitDocuments.forEach(doc -> doc.getMetadata().put("knowledge", ragTag));

            // 保存到向量库
            ragVectorStoreService.saveDocuments(splitDocuments);

            log.debug("文件处理成功: {}, 分块数: {}", file.getOriginalFilename(), splitDocuments.size());

        } finally {
            // 清理临时文件
            if (tempFile != null && tempFile.exists()) {
                boolean deleted = tempFile.delete();
                if (!deleted) {
                    log.warn("临时文件删除失败: {}", tempFile.getAbsolutePath());
                }
            }
        }
    }

    /**
     * 保存失败详情
     */
    private void saveFailureDetails(String taskId, List<FileProcessError> errors) {
        if (errors.isEmpty()) {
            return;
        }

        try {
            // 将失败信息序列化为 JSON
            String errorJson = objectMapper.writeValueAsString(errors);

            // 保存到任务表的扩展字段
            RagTask task = ragTaskRepository.findByTaskId(taskId);
            if (task != null) {
                task.setErrorDetails(errorJson);
                ragTaskRepository.update(task);
            }
        } catch (JsonProcessingException e) {
            log.error("序列化失败详情失败, taskId: {}", taskId, e);
        }
    }

    /**
     * 获取异常堆栈信息
     */
    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    private void updateTask(String taskId, RagTaskStatus status, int progress, String message) {
        RagTask task = ragTaskRepository.findByTaskId(taskId);
        if (task == null) {
            return;
        }
        task.setStatus(status);
        task.setProgress(progress);
        task.setMessage(message);
        ragTaskRepository.update(task);
    }

    private boolean isCancelled(String taskId) {
        RagTask task = ragTaskRepository.findByTaskId(taskId);
        return task != null && task.getStatus() == RagTaskStatus.CANCELLED;
    }

    private boolean isValidFile(Path file) {
        String path = file.toString();
        if (path.contains(File.separator + ".git" + File.separator) ||
                path.contains(File.separator + "target" + File.separator) ||
                path.contains(File.separator + "build" + File.separator) ||
                path.contains(File.separator + "node_modules" + File.separator) ||
                path.contains(File.separator + "dist" + File.separator) ||
                path.contains(File.separator + ".idea" + File.separator) ||
                path.contains(File.separator + "logs" + File.separator) ||
                path.contains(File.separator + ".gradle" + File.separator)) {
            return false;
        }
        try {
            long size = Files.size(file);
            if (size == 0 || size > MAX_FILE_BYTES) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
        return path.endsWith(".java") ||
                path.endsWith(".xml") ||
                path.endsWith(".yml") ||
                path.endsWith(".yaml") ||
                path.endsWith(".properties") ||
                path.endsWith(".sql") ||
                path.endsWith(".md") ||
                path.endsWith(".txt") ||
                path.endsWith(".json") ||
                path.endsWith(".js") ||
                path.endsWith(".ts");
    }

    private void deleteDirectory(File directory) throws IOException {
        if (directory == null || !directory.exists()) {
            return;
        }
        Files.walkFileTree(directory.toPath(), new SimpleFileVisitor<Path>() {
            /**
             * 删除遍历到的文件。
             *
             * @param file 文件路径
             * @param attrs 文件属性
             * @return 遍历控制结果
             * @throws IOException IO 异常
             */
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            /**
             * 删除遍历完成的目录。
             *
             * @param dir 目录路径
             * @param exc 异常信息
             * @return 遍历控制结果
             * @throws IOException IO 异常
             */
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
