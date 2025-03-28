package com.example.UChat.controller;

import com.example.UChat.model.Message;
import com.example.UChat.service.MessageService;
import com.example.UChat.service.UserService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
public class WebSocketController {

    private final MessageService messageService;

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketController(MessageService messageService, SimpMessagingTemplate messagingTemplate) {
        this.messageService = messageService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/messages/send/{receiverTag}")
    public void sendMessage(@DestinationVariable String receiverTag,
                            @Payload Map<String, String> payload,
                            Principal principal) {
        try {

            String senderTag = principal.getName();
            String content = payload.get("content");

            Message savedMessage = messageService.sendMessage(senderTag, receiverTag, content);

            messagingTemplate.convertAndSend("/topic/user/" + receiverTag, savedMessage);
            messagingTemplate.convertAndSend("/topic/user/" + senderTag, savedMessage);

        } catch (Exception e) {
            System.err.println("Error processing WebSocket message: " + e.getMessage());
            e.printStackTrace();
        }
    }

}