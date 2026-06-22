package ru.yandex.practicum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

import static ru.yandex.practicum.dto.PostValidationConstants.*;

public record UpdatePostDto (

        @NotNull(message = MSG_ID_REQUIRED)
        Long id,

        @NotBlank(message = MSG_TITLE_REQUIRED)
        @Size(max = TITLE_MAX_LENGTH, message = MSG_TITLE_MAX_LENGTH)
        String title,

        @NotBlank(message = MSG_TEXT_REQUIRED)
        @Size(max = TEXT_MAX_LENGTH, message = MSG_TEXT_MAX_LENGTH)
        String text,

        /*
         Особенность фронта - при обновлении поста передаётся пустой список тегов [] в 2-х случаях:
           1) если теги удалили
           2) если теги не изменяли (поле с тегами не редактировали)
         И на стороне бэка непонятно как определять какой именно из случаев передан.
         Поэтому, чтобы не допустить такой неоднозначной ситуации, у поста должен быть хотя бы 1 тег.

         А при обновлении убираем ограничение на минимальное количество тегов TAGS_MIN_COUNT
         И в обоих случаях пустой список тегов [] трактуем как - теги не изменяли (поле с тегами не редактировали).
         */
        @NotNull(message = MSG_TAGS_NOT_NULL)
        @Size(max = TAGS_MAX_COUNT, message = MSG_TAGS_MIN_MAX_COUNT)
        List<@Size(max = TAG_MAX_LENGTH, message = MSG_TAG_MAX_LENGTH) String> tags

) implements PostData {}