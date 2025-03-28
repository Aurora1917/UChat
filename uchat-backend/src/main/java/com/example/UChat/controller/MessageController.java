package com.example.UChat.controller;

import com.example.UChat.DTO.MessageDTO;
import com.example.UChat.model.AuthenticationResponse;
import com.example.UChat.model.Message;
import com.example.UChat.model.MessageRequest;
import com.example.UChat.model.User;
import com.example.UChat.security.JwtRequestFilter;
import com.example.UChat.service.FriendshipService;
import com.example.UChat.service.MessageService;
import com.example.UChat.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/messages")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class MessageController {

    private final MessageService messageService;
    private final JwtRequestFilter jwtRequestFilter;
    private final UserService userService;
    private final FriendshipService friendshipService;

    public MessageController(MessageService messageService, JwtRequestFilter jwtRequestFilter, UserService userService, FriendshipService friendshipService) {
        this.messageService = messageService;
        this.jwtRequestFilter = jwtRequestFilter;
        this.userService = userService;
        this.friendshipService = friendshipService;
    }

    @GetMapping("/history/{receiverTag}")
    public ResponseEntity<List<MessageDTO>> getChatHistory(@PathVariable String receiverTag,
                                                                                  HttpServletRequest request) {
        try {
            Long senderId = jwtRequestFilter.extractUserIdFromToken(request);
            String senderTag = userService.getUserTagById(senderId);

            List<Message> chatHistory = messageService.getChatHistory(receiverTag.toUpperCase(), senderTag.toUpperCase());
            List<MessageDTO> chatHistoryDTO = chatHistory.stream()
                    .map(MessageDTO::fromMessage)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(chatHistoryDTO);
        } catch (Exception e) {
            // Log the error
            System.err.println("Error in getChatHistory: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @GetMapping("/{userTag}")
    public ResponseEntity<List<MessageDTO>> getMessages(@PathVariable String userTag,
                                                                               HttpServletRequest request) {
        try {
            Long senderId = jwtRequestFilter.extractUserIdFromToken(request);
            String senderTag = userService.getUserTagById(senderId);

            List<Message> messages = messageService.getChatHistory(userTag.toUpperCase(), senderTag.toUpperCase());
            List<MessageDTO> messagesDTO = messages.stream()
                    .map(MessageDTO::fromMessage)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(messagesDTO);
        } catch (Exception e) {

            System.err.println("Error in getMessages: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @PostMapping("/send")
    public ResponseEntity<MessageDTO> sendMessage(@RequestBody MessageRequest messageRequest,
                                                  HttpServletRequest request) {
        try {
            Long senderId = jwtRequestFilter.extractUserIdFromToken(request);
            String senderTag = userService.getUserTagById(senderId);

            String receiverTag = messageRequest.getReceiverTag();


            Message message = messageService.sendMessage(senderTag, messageRequest.getReceiverTag(), messageRequest.getContent());
            return ResponseEntity.ok(MessageDTO.fromMessage(message));
        } catch (Exception e) {

            System.err.println("Error in sendMessage: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @PostMapping("delete/{messageId}")
    public ResponseEntity<String> deleteMessage(@PathVariable Long messageId,HttpServletRequest request)
    {
        try{
            Long userId = jwtRequestFilter.extractUserIdFromToken(request);
            messageService.deleteMessage(messageId,userId);
            return ResponseEntity.ok("Message deleted successfully");
        } catch (Exception e)
        {
            return ResponseEntity.badRequest().body("Error" + e.getMessage());
        }
    }
}