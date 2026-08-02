package ru.yandex.practicum.repository;

import ru.yandex.practicum.domain.Comment;

import java.util.List;
import java.util.Optional;

public interface CommentRepository {

    Long save(Long postId, Comment comment);

    int update(Long postId, Long commentId, Comment comment);

    Optional<Comment> findById(Long postId, Long commentId);

    List<Comment> findAllByPostId(Long postId);

    int deleteById(Long postId, Long commentId);

    boolean existsById(Long postId, Long commentId);

}
