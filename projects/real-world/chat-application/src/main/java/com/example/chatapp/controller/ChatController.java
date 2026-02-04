package com.example.chatapp.controller;

import com.example.chatapp.model.ChatMessage;
import com.example.chatapp.model.ChatRoom;
import com.example.chatapp.model.Message;
import com.example.chatapp.model.User;
import com.example.chatapp.service.ChatRoomService;
import com.example.chatapp.service.MessageService;
import com.example.chatapp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class ChatController {
    private final MessageService messageService;
    private final UserService userService;
    private final ChatRoomService chatRoomService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.sendMessage/{roomName}")
    @SendTo("/topic/{roomName}")
    public ChatMessage sendMessage(@DestinationVariable String roomName,
                                  @Payload ChatMessage chatMessage) {
        try {
            ChatRoom room = chatRoomService.getRoom(roomName);
            User sender = userService.getUser(chatMessage.getSender());
            
            messageService.saveMessage(room, sender, chatMessage.getContent(), Message.MessageType.CHAT);
            
            chatMessage.setTimestamp(LocalDateTime.now());
            chatMessage.setType(ChatMessage.MessageType.CHAT);
            chatMessage.setSenderDisplayName(sender.getDisplayName());
            chatMessage.setAvatarColor(sender.getAvatarColor());
            
            return chatMessage;
        } catch (Exception e) {
            e.printStackTrace();
            return chatMessage;
        }
    }

    @MessageMapping("/chat.addUser/{roomName}")
    @SendTo("/topic/{roomName}")
    public ChatMessage addUser(@DestinationVariable String roomName,
                              @Payload ChatMessage chatMessage,
                              SimpMessageHeaderAccessor headerAccessor) {
        try {
            User user;
            if (userService.userExists(chatMessage.getSender())) {
                user = userService.getUser(chatMessage.getSender());
                userService.setUserOnline(chatMessage.getSender(), true);
            } else {
                user = userService.createUser(chatMessage.getSender(), chatMessage.getSenderDisplayName());
            }
            
            headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
            headerAccessor.getSessionAttributes().put("roomName", roomName);
            
            ChatRoom room = chatRoomService.getRoom(roomName);
            messageService.saveMessage(room, user, "joined the chat", Message.MessageType.JOIN);
            
            chatMessage.setType(ChatMessage.MessageType.JOIN);
            chatMessage.setTimestamp(LocalDateTime.now());
            chatMessage.setSenderDisplayName(user.getDisplayName());
            chatMessage.setAvatarColor(user.getAvatarColor());
            
            return chatMessage;
        } catch (Exception e) {
            e.printStackTrace();
            return chatMessage;
        }
    }

    @MessageMapping("/chat.private")
    public void sendPrivateMessage(@Payload ChatMessage chatMessage) {
        try {
            User sender = userService.getUser(chatMessage.getSender());
            User recipient = userService.getUser(chatMessage.getRecipient());
            
            messageService.savePrivateMessage(sender, recipient, chatMessage.getContent());
            
            chatMessage.setTimestamp(LocalDateTime.now());
            chatMessage.setSenderDisplayName(sender.getDisplayName());
            chatMessage.setAvatarColor(sender.getAvatarColor());
            
            messagingTemplate.convertAndSendToUser(
                chatMessage.getRecipient(),
                "/queue/messages",
                chatMessage
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @MessageMapping("/chat.typing/{roomName}")
    @SendTo("/topic/{roomName}/typing")
    public ChatMessage userTyping(@DestinationVariable String roomName,
                                  @Payload ChatMessage chatMessage) {
        chatMessage.setType(ChatMessage.MessageType.TYPING);
        return chatMessage;
    }
}
