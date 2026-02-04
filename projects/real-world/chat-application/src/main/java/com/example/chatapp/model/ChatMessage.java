package com.example.chatapp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private String content;
    private String sender;
    private String senderDisplayName;
    private String avatarColor;
    private MessageType type;
    private String roomName;
    private String recipient;
    private LocalDateTime timestamp;

    public enum MessageType {
        CHAT, JOIN, LEAVE, TYPING
    }
}
