package com.example.chatapp.repository;

import com.example.chatapp.model.Message;
import com.example.chatapp.model.ChatRoom;
import com.example.chatapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByRoomOrderByTimestampAsc(ChatRoom room);
    
    @Query("SELECT m FROM Message m WHERE " +
           "(m.sender = ?1 AND m.recipient = ?2) OR " +
           "(m.sender = ?2 AND m.recipient = ?1) " +
           "ORDER BY m.timestamp ASC")
    List<Message> findPrivateMessages(User user1, User user2);
    
    List<Message> findTop50ByRoomOrderByTimestampDesc(ChatRoom room);
}
