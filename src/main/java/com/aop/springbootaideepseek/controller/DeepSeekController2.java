package com.aop.springbootaideepseek.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai2")
public class DeepSeekController2 {

    private final ChatClient chatClient;

    // 推荐注入 ChatClient.Builder 来构建实例
    public DeepSeekController2(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    /**
     * 同步调用：根据消息直接输出回答
     */
    @GetMapping("/chat")
    public String chat(@RequestParam(value = "message") String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content(); // 直接获取文本内容
    }

    /**
     * 流式调用：SSE 模式输出
     */
    @GetMapping(value = "/chatFlux", produces = MediaType.TEXT_EVENT_STREAM_VALUE + "; charset=UTF-8")
    public Flux<String> chatFlux(@RequestParam(value = "message") String message) {
        return chatClient.prompt()
                .user(message)
                .stream()
                .content(); // 返回 Flux<String>
    }
}

