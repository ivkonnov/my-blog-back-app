package ru.yandex.practicum.dto;

import java.util.List;

public record PagePostsDto(
        List<PostPreviewDto> posts,
        boolean hasPrev,
        boolean hasNext,
        int lastPage
) {}