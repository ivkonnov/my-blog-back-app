package ru.yandex.practicum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import static ru.yandex.practicum.exception.ErrorMessages.*;
import static ru.yandex.practicum.validation.PostValidationLimits.COMMENT_MAX_LENGTH;

public record UpdateCommentDto(

        @NotNull(message = MSG_COMMENT_ID_REQUIRED)
        Long id,

        @NotBlank(message = MSG_COMMENT_REQUIRED)
        @Size(max = COMMENT_MAX_LENGTH, message = MSG_COMMENT_MAX_LENGTH)
        String text,

        @NotNull(message = MSG_POST_ID_REQUIRED)
        Long postId

) {}
