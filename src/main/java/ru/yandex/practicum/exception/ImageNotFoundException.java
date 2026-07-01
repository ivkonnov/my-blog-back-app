package ru.yandex.practicum.exception;

import lombok.Getter;

@Getter
public class ImageNotFoundException extends RuntimeException {

    private final Long postId;

    public ImageNotFoundException(Long postId) {
        super(String.format("Image for post with id %s not found", postId));
        this.postId = postId;
    }
}
