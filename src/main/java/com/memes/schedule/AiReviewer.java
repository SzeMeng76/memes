package com.memes.schedule;

import static com.memes.util.GsonUtil.extractJsonFromModelOutput;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import com.google.protobuf.util.JsonFormat;
import com.memes.model.pojo.MediaContent;
import com.memes.model.transport.LLMReviewResult;
import com.memes.model.transport.ReviewOutcome;
import com.memes.service.MediaContentService;

import io.github.sashirestela.openai.SimpleOpenAI;
import io.github.sashirestela.openai.domain.chat.ChatRequest;
import io.github.sashirestela.openai.domain.chat.ChatResponse;
import io.github.sashirestela.openai.domain.chat.message.ChatMsg;
import io.github.sashirestela.openai.domain.chat.message.ChatMsgUser;
import io.github.sashirestela.openai.domain.chat.message.ImageUrl;
import io.github.sashirestela.openai.domain.chat.message.ImageUrl.Detail;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Lazy(value = false)
@Profile("prod")
public class AiReviewer {

    final MeterRegistry registry;

    private static String SYS_PROMPT;
    private static final String REVIEW_PROMPT = "请审核这个图片";

    @Value("classpath:prompt.xml")
    private Resource promptResource;

    @Value("${openai.apiKey}")
    private String apiKey;

    @Value("${openai.baseUrl}")
    private String baseUrl;

    @Value("${openai.visionModel}")
    private String visionModel;

    private SimpleOpenAI openAI;
    private final MediaContentService mediaContentService;
    private final ExecutorService reviewExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setName("ai-review-thread");
        return thread;
    });

    public AiReviewer(MeterRegistry registry, MediaContentService mediaContentService) {
        this.registry = registry;
        this.mediaContentService = mediaContentService;
    }

    @PostConstruct
    public void init() throws IOException {
        SYS_PROMPT = StreamUtils.copyToString(promptResource.getInputStream(), StandardCharsets.UTF_8);

        // Initialize OpenAI client with custom base URL support
        openAI = SimpleOpenAI.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .build();

        log.info("OpenAI client initialized with base URL: {}, model: {}", baseUrl, visionModel);
        startReview();
    }

    @PreDestroy
    public void cleanup() {
        reviewExecutor.shutdownNow();
    }

    private void startReview() {
        reviewExecutor.submit(() -> {
            log.info("Starting AI reviewer...");
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    List<MediaContent> mediaContents = mediaContentService.listPendingMediaContent(100);
                    mediaContents
                        .stream()
                        .filter(Objects::nonNull)
                        .filter(mediaContent -> mediaContent.getDataType() == MediaContent.DataType.IMAGE)
                        .forEach(this::processMediaContentReview);
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Error in review process", e);
                }
            }
        });
    }

    /**
     * 调用 OpenAI Vision API 审核图片
     *
     * @param url 图片链接
     * @return LLMReviewResult
     */
    public LLMReviewResult callWithRemoteImage(String url) {
        try {
            log.debug("Calling OpenAI Vision API with URL: {}", url);

            // Create chat request with vision capability
            var chatRequest = ChatRequest.builder()
                .model(visionModel)
                .messages(List.of(
                    ChatMsg.SystemMessage.of(SYS_PROMPT),
                    ChatMsgUser.of(
                        ChatMsgUser.UserContent.of(REVIEW_PROMPT),
                        ChatMsgUser.UserContent.of(ImageUrl.of(url, Detail.AUTO))
                    )
                ))
                .temperature(0.0)
                .maxTokens(1000)
                .build();

            // Call OpenAI API
            ChatResponse response = openAI.chatCompletions()
                .create(chatRequest)
                .join();

            // Handle usage statistics
            var usage = response.getUsage();
            log.info("OpenAI API Usage: prompt_tokens={}, completion_tokens={}, total_tokens={}",
                usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());

            registry.counter("total_token", "model", visionModel).increment(usage.getTotalTokens());
            registry.counter("input_token", "model", visionModel).increment(usage.getPromptTokens());
            registry.counter("output_token", "model", visionModel).increment(usage.getCompletionTokens());
            log.info("Sent LLM Usage Data to metrics.");

            // Extract and parse model output
            String modelOut = response.firstContent();
            String jsonStr = extractJsonFromModelOutput(modelOut);
            log.debug("Raw LLM Output: {}", jsonStr);

            LLMReviewResult.Builder builder = LLMReviewResult.newBuilder();
            JsonFormat.parser().merge(jsonStr, builder);
            return builder.build();

        } catch (Exception e) {
            log.error("Error calling OpenAI Vision API. URL: {}", url, e);
            registry.counter("llm_api_error", "model", visionModel).increment();

            String errorMsg = e.getMessage();
            if (errorMsg != null && (errorMsg.contains("content_policy_violation") || errorMsg.contains("inappropriate"))) {
                registry.counter("llm_inappropriate_content", "model", visionModel).increment();
                return LLMReviewResult.newBuilder()
                    .setOutcome(ReviewOutcome.FLAGGED)
                    .setFailureReason("Content policy violation: " + errorMsg)
                    .build();
            }

            return LLMReviewResult.newBuilder()
                .setOutcome(ReviewOutcome.FLAGGED)
                .setFailureReason("OpenAI API call failed: " + errorMsg)
                .build();
        }
    }

    /**
     * 处理媒体内容审核
     */
    private void processMediaContentReview(MediaContent mediaContent) {
        log.info("开始处理媒体内容：{}", mediaContent.getId());

        if (mediaContent.getDataType() != MediaContent.DataType.IMAGE) {
            log.warn("不支持的媒体类型：{}，跳过审核", mediaContent.getDataType());
            return;
        }

        // 执行图片审核
        log.info("正在审核图片内容：{}", mediaContent.getId());
        LLMReviewResult result = callWithRemoteImage(mediaContent.getDataContent());

        // 记录指标
        registry.counter("llm_review_count", "outcome", result.getOutcome().name()).increment();
        log.info("AI 审核结果：{} - 媒体 ID: {}", result.getOutcome().name(), mediaContent.getId());

        // 更新媒体内容
        mediaContent.setLlmDescription(result.getMediaDescription());
        mediaContent.setRejectionReason(result.getFailureReason());
        mediaContent.setLlmModerationStatus(MediaContent.AiModerationStatus.valueOf(result.getOutcome().name()));
        mediaContentService.updateById(mediaContent);

        // 处理审核结果
        ReviewOutcome outcome = result.getOutcome();
        if (outcome == ReviewOutcome.APPROVED) {
            log.info("媒体内容 {} 通过审核，正在进行批准", mediaContent.getId());
            boolean updateSuccess = mediaContentService.markMediaStatus(mediaContent.getId(), MediaContent.ContentStatus.APPROVED);
            log.info(updateSuccess ? "媒体内容 {} 已成功批准" : "媒体内容 {} 批准失败", mediaContent.getId());
        } else if (outcome == ReviewOutcome.FLAGGED || outcome == ReviewOutcome.REJECTED) {
            log.info("媒体内容 {} 被标记或拒绝，将进行人工审核", mediaContent.getId());
        } else {
            log.error("媒体内容 {} 出现未知审核结果，将进行人工审核", mediaContent.getId());
        }
    }
}
