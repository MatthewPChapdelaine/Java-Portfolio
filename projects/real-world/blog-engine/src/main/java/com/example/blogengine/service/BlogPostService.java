package com.example.blogengine.service;

import com.example.blogengine.model.BlogPost;
import com.example.blogengine.model.User;
import com.example.blogengine.repository.BlogPostRepository;
import lombok.RequiredArgsConstructor;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BlogPostService {
    private final BlogPostRepository blogPostRepository;
    private final Parser markdownParser = Parser.builder().build();
    private final HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();

    @Transactional(readOnly = true)
    public List<BlogPost> getAllPublishedPosts() {
        return blogPostRepository.findByPublishedTrueOrderByPublishedAtDesc();
    }

    @Transactional(readOnly = true)
    public BlogPost getPostBySlug(String slug) {
        return blogPostRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Post not found: " + slug));
    }

    @Transactional(readOnly = true)
    public List<BlogPost> getPostsByAuthor(String username) {
        return blogPostRepository.findByAuthor_UsernameOrderByCreatedAtDesc(username);
    }

    @Transactional(readOnly = true)
    public List<BlogPost> getPostsByTag(String tag) {
        return blogPostRepository.findByTagAndPublishedTrue(tag);
    }

    @Transactional
    public BlogPost createPost(BlogPost post, User author) {
        post.setAuthor(author);
        post.setSlug(generateSlug(post.getTitle()));
        post.setRenderedContent(renderMarkdown(post.getContent()));
        post.setExcerpt(generateExcerpt(post.getContent()));
        return blogPostRepository.save(post);
    }

    @Transactional
    public BlogPost updatePost(Long id, BlogPost updatedPost) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        
        post.setTitle(updatedPost.getTitle());
        post.setContent(updatedPost.getContent());
        post.setRenderedContent(renderMarkdown(updatedPost.getContent()));
        post.setExcerpt(generateExcerpt(updatedPost.getContent()));
        post.setTags(updatedPost.getTags());
        post.setUpdatedAt(LocalDateTime.now());
        
        return blogPostRepository.save(post);
    }

    @Transactional
    public void deletePost(Long id) {
        blogPostRepository.deleteById(id);
    }

    @Transactional
    public BlogPost publishPost(Long id) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        post.setPublished(true);
        post.setPublishedAt(LocalDateTime.now());
        return blogPostRepository.save(post);
    }

    @Transactional
    public BlogPost unpublishPost(Long id) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        post.setPublished(false);
        return blogPostRepository.save(post);
    }

    private String renderMarkdown(String markdown) {
        var document = markdownParser.parse(markdown);
        return htmlRenderer.render(document);
    }

    private String generateSlug(String title) {
        String baseSlug = title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        
        String slug = baseSlug;
        int counter = 1;
        while (blogPostRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
        }
        return slug;
    }

    private String generateExcerpt(String content) {
        String plainText = content.replaceAll("#", "").trim();
        if (plainText.length() <= 200) {
            return plainText;
        }
        return plainText.substring(0, 197) + "...";
    }
}
