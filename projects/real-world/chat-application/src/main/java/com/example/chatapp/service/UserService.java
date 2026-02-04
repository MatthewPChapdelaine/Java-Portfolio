package com.example.chatapp.service;

import com.example.chatapp.model.User;
import com.example.chatapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private static final String[] COLORS = {
        "#FF6B6B", "#4ECDC4", "#45B7D1", "#FFA07A", "#98D8C8",
        "#F7DC6F", "#BB8FCE", "#85C1E2", "#F8B4D9", "#A8E6CF"
    };

    @Transactional
    public User createUser(String username, String displayName) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setDisplayName(displayName != null ? displayName : username);
        user.setAvatarColor(COLORS[new Random().nextInt(COLORS.length)]);
        user.setOnline(true);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    @Transactional
    public void setUserOnline(String username, boolean online) {
        User user = getUser(username);
        user.setOnline(online);
        if (!online) {
            user.setLastSeenAt(LocalDateTime.now());
        }
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<User> getOnlineUsers() {
        return userRepository.findByOnlineTrue();
    }

    @Transactional(readOnly = true)
    public boolean userExists(String username) {
        return userRepository.existsByUsername(username);
    }
}
