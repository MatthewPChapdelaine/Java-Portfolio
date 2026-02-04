package com.example.chatapp.service;

import com.example.chatapp.model.ChatRoom;
import com.example.chatapp.model.Message;
import com.example.chatapp.model.User;
import com.example.chatapp.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageRepository messageRepository;

    @Transactional
    public Message saveMessage(ChatRoom room, User sender, String content, Message.MessageType type) {
        Message message = new Message();
        message.setRoom(room);
        message.setSender(sender);
        message.setContent(content);
        message.setType(type);
        return messageRepository.save(message);
    }

    @Transactional
    public Message savePrivateMessage(User sender, User recipient, String content) {
        Message message = new Message();
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setContent(content);
        message.setType(Message.MessageType.CHAT);
        return messageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public List<Message> getRoomMessages(ChatRoom room) {
        return messageRepository.findByRoomOrderByTimestampAsc(room);
    }

    @Transactional(readOnly = true)
    public List<Message> getRecentRoomMessages(ChatRoom room, int limit) {
        List<Message> messages = messageRepository.findTop50ByRoomOrderByTimestampDesc(room);
        Collections.reverse(messages);
        return messages;
    }

    @Transactional(readOnly = true)
    public List<Message> getPrivateMessages(User user1, User user2) {
        return messageRepository.findPrivateMessages(user1, user2);
    }
}
