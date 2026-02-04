package com.example.chatapp.service;

import com.example.chatapp.model.ChatRoom;
import com.example.chatapp.model.User;
import com.example.chatapp.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;

    @Transactional
    public ChatRoom createRoom(String name, String description, User creator) {
        if (chatRoomRepository.existsByName(name)) {
            throw new RuntimeException("Room already exists: " + name);
        }

        ChatRoom room = new ChatRoom();
        room.setName(name);
        room.setDescription(description);
        room.setCreatedBy(creator);
        room.setPrivate(false);
        return chatRoomRepository.save(room);
    }

    @Transactional(readOnly = true)
    public ChatRoom getRoom(String name) {
        return chatRoomRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Room not found: " + name));
    }

    @Transactional(readOnly = true)
    public List<ChatRoom> getPublicRooms() {
        return chatRoomRepository.findByIsPrivateFalse();
    }

    @Transactional(readOnly = true)
    public List<ChatRoom> getAllRooms() {
        return chatRoomRepository.findAll();
    }
}
