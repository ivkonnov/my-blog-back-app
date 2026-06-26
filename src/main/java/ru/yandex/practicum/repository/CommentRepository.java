package ru.yandex.practicum.repository;

import ru.yandex.practicum.domain.Comment;

import java.util.Optional;

public interface CommentRepository {

    Long save(Long postId, Comment comment);

    Optional<Comment> findById(Long postId, Long commentId);
}
