package com.example.blogengine.service;

import com.example.blogengine.model.Comment;
import com.example.blogengine.model.BlogPost;
import com.example.blogengine.model.User;
import com.example.blogengine.repository.CommentRepository;
import com.example.blogengine.repository.BlogPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final BlogPostRepository blogPostRepository;

    @Transactional(readOnly = true)
    public List<Comment> getApprovedCommentsByPost(Long postId) {
        return commentRepository.findByPost_IdAndApprovedTrueOrderByCreatedAtDesc(postId);
    }

    @Transactional(readOnly = true)
    public List<Comment> getPendingComments() {
        return commentRepository.findByApprovedFalseOrderByCreatedAtDesc();
    }

    @Transactional
    public Comment addComment(Long postId, String content, User author) {
        BlogPost post = blogPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        
        Comment comment = new Comment();
        comment.setPost(post);
        comment.setContent(content);
        comment.setAuthor(author);
        comment.setApproved(true);
        
        return commentRepository.save(comment);
    }

    @Transactional
    public Comment approveComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        comment.setApproved(true);
        return commentRepository.save(comment);
    }

    @Transactional
    public void deleteComment(Long commentId) {
        commentRepository.deleteById(commentId);
    }
}
