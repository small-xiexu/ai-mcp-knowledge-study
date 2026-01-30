package com.xbk.knowledge.test;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * OpenAI 大模型集成测试类
 * 演示基于 Spring AI 框架调用 OpenAI API（gpt-4o 模型）
 * 包含：同步调用、流式调用、图片识别、RAG 知识库问答等场景
 *
 * @author xiexu
 */
@Slf4j
@SpringBootTest
public class OpenAiTest {

    /**
     * 图片资源，用于多模态测试
     */
    private final Resource imageResource;

    /**
     * OpenAI 聊天模型，由 Spring AI 自动装配
     */
    private final OpenAiChatModel openAiChatModel;

    /**
     * 基于内存的简单向量存储（开发测试用）
     */
    private final SimpleVectorStore simpleVectorStore;

    /**
     * 基于 PostgreSQL 的向量存储（生产环境推荐）
     */
    private final PgVectorStore pgVectorStore;

    /**
     * 文本分割器，将长文本切分为适合嵌入的小块
     */
    private final TokenTextSplitter tokenTextSplitter;

    /**
     * OpenAI API 客户端
     */
    private final OpenAiApi openAiApi;

    @Autowired
    public OpenAiTest(OpenAiChatModel openAiChatModel,
                      @Value("classpath:data/dog.png") Resource imageResource,
                      @Qualifier("openAiSimpleVectorStore") SimpleVectorStore simpleVectorStore,
                      @Qualifier("openAiPgVectorStore") PgVectorStore pgVectorStore,
                      TokenTextSplitter tokenTextSplitter,
                      OpenAiApi openAiApi) {
        this.openAiChatModel = openAiChatModel;
        this.imageResource = imageResource;
        this.simpleVectorStore = simpleVectorStore;
        this.pgVectorStore = pgVectorStore;
        this.tokenTextSplitter = tokenTextSplitter;
        this.openAiApi = openAiApi;
    }

    /**
     * 测试同步调用模型
     * 发送简单数学问题，验证模型基本响应能力
     */
    @Test
    public void test_call() {
        OpenAiChatOptions chatOptions = OpenAiChatOptions
                .builder()
                .model("deepseek-ai/DeepSeek-R1-Distill-Qwen-7B")
                .build();
        Prompt prompt = new Prompt("1+1", chatOptions);
        ChatResponse response = openAiChatModel.call(prompt);

        String responseJson = JSON.toJSONString(response);
        log.info("测试结果(call):{}", responseJson);
    }

    /**
     * 测试多模态能力（图片识别）
     * 发送图片并要求模型描述图片内容
     * GPT-4o 支持视觉理解能力
     */
    @Test
    public void test_call_images() {
        // 构建包含图片的用户消息（使用 Builder 模式）
        MimeType mimeType = MimeType.valueOf(MimeTypeUtils.IMAGE_PNG_VALUE);
        Media imageMedia = new Media(mimeType, imageResource);
        UserMessage userMessage = UserMessage.builder()
                .text("请描述这张图片的主要内容，并说明图中物品的可能用途。")
                .media(imageMedia)
                .build();

        OpenAiChatOptions chatOptions = OpenAiChatOptions
                .builder()
                .model("gpt-4o")
                .build();
        Prompt prompt = new Prompt(userMessage, chatOptions);
        ChatResponse response = openAiChatModel.call(prompt);

        String responseJson = JSON.toJSONString(response);
        log.info("测试结果(images):{}", responseJson);
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
        OpenAiChatOptions chatOptions = OpenAiChatOptions
                .builder()
                .model("gpt-4o")
                .build();
        Prompt prompt = new Prompt("1+1", chatOptions);
        Flux<ChatResponse> stream = openAiChatModel.stream(prompt);

        // 订阅响应流：逐块处理、错误处理、完成回调
        Consumer<ChatResponse> responseConsumer = chatResponse -> {
            AssistantMessage output = chatResponse
                    .getResult()
                    .getOutput();
            String outputJson = JSON.toJSONString(output);
            log.info("测试结果(stream): {}", outputJson);
        };
        Consumer<Throwable> errorConsumer = Throwable::printStackTrace;
        Runnable completionHandler = () -> {
            countDownLatch.countDown();
            log.info("测试结果(stream): done!");
        };
        stream.subscribe(
                responseConsumer,
                errorConsumer,
                completionHandler
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
        Consumer<Document> metadataAppender = doc -> doc
                .getMetadata()
                .put("knowledge", "知识库名称v2");
        documents.forEach(metadataAppender);
        documentSplitterList.forEach(metadataAppender);

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
        String documentsCollectors = "";
        if (documents != null) {
            Collector<CharSequence, ?, String> joiningCollector = Collectors.joining();
            Function<Document, CharSequence> textMapper = Document::getText;
            documentsCollectors = documents
                    .stream()
                    .map(textMapper)
                    .collect(joiningCollector);
        }

        // 4. 使用模板创建系统消息，将文档内容填充到 {documents} 占位符
        SystemPromptTemplate promptTemplate = new SystemPromptTemplate(SYSTEM_PROMPT);
        Map<String, Object> promptVariables = Map.of("documents", documentsCollectors);
        Message ragMessage = promptTemplate.createMessage(promptVariables);

        // 5. 组装消息列表：用户问题 + 系统提示（含检索上下文）
        ArrayList<Message> messages = new ArrayList<>();
        UserMessage userMessage = new UserMessage(message);
        messages.add(userMessage);
        messages.add(ragMessage);

        // 6. 调用模型生成最终答案
        OpenAiChatOptions chatOptions = OpenAiChatOptions
                .builder()
                .model("gpt-4o")
                .build();
        Prompt prompt = new Prompt(messages, chatOptions);
        ChatResponse chatResponse = openAiChatModel.call(prompt);

        String responseJson = JSON.toJSONString(chatResponse);
        log.info("测试结果:{}", responseJson);
    }

    @Test
    public void test_() {
    }

}
