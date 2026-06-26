package ru.yandex.practicum.exception;

import lombok.Getter;

import static ru.yandex.practicum.exception.ErrorMessages.MSG_POST_NOT_FOUND;

@Getter
public class PostNotFoundException extends RuntimeException {

    private final Long postId;

    public PostNotFoundException(Long postId) {
        super(MSG_POST_NOT_FOUND);
        this.postId = postId;
    }

}
