package ru.yandex.practicum.dto;

public record CommentDto(
        Long id,
        String text,
        Long postId
) {}
