package ru.yandex.practicum.exception;

import lombok.Getter;

@Getter
public class ImageReadFailedException extends RuntimeException {

    private final Long postId;

    public ImageReadFailedException(Long postId, String message) {
        super(message);
        this.postId = postId;
    }
}
