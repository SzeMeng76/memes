package com.memes.schedule;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import com.memes.model.pojo.MediaContent;
import com.memes.service.MediaContentService;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Lazy(value = false)
@Profile("prod")
public class SharpReview {

    final MeterRegistry registry;
    final ChatModel chatModel;

    private static String SYS_PROMPT;

    @Value("classpath:sharp_review.xml")
    private Resource promptResource;

    @Value("${spring.ai.openai.chat.options.model}")
    private String model;

    private final MediaContentService mediaContentService;
    private final ExecutorService reviewExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setName("sharp-review-thread");
        return thread;
    });

    public SharpReview(MeterRegistry registry, ChatModel chatModel, MediaContentService mediaContentService) {
        this.registry = registry;
        this.chatModel = chatModel;
        this.mediaContentService = mediaContentService;
    }

    @PostConstruct
    public void init() throws IOException {
        SYS_PROMPT = StreamUtils.copyToString(promptResource.getInputStream(), StandardCharsets.UTF_8);
        log.info("Spring AI OpenAI ChatModel initialized for sharp review with model: {}", model);
        startReview();
    }

    @PreDestroy
    public void cleanup() {
        reviewExecutor.shutdownNow();
    }

    private void startReview() {
        reviewExecutor.submit(() -> {
            log.info("Starting sharp reviewer...");
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    List<MediaContent> mediaContents = mediaContentService.listNoSharpReviewMediaContent(5);
                    mediaContents
                        .stream()
                        .filter(Objects::nonNull)
                        .filter(mediaContent -> StringUtils.isNotEmpty(mediaContent.getLlmDescription()))
                        .forEach(this::sharpReview);
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Error in sharp review process", e);
                }
            }
        });
    }

    /**
     * 图片锐评
     */
    private void sharpReview(MediaContent mediaContent) {
        log.info("开始锐评：{}", mediaContent.getId());
        try {
            // Create system message
            var systemMessage = new org.springframework.ai.chat.messages.SystemMessage(SYS_PROMPT);

            // Create user message
            var userMessage = new org.springframework.ai.chat.messages.UserMessage(mediaContent.getLlmDescription());

            // Create prompt with options
            var chatOptions = OpenAiChatOptions.builder()
                .model(model)
                .temperature(0.7)
                .maxTokens(500)
                .build();

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
            }

            // Get response content
            String content = response.getResult().getOutput().getText();
            if (StringUtils.isNotEmpty(content)) {
                log.info("LLM Output: {}", content);
                mediaContent.setSharpReview(content);
            } else {
                log.warn("LLM Output is empty for media content: {}", mediaContent.getId());
                mediaContent.setSharpReview("[REVIEW_FAILED]");
            }

            mediaContentService.updateById(mediaContent);

        } catch (Exception e) {
            log.error("Review failed for media content: {}", mediaContent.getId(), e);
            mediaContent.setSharpReview("[REVIEW_FAILED]");
            mediaContentService.updateById(mediaContent);
        }
    }
}
