package ru.yandex.practicum.exception;

import lombok.Getter;

@Getter
public class ImageEmptyException extends RuntimeException {

    private final Long postId;

    public ImageEmptyException(Long postId) {
        super(String.format("Image is not selected for post with id %s", postId));
        this.postId = postId;
    }
}
