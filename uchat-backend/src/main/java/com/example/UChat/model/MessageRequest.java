package com.example.UChat.model;

public class MessageRequest {
    private String receiverTag;
    private String content;

    // Getters and Setters
    public String getReceiverTag() {
        return receiverTag;
    }

    public void setReceiverTag(String receiverTag) {
        this.receiverTag = receiverTag;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}