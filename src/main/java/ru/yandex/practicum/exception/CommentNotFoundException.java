package ru.yandex.practicum.exception;

import lombok.Getter;

import static ru.yandex.practicum.exception.ErrorMessages.MSG_COMMENT_NOT_FOUND;

@Getter
public class CommentNotFoundException extends RuntimeException {

    private final Long postId;

    private final Long commentId;

    public CommentNotFoundException(Long postId, Long commentId) {
        super(MSG_COMMENT_NOT_FOUND);
        this.postId = postId;
        this.commentId = commentId;
    }
}
