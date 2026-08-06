package ru.yandex.practicum.exception;

import lombok.Getter;

@Getter
public class ImageUpdateFailedException extends RuntimeException {

    private final Long postId;

    public ImageUpdateFailedException(Long postId) {
        super(String.format("Failed to update image for post with id %s", postId));
        this.postId = postId;
    }
}
