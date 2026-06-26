package ru.yandex.practicum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import static ru.yandex.practicum.exception.ErrorMessages.*;
import static ru.yandex.practicum.validation.PostValidationLimits.*;

public record NewCommentDto(

        @NotBlank(message = MSG_COMMENT_REQUIRED)
        @Size(max = COMMENT_MAX_LENGTH, message = MSG_COMMENT_MAX_LENGTH)
        String text,

        @NotNull(message = MSG_POST_ID_REQUIRED)
        Long postId
) {}
