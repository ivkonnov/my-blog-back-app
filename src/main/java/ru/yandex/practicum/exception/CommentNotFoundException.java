package ru.yandex.practicum.exception;

import lombok.Getter;

@Getter
public class CommentNotFoundException extends RuntimeException {

    private final Long postId;

    private final Long commentId;

    public CommentNotFoundException(Long postId, Long commentId) {
        super("Comment with id " + commentId + " for post with id " + postId + " not found");
        this.postId = postId;
        this.commentId = commentId;
    }
}
