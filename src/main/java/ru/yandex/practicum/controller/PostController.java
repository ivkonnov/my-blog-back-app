package ru.yandex.practicum.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.yandex.practicum.dto.*;
import ru.yandex.practicum.service.PostService;

import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    // Получение постов по запросу из строки поиска
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PagePostsDto> getPosts(
            @RequestParam("search") String search,
            @RequestParam("pageNumber") int pageNumber,
            @RequestParam("pageSize") int pageSize
    ) {
        log.info("Get posts with search {} pageNumber {} pageSize {}", search, pageNumber, pageSize);
        PagePostsDto page = postService.getPosts(search, pageNumber, pageSize);
        log.info("Get posts with search {} pageNumber {} pageSize {} count {}", search, pageNumber, pageSize, page.posts().size());
        return ResponseEntity.ok(page);
    }

    // Добавление поста
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PostDto> addPost(
            @Valid @RequestBody NewPostDto newPostDto
    ) {
        log.info("Add new post with title {}", newPostDto.title());
        PostDto postDto = postService.addPost(newPostDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(postDto);
    }

    // Получение поста по id
    @GetMapping(value = "/{postId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PostDto> getPost(@PathVariable("postId") Long postId) {
        log.info("Get post with id {}", postId);
        return postService.getPost(postId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    log.warn("Post with id {} not found", postId);
                    return ResponseEntity.notFound().build();
                });
    }

    // Обновление картинки поста
    @PutMapping(value = "/{postId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> updateImage(
            @PathVariable("postId") Long postId,
            @RequestParam("image") MultipartFile image
    ) throws Exception {
        log.info("Update image for post with id {}", postId);
        if (!postService.existsPost(postId)) {
            log.warn("Post with id {} for update image not found", postId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Post not found");
        }
        if (image.isEmpty()) {
            log.warn("Empty image for post with id {}", postId);
            return ResponseEntity.badRequest().body("Empty image");
        }
        boolean ok = postService.updateImage(postId, image.getBytes());
        if (!ok) {
            log.error("Failed to update image for post with id {}", postId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update image");
        }
        return ResponseEntity.ok().build();
    }

    // Получение картинки поста
    @GetMapping(value = "/{postId}/image", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getImage(@PathVariable("postId") Long postId) {
        log.info("Get image for post with id {}", postId);
        if (!postService.existsPost(postId)) {
            log.warn("Post with id {} for get image not found", postId);
            return ResponseEntity.notFound().build();
        }
        Optional<byte[]> bytes = postService.getImage(postId);
        return bytes.map(image -> ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(image))
            .orElseGet(() -> {
                log.warn("Image for post with id {} not found", postId);
                return ResponseEntity.notFound().build();
            });
    }
}