package com.example.websocketdemo;

import com.example.websocketdemo.model.ChatMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertNotNull;

//@RunWith(SpringRunner.class)
//@SpringBootTest
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WebsocketDemoApplicationTests {
    private static final String SEND_CREATE_USER_ENDPOINT = "/app/chat.addUser";
    private static final String SEND_MESSAGE_ENDPOINT = "/app/chat.sendMessage";
    private static final String SUBSCRIBE_CREATE_PUBLIC_ENDPOINT = "/topic/public";
    private static final String SUBSCRIBE_CREATE_BLACK_ENDPOINT = "/topic/black";


    @Value("${local.server.port}")
    private int port;
    private String URL;
    private CompletableFuture<ChatMessage> completableFuture;

    @Before
    public void setup() {
        completableFuture = new CompletableFuture<>();
        URL = "ws://localhost:" + port + "/ws";
    }

    private ChatMessage getChatMessage(String sender, ChatMessage.MessageType type, String message) {
        ChatMessage cm = new ChatMessage();
        cm.setSender(sender);
        cm.setType(type);
        cm.setContent(message);
        return cm;
    }

    @Test
    public void testJoinUser() throws URISyntaxException, InterruptedException, ExecutionException, TimeoutException {

        WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(createTransportClient()));
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        StompSession stompSession = stompClient.connect(URL, new StompSessionHandlerAdapter() {
        }).get(1, SECONDS);

        stompSession.subscribe(SUBSCRIBE_CREATE_PUBLIC_ENDPOINT, new StompFrameHandler());
        stompSession.send(SEND_CREATE_USER_ENDPOINT, getChatMessage("TEST", ChatMessage.MessageType.JOIN, ""));

        ChatMessage msgState = completableFuture.get(10, SECONDS);
        System.out.println(msgState);
        assertNotNull(msgState);
    }

    @Test
    public void testSendMessage() throws URISyntaxException, InterruptedException, ExecutionException, TimeoutException {
        WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(createTransportClient()));
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        StompSession stompSession = stompClient.connect(URL, new StompSessionHandlerAdapter() {
        }).get(1, SECONDS);

        stompSession.subscribe(SUBSCRIBE_CREATE_PUBLIC_ENDPOINT, new StompFrameHandler());

        stompSession.send(SEND_MESSAGE_ENDPOINT, getChatMessage("TEST", ChatMessage.MessageType.CHAT, "have nice day lahuman"));
        ChatMessage msgState = completableFuture.get(10, SECONDS);
        System.out.println(msgState);
        assertNotNull(msgState);
    }

    private List<Transport> createTransportClient() {
        List<Transport> transports = new ArrayList<>(1);
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        return transports;
    }

    private class StompFrameHandler implements org.springframework.messaging.simp.stomp.StompFrameHandler {
        @Override
        public Type getPayloadType(StompHeaders stompHeaders) {
            System.out.println(stompHeaders.toString());
            return ChatMessage.class;
        }

        @Override
        public void handleFrame(StompHeaders stompHeaders, Object o) {
            System.out.println(((ChatMessage) o).getContent());
            completableFuture.complete((ChatMessage) o);
        }
    }

}
