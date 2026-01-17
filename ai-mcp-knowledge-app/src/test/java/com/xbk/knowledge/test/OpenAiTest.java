package com.xbk.knowledge.test;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.Media;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * OpenAI 大模型集成测试类
 * 演示基于 Spring AI 框架调用 OpenAI API（gpt-4o 模型）
 * 包含：同步调用、流式调用、图片识别、RAG 知识库问答等场景
 *
 * @author xiexu
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class OpenAiTest {

    /**
     * 图片资源，用于多模态测试
     */
    @Value("classpath:data/dog.png")
    private Resource imageResource;

    /**
     * OpenAI 聊天模型，由 Spring AI 自动装配
     */
    @Autowired
    private OpenAiChatModel openAiChatModel;

    /**
     * 基于内存的简单向量存储（开发测试用）
     */
    @jakarta.annotation.Resource(name = "openAiSimpleVectorStore")
    private SimpleVectorStore simpleVectorStore;

    /**
     * 基于 PostgreSQL 的向量存储（生产环境推荐）
     */
    @jakarta.annotation.Resource(name = "openAiPgVectorStore")
    private PgVectorStore pgVectorStore;

    /**
     * 文本分割器，将长文本切分为适合嵌入的小块
     */
    @jakarta.annotation.Resource
    private TokenTextSplitter tokenTextSplitter;

    /**
     * OpenAI API 客户端
     */
    @jakarta.annotation.Resource
    private OpenAiApi openAiApi;

    /**
     * 测试同步调用模型
     * 发送简单数学问题，验证模型基本响应能力
     */
    @Test
    public void test_call() {
        ChatResponse response = openAiChatModel.call(new Prompt(
                "1+1",
                OpenAiChatOptions.builder()
                        .model("gpt-4o")
                        .build()));

        log.info("测试结果(call):{}", JSON.toJSONString(response));
    }

    /**
     * 测试多模态能力（图片识别）
     * 发送图片并要求模型描述图片内容
     * GPT-4o 支持视觉理解能力
     */
    @Test
    public void test_call_images() {
        // 构建包含图片的用户消息
        UserMessage userMessage = new UserMessage("请描述这张图片的主要内容，并说明图中物品的可能用途。",
                new Media(MimeType.valueOf(MimeTypeUtils.IMAGE_PNG_VALUE),
                        imageResource));

        ChatResponse response = openAiChatModel.call(new Prompt(
                userMessage,
                OpenAiChatOptions.builder()
                        .model("gpt-4o")
                        .build()));

        log.info("测试结果(images):{}", JSON.toJSONString(response));
    }

    /**
     * 测试流式调用模型
     * 使用 Reactor Flux 实现流式响应，适用于打字机效果展示
     * CountDownLatch 用于等待异步流完成
     */
    @Test
    public void test_stream() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        // 发起流式请求，返回 Flux 响应流
        Flux<ChatResponse> stream = openAiChatModel.stream(new Prompt(
                "1+1",
                OpenAiChatOptions.builder()
                        .model("gpt-4o")
                        .build()));

        // 订阅响应流：逐块处理、错误处理、完成回调
        stream.subscribe(
                chatResponse -> {
                    AssistantMessage output = chatResponse.getResult().getOutput();
                    log.info("测试结果(stream): {}", JSON.toJSONString(output));
                },
                Throwable::printStackTrace,
                () -> {
                    countDownLatch.countDown();
                    log.info("测试结果(stream): done!");
                }
        );

        // 阻塞主线程，等待流式响应完成
        countDownLatch.await();
    }

    /**
     * 上传文档到向量数据库
     * 流程：读取文档 -> 分词切块 -> 添加元数据 -> 存入 PgVector
     * 这是 RAG（检索增强生成）的数据准备阶段
     */
    @Test
    public void upload() {
        // 1. 使用 Tika 读取文档（支持多种格式：txt、pdf、docx 等）
        TikaDocumentReader reader = new TikaDocumentReader("./data/file.txt");

        List<Document> documents = reader.get();

        // 2. 使用默认分割器进行文本切块
        List<Document> documentSplitterList = tokenTextSplitter.apply(documents);

        // 3. 为文档添加元数据标签，便于后续按知识库筛选
        documents.forEach(doc -> doc.getMetadata().put("knowledge", "知识库名称v2"));
        documentSplitterList.forEach(doc -> doc.getMetadata().put("knowledge", "知识库名称v2"));

        // 4. 将切块后的文档写入 PostgreSQL 向量数据库（注释掉避免重复写入）
//        pgVectorStore.accept(documentSplitterList);

        log.info("上传完成");
    }

    /**
     * RAG 知识库问答测试
     * 流程：用户提问 -> 向量检索相关文档 -> 构建带上下文的 Prompt -> 调用模型生成答案
     * 这是典型的 RAG（Retrieval-Augmented Generation）实现
     */
    @Test
    public void chat() {
        String message = "王大瓜今年几岁";

        // 系统提示词模板：指导模型基于检索到的文档回答问题
        String SYSTEM_PROMPT = """
                Use the information from the DOCUMENTS section to provide accurate answers but act as if you knew this information innately.
                If unsure, simply state that you don't know.
                Another thing you need to note is that your reply must be in Chinese!
                DOCUMENTS:
                    {documents}
                """;

        // 1. 构建向量搜索请求：查询语句、返回 Top5、按知识库过滤
        SearchRequest request = SearchRequest.builder()
                .query(message)
                .topK(5)
                .filterExpression("knowledge == '知识库名称v2'")
                .build();

        // 2. 执行向量相似度搜索，检索最相关的文档片段
        List<Document> documents = pgVectorStore.similaritySearch(request);

        // 3. 将检索到的文档内容拼接为上下文字符串（空值保护）
        String documentsCollectors = null == documents ? "" : documents.stream().map(Document::getText).collect(Collectors.joining());

        // 4. 使用模板创建系统消息，将文档内容填充到 {documents} 占位符
        Message ragMessage = new SystemPromptTemplate(SYSTEM_PROMPT).createMessage(Map.of("documents", documentsCollectors));

        // 5. 组装消息列表：用户问题 + 系统提示（含检索上下文）
        ArrayList<Message> messages = new ArrayList<>();
        messages.add(new UserMessage(message));
        messages.add(ragMessage);

        // 6. 调用模型生成最终答案
        ChatResponse chatResponse = openAiChatModel.call(new Prompt(
                messages,
                OpenAiChatOptions.builder()
                        .model("gpt-4o")
                        .build()));

        log.info("测试结果:{}", JSON.toJSONString(chatResponse));
    }

    @Test
    public void test_() {
    }

}
