package com.xbk.knowledge.application.service.rag;

import com.xbk.knowledge.domain.model.entity.RagTask;
import com.xbk.knowledge.domain.repository.RagTaskRepository;
import com.xbk.knowledge.types.enums.RagTaskStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.PathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RAG 任务处理器
 * 负责异步处理 Git 仓库解析任务
 *
 * 职责：异步任务执行与进度更新
 * @author xiexu
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagTaskProcessor {

    private static final int MAX_FILE_BYTES = 1024 * 1024;

    private final RagVectorStoreService ragVectorStoreService;
    private final RagTaskRepository ragTaskRepository;
    private final TokenTextSplitter tokenTextSplitter;

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
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
