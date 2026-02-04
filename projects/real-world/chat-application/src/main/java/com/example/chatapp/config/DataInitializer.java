package com.example.chatapp.config;

import com.example.chatapp.model.ChatRoom;
import com.example.chatapp.model.User;
import com.example.chatapp.service.ChatRoomService;
import com.example.chatapp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {
    private final UserService userService;
    private final ChatRoomService chatRoomService;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            User systemUser = userService.createUser("system", "System");
            systemUser.setOnline(false);
            
            chatRoomService.createRoom("general", "General discussion", systemUser);
            chatRoomService.createRoom("random", "Random topics", systemUser);
            chatRoomService.createRoom("tech", "Technology discussions", systemUser);
            
            System.out.println("Chat application initialized!");
            System.out.println("Default rooms created: general, random, tech");
        };
    }
}
