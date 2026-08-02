package ru.yandex.practicum.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.domain.Comment;
import ru.yandex.practicum.dto.CommentDto;
import ru.yandex.practicum.dto.NewCommentDto;
import ru.yandex.practicum.dto.UpdateCommentDto;
import ru.yandex.practicum.exception.CommentNotFoundException;
import ru.yandex.practicum.exception.PostNotFoundException;
import ru.yandex.practicum.mapper.CommentMapper;
import ru.yandex.practicum.repository.CommentRepository;

import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;

    private final PostService postService;

    private final CommentMapper commentMapper;

    public CommentService(
            CommentRepository commentRepository,
            PostService postService,
            CommentMapper commentMapper
    ) {
        this.commentRepository = commentRepository;
        this.postService = postService;
        this.commentMapper = commentMapper;
    }

    @Transactional
    public CommentDto addComment(Long postId, NewCommentDto newCommentDto) {
        if (!postService.existsById(postId))
            throw new PostNotFoundException(postId);

        Comment comment = commentMapper.toComment(newCommentDto);
        Long commentId = commentRepository.save(postId, comment);
        comment.setId(commentId);

        // Увеличиваем счетчик комментариев поста
        postService.incrementCommentsCount(postId);

        return commentMapper.toCommentDto(comment);
    }

    @Transactional(readOnly = true)
    public CommentDto getComment(Long postId, Long commentId) {
        if (!postService.existsById(postId))
            throw new PostNotFoundException(postId);

        return commentRepository.findById(postId, commentId)
                .map(commentMapper::toCommentDto)
                .orElseThrow(() -> new CommentNotFoundException(postId, commentId));
    }

    @Transactional(readOnly = true)
    public List<CommentDto> getComments(Long postId) {
        return commentRepository.findAllByPostId(postId).stream()
                .map(commentMapper::toCommentDto)
                .toList();
    }

    @Transactional
    public CommentDto updateComment(Long postId, Long commentId, UpdateCommentDto updateCommentDto) {
        if (!postService.existsById(postId))
            throw new PostNotFoundException(postId);

        Comment comment = commentMapper.toComment(updateCommentDto);
        comment.setId(commentId);
        comment.setPostId(postId);

        int updated = commentRepository.update(postId, commentId, comment);
        if (updated == 0)
            throw new CommentNotFoundException(postId, commentId);

        return commentMapper.toCommentDto(comment);
    }

    @Transactional
    public void deleteComment(Long postId, Long commentId) {
        if (!postService.existsById(postId))
            throw new PostNotFoundException(postId);

        int deleted = commentRepository.deleteById(postId, commentId);
        if (deleted == 0) throw new CommentNotFoundException(postId, commentId);

        // Уменьшаем счетчик комментариев поста
        postService.decrementCommentsCount(postId);
    }
}
