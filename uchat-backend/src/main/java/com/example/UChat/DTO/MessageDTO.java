package com.example.UChat.DTO;

import com.example.UChat.model.Message;

import java.time.LocalDateTime;

public class MessageDTO {
    private Long id;
    private Long senderId;
    private String senderTag;
    private Long receiverId;
    private String receiverTag;
    private String content;
    private LocalDateTime timestamp;

    public static MessageDTO fromMessage(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setSenderId(message.getSender().getId());
        dto.setSenderTag(message.getSender().getUserTag());
        dto.setReceiverId(message.getReceiver().getId());
        dto.setReceiverTag(message.getReceiver().getUserTag());
        dto.setContent(message.getContent());
        dto.setTimestamp(message.getTimestamp());
        return dto;
    }


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public String getSenderTag() { return senderTag; }
    public void setSenderTag(String senderTag) { this.senderTag = senderTag; }

    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }

    public String getReceiverTag() { return receiverTag; }
    public void setReceiverTag(String receiverTag) { this.receiverTag = receiverTag; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
