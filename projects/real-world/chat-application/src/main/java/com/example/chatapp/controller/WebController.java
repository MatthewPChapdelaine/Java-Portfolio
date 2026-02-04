package com.example.chatapp.controller;

import com.example.chatapp.model.ChatRoom;
import com.example.chatapp.model.Message;
import com.example.chatapp.model.User;
import com.example.chatapp.service.ChatRoomService;
import com.example.chatapp.service.MessageService;
import com.example.chatapp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class WebController {
    private final ChatRoomService chatRoomService;
    private final MessageService messageService;
    private final UserService userService;

    @GetMapping("/")
    public String index(Model model) {
        List<ChatRoom> rooms = chatRoomService.getPublicRooms();
        model.addAttribute("rooms", rooms);
        return "index";
    }

    @GetMapping("/chat/{roomName}")
    public String chatRoom(@PathVariable String roomName, Model model) {
        try {
            ChatRoom room = chatRoomService.getRoom(roomName);
            List<Message> messages = messageService.getRecentRoomMessages(room, 50);
            List<User> onlineUsers = userService.getOnlineUsers();
            
            model.addAttribute("room", room);
            model.addAttribute("messages", messages);
            model.addAttribute("onlineUsers", onlineUsers);
            
            return "chat";
        } catch (Exception e) {
            return "redirect:/";
        }
    }

    @PostMapping("/rooms/create")
    public String createRoom(@RequestParam String name,
                           @RequestParam String description,
                           @RequestParam String creator) {
        try {
            User user = userService.userExists(creator) 
                ? userService.getUser(creator) 
                : userService.createUser(creator, creator);
            
            chatRoomService.createRoom(name, description, user);
            return "redirect:/";
        } catch (Exception e) {
            return "redirect:/?error=" + e.getMessage();
        }
    }
}
