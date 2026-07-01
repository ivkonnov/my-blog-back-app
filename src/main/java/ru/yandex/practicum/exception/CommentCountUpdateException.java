package ru.yandex.practicum.exception;

import lombok.Getter;

@Getter
public class CommentCountUpdateException extends RuntimeException {

    private final Long postId;

    public CommentCountUpdateException(Long postId) {
        super(String.format("Couldn't update comment counter for post with id %s", postId));
        this.postId = postId;
    }
}
