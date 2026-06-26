package ru.yandex.practicum.exception;

import lombok.Getter;

import static ru.yandex.practicum.exception.ErrorMessages.MSG_COMMENT_COUNT_UPDATE_ERROR;

@Getter
public class CommentCountUpdateException extends RuntimeException {

    private final Long postId;

    public CommentCountUpdateException(Long postId) {
        super(MSG_COMMENT_COUNT_UPDATE_ERROR);
        this.postId = postId;
    }
}
