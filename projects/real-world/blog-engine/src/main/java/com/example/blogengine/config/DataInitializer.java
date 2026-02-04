package com.example.blogengine.config;

import com.example.blogengine.model.User;
import com.example.blogengine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            if (userRepository.count() == 0) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setEmail("admin@blog.com");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setDisplayName("Administrator");
                Set<String> adminRoles = new HashSet<>();
                adminRoles.add("ROLE_USER");
                adminRoles.add("ROLE_ADMIN");
                admin.setRoles(adminRoles);
                userRepository.save(admin);

                User user = new User();
                user.setUsername("john");
                user.setEmail("john@example.com");
                user.setPassword(passwordEncoder.encode("password"));
                user.setDisplayName("John Doe");
                Set<String> userRoles = new HashSet<>();
                userRoles.add("ROLE_USER");
                user.setRoles(userRoles);
                userRepository.save(user);

                System.out.println("Sample users created:");
                System.out.println("  Admin - username: admin, password: admin123");
                System.out.println("  User  - username: john, password: password");
            }
        };
    }
}
