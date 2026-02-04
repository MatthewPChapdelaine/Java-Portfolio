package com.example.blogengine.repository;

import com.example.blogengine.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPost_IdAndApprovedTrueOrderByCreatedAtDesc(Long postId);
    List<Comment> findByApprovedFalseOrderByCreatedAtDesc();
    long countByPost_IdAndApprovedTrue(Long postId);
}
