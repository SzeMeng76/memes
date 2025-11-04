package com.memes.schedule;

import static com.memes.util.GsonUtil.extractJsonFromModelOutput;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StreamUtils;

import com.google.protobuf.util.JsonFormat;
import com.memes.model.pojo.MediaContent;
import com.memes.model.transport.LLMReviewResult;
import com.memes.model.transport.ReviewOutcome;
import com.memes.service.MediaContentService;

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
    final ChatModel chatModel;

    private static String SYS_PROMPT;
    private static final String REVIEW_PROMPT = "请审核这个图片";

    @Value("classpath:prompt.xml")
    private Resource promptResource;

    @Value("${ai.vision-model:${spring.ai.openai.chat.options.model}}")
    private String model;

    private final MediaContentService mediaContentService;
    private final ExecutorService reviewExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setName("ai-review-thread");
        return thread;
    });

    public AiReviewer(MeterRegistry registry, ChatModel chatModel, MediaContentService mediaContentService) {
        this.registry = registry;
        this.chatModel = chatModel;
        this.mediaContentService = mediaContentService;
    }

    @PostConstruct
    public void init() throws IOException {
        SYS_PROMPT = StreamUtils.copyToString(promptResource.getInputStream(), StandardCharsets.UTF_8);
        log.info("Spring AI OpenAI ChatModel initialized with model: {}", model);
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
     * 调用 Spring AI OpenAI Vision API 审核图片
     *
     * @param url 图片链接
     * @return LLMReviewResult
     */
    public LLMReviewResult callWithRemoteImage(String url) {
        try {
            log.debug("Calling Spring AI OpenAI Vision API with URL: {}", url);

            // Create media object from image URL
            UrlResource imageResource = new UrlResource(new URL(url));
            Media media = new Media(MimeTypeUtils.IMAGE_PNG, imageResource);

            // Create system and user messages
            var systemMessage = new org.springframework.ai.chat.messages.SystemMessage(SYS_PROMPT);
            var userMessage = org.springframework.ai.chat.messages.UserMessage.builder()
                .text(REVIEW_PROMPT)
                .media(media)
                .build();

            // Create prompt with options
            // GPT-5 series models don't support custom temperature or maxTokens
            var optionsBuilder = OpenAiChatOptions.builder().model(model);

            if (!model.startsWith("gpt-5") && !model.startsWith("o1") && !model.startsWith("o3")) {
                // Only non-reasoning models support these parameters
                optionsBuilder.temperature(0.0).maxTokens(1000);
            }

            var chatOptions = optionsBuilder.build();

            var prompt = new Prompt(List.of(systemMessage, userMessage), chatOptions);

            // Call OpenAI API
            var response = chatModel.call(prompt);

            // Handle usage statistics
            var usage = response.getMetadata().getUsage();
            if (usage != null) {
                log.info("OpenAI API Usage: prompt_tokens={}, completion_tokens={}, total_tokens={}",
                    usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());

                registry.counter("total_token", "model", model).increment(usage.getTotalTokens());
                registry.counter("input_token", "model", model).increment(usage.getPromptTokens());
                registry.counter("output_token", "model", model).increment(usage.getCompletionTokens());
                log.info("Sent LLM Usage Data to metrics.");
            }

            // Extract and parse model output
            String modelOut = response.getResult().getOutput().getText();
            String jsonStr = extractJsonFromModelOutput(modelOut);
            log.debug("Raw LLM Output: {}", jsonStr);

            LLMReviewResult.Builder builder = LLMReviewResult.newBuilder();
            JsonFormat.parser().merge(jsonStr, builder);
            return builder.build();

        } catch (Exception e) {
            log.error("Error calling Spring AI OpenAI Vision API. URL: {}", url, e);
            registry.counter("llm_api_error", "model", model).increment();

            String errorMsg = e.getMessage();
            if (errorMsg != null && (errorMsg.contains("content_policy_violation") || errorMsg.contains("inappropriate"))) {
                registry.counter("llm_inappropriate_content", "model", model).increment();
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
