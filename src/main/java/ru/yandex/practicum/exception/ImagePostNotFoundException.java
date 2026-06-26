package ru.yandex.practicum.exception;

import lombok.Getter;

import static ru.yandex.practicum.exception.ErrorMessages.MSG_IMAGE_NOT_FOUND;

@Getter
public class ImagePostNotFoundException extends RuntimeException {

    private final Long postId;

    public ImagePostNotFoundException(Long postId) {
        super(MSG_IMAGE_NOT_FOUND);
        this.postId = postId;
    }
}
