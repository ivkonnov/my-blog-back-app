package ru.yandex.practicum.dto;

import java.util.List;

public record PagePostsDto(
        List<PostDto> posts,
        boolean hasPrev,
        boolean hasNext,
        int lastPage
) {}