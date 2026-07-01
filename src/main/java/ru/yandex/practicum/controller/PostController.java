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
import ru.yandex.practicum.exception.IdMismatchException;
import ru.yandex.practicum.service.CommentService;
import ru.yandex.practicum.service.PostService;

import java.util.List;

import static java.util.Collections.emptyList;

@Slf4j
@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    private final CommentService commentService;

    public PostController(PostService postService, CommentService commentService) {
        this.postService = postService;
        this.commentService = commentService;
    }

    // Получение постов по запросу из строки поиска
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PagePostsDto> getPagePosts(
            @RequestParam("search") String search,
            @RequestParam("pageNumber") int pageNumber,
            @RequestParam("pageSize") int pageSize
    ) {
        log.info("Get posts with search: {} pageNumber: {} pageSize: {}", search, pageNumber, pageSize);
        PagePostsDto page = postService.getPosts(search, pageNumber, pageSize);
        return ResponseEntity.ok(page);
    }

    // Добавление поста
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PostDto> addPost(
            @Valid @RequestBody NewPostDto newPostDto
    ) {
        log.info("Add new post with title: {}", newPostDto.title());
        PostDto postDto = postService.addPost(newPostDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(postDto);
    }

    // Получение поста
    @GetMapping(value = "/{postId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PostDto> getPost(@PathVariable("postId") Long postId) {
        log.info("Get post with id: {}", postId);
        PostDto postDto = postService.getPost(postId);
        return ResponseEntity.ok(postDto);
    }

    // Обновление поста
    @PutMapping(value = "/{postId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PostDto> updatePost(@PathVariable("postId") Long postId, @Valid @RequestBody UpdatePostDto updatePostDto) {
        if (!postId.equals(updatePostDto.id())) {
            throw new IdMismatchException("postId", postId, updatePostDto.id());
        }
        log.info("Update postId: {}", postId);
        PostDto postDto = postService.updatePost(postId, updatePostDto);
        return ResponseEntity.ok(postDto);
    }

    // Добавление лайка
    @PostMapping(value = "/{postId}/likes", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Long> addLike(@PathVariable("postId") Long postId) {
        log.info("Add like for postId: {}", postId);
        Long updatedLikes = postService.addLike(postId);
        return ResponseEntity.ok(updatedLikes);
    }

    // Обновление картинки поста
    @PutMapping(value = "/{postId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> updateImage(
            @PathVariable("postId") Long postId,
            @RequestParam("image") MultipartFile image
    ) throws Exception {
        if (image.isEmpty()) {
            log.warn("Empty image for postId: {}", postId);
            return ResponseEntity.badRequest().body("Empty image");
        }
        log.info("Update image for postId: {}", postId);
        boolean updated = postService.updateImage(postId, image.getBytes());
        if (!updated) {
            log.error("Failed to update image for postId: {}", postId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update image");
        }
        return ResponseEntity.ok().build();
    }

    // Получение картинки поста
    @GetMapping(value = "/{postId}/image", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getImage(@PathVariable("postId") Long postId) {
        log.info("Get image of postId: {}", postId);
        byte[] bytes = postService.getImage(postId);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(bytes);
    }

    @PostMapping(value = "/{postId}/comments", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CommentDto> addComment(
            @PathVariable("postId") Long postId,
            @Valid @RequestBody NewCommentDto newCommentDto
    ) {
        if (!postId.equals(newCommentDto.postId()))
            throw new IdMismatchException("postId", postId, newCommentDto.postId());

        log.info("Add comment for postId: {}", postId);
        CommentDto commentDto = commentService.addComment(postId, newCommentDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(commentDto);
    }

    @GetMapping(value = "/{postId}/comments/{commentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CommentDto> getComment(
            @PathVariable("postId") Long postId,
            @PathVariable("commentId") Long commentId
    ) {
        log.info("Get commentId: {} of postId: {}", commentId, postId);
        CommentDto commentDto = commentService.getComment(postId, commentId);
        return ResponseEntity.ok(commentDto);
    }

    @GetMapping(value = "/{postId}/comments", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CommentDto>> getComments(
            @PathVariable("postId") String postId
    ) {
        log.info("Get all comments of postId: {}", postId);
        // странно, что фронт не определив номер id поста преждевременно отправляет запрос с undefined
        if (postId.equals("undefined")) {
            return ResponseEntity.badRequest().body(emptyList());
        }
        List<CommentDto> comments = commentService.getComments(Long.valueOf(postId));
        return ResponseEntity.ok(comments);
    }

    @PutMapping(value = "/{postId}/comments/{commentId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CommentDto> updateComment(
            @PathVariable("postId") Long postId,
            @PathVariable("commentId") Long commentId,
            @Valid @RequestBody UpdateCommentDto updateCommentDto
    ) {
        // При обновлении комментариев всплывает баг - фронт отправляет commentId вместо postId в url
        if (!postId.equals(updateCommentDto.postId()))
            throw new IdMismatchException("postId", postId, updateCommentDto.postId());

        if (!commentId.equals(updateCommentDto.id()))
            throw new IdMismatchException("commentId", commentId, updateCommentDto.id());

        log.info("Update commentId: {} of postId: {}", commentId, postId);
        CommentDto updatedComment = commentService.updateComment(postId, commentId, updateCommentDto);
        return ResponseEntity.ok(updatedComment);
    }

}