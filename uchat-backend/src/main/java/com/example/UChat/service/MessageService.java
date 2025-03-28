package com.example.UChat.service;


import com.example.UChat.model.Message;
import com.example.UChat.model.User;
import com.example.UChat.repository.MessageRepository;
import com.example.UChat.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;


    public MessageService(MessageRepository messageRepository, UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    public Message sendMessage(String senderTag, String receiverTag, String content)
    {
        User sender = userRepository.findByUserTag(senderTag)
                .orElseThrow(()-> new RuntimeException("Sender not found"));
        User receiver = userRepository.findByUserTag(receiverTag)
                .orElseThrow(()-> new RuntimeException("Receiver not found"));

        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(content);
        message.setTimestamp(LocalDateTime.now());

        return messageRepository.save(message);
    }

    public List<Message> getChatHistory(String receiverTag, String senderTag) {


        User sender = userRepository.findByUserTag(senderTag)
                .orElseThrow(() -> new RuntimeException("Sender not found"));

        User receiver = userRepository.findByUserTag(receiverTag)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));


        return messageRepository.findMessagesBySenderIdAndReceiverIdOrSenderIdAndReceiverId(
                sender.getId(), receiver.getId(), receiver.getId(), sender.getId());
    }

    public void deleteMessage(Long messageId, Long userId)
    {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));


        if (!message.getSender().getId().equals(userId)) {
            throw new RuntimeException("You are not authorized to delete this message");
        }

        messageRepository.deleteById(messageId);
    }


}
