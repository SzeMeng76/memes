package com.memes.schedule;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import com.memes.model.pojo.MediaContent;
import com.memes.service.MediaContentService;

import io.github.sashirestela.openai.SimpleOpenAI;
import io.github.sashirestela.openai.domain.chat.ChatRequest;
import io.github.sashirestela.openai.domain.chat.ChatResponse;
import io.github.sashirestela.openai.domain.chat.message.ChatMsg;
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

    private static ChatMsg SYS_MSG;

    @Value("classpath:sharp_review.xml")
    private Resource promptResource;

    @Value("${openai.apiKey}")
    private String apiKey;

    @Value("${openai.baseUrl}")
    private String baseUrl;

    @Value("${openai.model}")
    private String model;

    private SimpleOpenAI openAI;
    private final MediaContentService mediaContentService;
    private final ExecutorService reviewExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setName("sharp-review-thread");
        return thread;
    });

    public SharpReview(MeterRegistry registry, MediaContentService mediaContentService) {
        this.registry = registry;
        this.mediaContentService = mediaContentService;
    }

    @PostConstruct
    public void init() throws IOException {
        String SYS_PROMPT = StreamUtils.copyToString(promptResource.getInputStream(), StandardCharsets.UTF_8);
        SYS_MSG = ChatMsg.SystemMessage.of(SYS_PROMPT);

        // Initialize OpenAI client with custom base URL support
        openAI = SimpleOpenAI.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .build();

        log.info("OpenAI client initialized for sharp review with base URL: {}, model: {}", baseUrl, model);
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
            // Create chat request
            var chatRequest = ChatRequest.builder()
                .model(model)
                .messages(List.of(
                    SYS_MSG,
                    ChatMsg.UserMessage.of(mediaContent.getLlmDescription())
                ))
                .temperature(0.7)
                .maxTokens(500)
                .build();

            // Call OpenAI API
            ChatResponse response = openAI.chatCompletions()
                .create(chatRequest)
                .join();

            // Handle usage statistics
            var usage = response.getUsage();
            log.info("OpenAI API Usage: prompt_tokens={}, completion_tokens={}, total_tokens={}",
                usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());

            registry.counter("total_token", "model", model).increment(usage.getTotalTokens());
            registry.counter("input_token", "model", model).increment(usage.getPromptTokens());
            registry.counter("output_token", "model", model).increment(usage.getCompletionTokens());

            // Get response content
            String content = response.firstContent();
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
