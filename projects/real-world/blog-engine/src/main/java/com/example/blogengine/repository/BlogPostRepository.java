package com.example.blogengine.repository;

import com.example.blogengine.model.BlogPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {
    Optional<BlogPost> findBySlug(String slug);
    List<BlogPost> findByPublishedTrueOrderByPublishedAtDesc();
    List<BlogPost> findByAuthor_UsernameOrderByCreatedAtDesc(String username);
    
    @Query("SELECT p FROM BlogPost p WHERE p.published = true AND :tag MEMBER OF p.tags ORDER BY p.publishedAt DESC")
    List<BlogPost> findByTagAndPublishedTrue(String tag);
    
    boolean existsBySlug(String slug);
}
