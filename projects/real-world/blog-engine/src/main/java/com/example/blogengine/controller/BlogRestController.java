package com.example.blogengine.controller;

import com.example.blogengine.model.BlogPost;
import com.example.blogengine.service.BlogPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class BlogRestController {
    private final BlogPostService blogPostService;

    @GetMapping
    public ResponseEntity<List<BlogPost>> getAllPosts() {
        return ResponseEntity.ok(blogPostService.getAllPublishedPosts());
    }

    @GetMapping("/{slug}")
    public ResponseEntity<BlogPost> getPost(@PathVariable String slug) {
        return ResponseEntity.ok(blogPostService.getPostBySlug(slug));
    }

    @GetMapping("/tag/{tag}")
    public ResponseEntity<List<BlogPost>> getPostsByTag(@PathVariable String tag) {
        return ResponseEntity.ok(blogPostService.getPostsByTag(tag));
    }
}
