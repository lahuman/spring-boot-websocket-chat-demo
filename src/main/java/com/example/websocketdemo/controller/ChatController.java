package com.example.websocketdemo.controller;

import com.example.websocketdemo.model.ChatMessage;
import com.example.websocketdemo.service.BlackWordService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by rajeevkumarsingh on 24/07/17.
 */
@Controller
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Value("${webpurify.base.url}")
    private String baseUrl;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ObjectReader objectListStringReader;

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    @Autowired
    private BlackWordService service;

    @GetMapping("/blackWordList")
    public @ResponseBody
    ChatMessage blackWordList() {
        //send blackWorld List
        ChatMessage blackMessage = new ChatMessage();
        blackMessage.setType(ChatMessage.MessageType.BLACK);
        blackMessage.setContent(service.getBlackWordList().stream().collect(Collectors.joining(",")));
        return blackMessage;
    }

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage) {

        String apiResult = restTemplate.postForObject(baseUrl, "text=" + chatMessage.getContent(), String.class);
        try {
            JsonNode jsonResult = objectMapper.readTree(apiResult);
            if (jsonResult.get("rsp").get("expletive") != null) {
                blackWordProcess(chatMessage, jsonResult);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return chatMessage;
    }

    private void blackWordProcess(@Payload ChatMessage chatMessage, JsonNode jsonResult) throws IOException {
        logger.info(jsonResult.get("rsp").get("expletive").toString());
        List<String> blackList = ((List<String>) (objectListStringReader.readValue(jsonResult.get("rsp")
                .get("expletive")))).stream().distinct().collect(Collectors.toList());
        blackList.stream().forEach(b -> {
            chatMessage.setContent(chatMessage.getContent()
                    .replaceAll("(?i)" + b, "<font color='red'>" + b + "</font>"));
            try {
                if (service.addBlackWord(chatMessage, b)) {
                    ChatMessage blackWord = new ChatMessage();
                    blackWord.setType(ChatMessage.MessageType.BLACK);
                    blackWord.setContent(b);
                    blackWord.setSender(chatMessage.getSender());
                    messagingTemplate.convertAndSend("/topic/black", blackWord);
                }
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Payload ChatMessage chatMessage,
                               SimpMessageHeaderAccessor headerAccessor) {
        // Add username in web socket session
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        return chatMessage;
    }

}
