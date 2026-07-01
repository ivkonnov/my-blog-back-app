package ru.yandex.practicum.exception;

import lombok.Getter;

@Getter
public class PostNotFoundException extends RuntimeException {

    private final Long postId;

    public PostNotFoundException(Long postId) {
        super(String.format("Post not found with postId: %s", postId));
        this.postId = postId;
    }

}
