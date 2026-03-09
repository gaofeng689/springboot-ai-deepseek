package com.aop.springbootaideepseek.controller;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
public class DeepSeekController {

    private final OpenAiChatModel chatModel;

    public DeepSeekController(OpenAiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 根据消息直接输出回答
     */
    @GetMapping("/chat")
    public String chat(@RequestParam(value = "message") String message) {
        return chatModel.call(message);
    }


    @GetMapping(value = "/chatFlux", produces = MediaType.TEXT_EVENT_STREAM_VALUE + "; charset=UTF-8")
    public Flux<String> chatFlux(@RequestParam(value = "message") String message) {
        return chatModel.stream(message);
    }
}
