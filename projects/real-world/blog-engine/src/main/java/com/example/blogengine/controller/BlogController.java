package com.example.blogengine.controller;

import com.example.blogengine.model.BlogPost;
import com.example.blogengine.model.Comment;
import com.example.blogengine.model.User;
import com.example.blogengine.service.BlogPostService;
import com.example.blogengine.service.CommentService;
import com.example.blogengine.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class BlogController {
    private final BlogPostService blogPostService;
    private final CommentService commentService;
    private final UserService userService;

    @GetMapping("/")
    public String home(Model model) {
        List<BlogPost> posts = blogPostService.getAllPublishedPosts();
        model.addAttribute("posts", posts);
        return "index";
    }

    @GetMapping("/posts/{slug}")
    public String viewPost(@PathVariable String slug, Model model) {
        BlogPost post = blogPostService.getPostBySlug(slug);
        List<Comment> comments = commentService.getApprovedCommentsByPost(post.getId());
        model.addAttribute("post", post);
        model.addAttribute("comments", comments);
        return "post";
    }

    @GetMapping("/posts/tag/{tag}")
    public String postsByTag(@PathVariable String tag, Model model) {
        List<BlogPost> posts = blogPostService.getPostsByTag(tag);
        model.addAttribute("posts", posts);
        model.addAttribute("tag", tag);
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                          @RequestParam String email,
                          @RequestParam String password,
                          @RequestParam String displayName,
                          Model model) {
        try {
            userService.registerUser(username, email, password, displayName);
            return "redirect:/login?registered";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

    @GetMapping("/admin")
    public String adminPanel(Model model, Authentication auth) {
        User user = userService.getUserByUsername(auth.getName());
        List<BlogPost> posts = blogPostService.getPostsByAuthor(user.getUsername());
        model.addAttribute("posts", posts);
        return "admin/dashboard";
    }

    @GetMapping("/admin/posts/new")
    public String newPostForm(Model model) {
        model.addAttribute("post", new BlogPost());
        return "admin/post-form";
    }

    @PostMapping("/admin/posts")
    public String createPost(@ModelAttribute BlogPost post, Authentication auth) {
        User user = userService.getUserByUsername(auth.getName());
        blogPostService.createPost(post, user);
        return "redirect:/admin";
    }

    @GetMapping("/admin/posts/{id}/edit")
    public String editPostForm(@PathVariable Long id, Model model) {
        BlogPost post = blogPostService.getPostBySlug(id.toString());
        model.addAttribute("post", post);
        return "admin/post-form";
    }

    @PostMapping("/admin/posts/{id}")
    public String updatePost(@PathVariable Long id, @ModelAttribute BlogPost post) {
        blogPostService.updatePost(id, post);
        return "redirect:/admin";
    }

    @PostMapping("/admin/posts/{id}/publish")
    public String publishPost(@PathVariable Long id) {
        blogPostService.publishPost(id);
        return "redirect:/admin";
    }

    @PostMapping("/admin/posts/{id}/unpublish")
    public String unpublishPost(@PathVariable Long id) {
        blogPostService.unpublishPost(id);
        return "redirect:/admin";
    }

    @PostMapping("/admin/posts/{id}/delete")
    public String deletePost(@PathVariable Long id) {
        blogPostService.deletePost(id);
        return "redirect:/admin";
    }

    @PostMapping("/posts/{postId}/comments")
    public String addComment(@PathVariable Long postId,
                           @RequestParam String content,
                           Authentication auth) {
        User user = userService.getUserByUsername(auth.getName());
        commentService.addComment(postId, content, user);
        BlogPost post = blogPostService.getPostBySlug(postId.toString());
        return "redirect:/posts/" + post.getSlug();
    }
}
