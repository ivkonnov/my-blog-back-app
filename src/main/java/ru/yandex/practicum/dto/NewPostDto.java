package ru.yandex.practicum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

import static ru.yandex.practicum.exception.ErrorMessages.*;
import static ru.yandex.practicum.validation.PostValidationLimits.*;

public record NewPostDto(

        /*
         В задании не сказано, но логично, что нужно ограничить длину заголовка, текста, кол-во и длину тегов,
         чтобы и в схеме БД указать ограничения и чтобы не было проблем, если вдруг передадутся слишком длинные значения.
         В реальности же нужно уточнить у проджекта или аналитика, но для примера пока выставил примерные лимиты соцсетей
         */

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
         Поэтому, чтобы не допустить такой ситуации, у поста должен быть хотя бы 1 тег.
         */
        @NotNull(message = MSG_TAGS_NOT_NULL)
        @Size(min = TAGS_MIN_COUNT, max = TAGS_MAX_COUNT, message = MSG_TAGS_MIN_MAX_COUNT)
        List<@Size(max = TAG_MAX_LENGTH, message = MSG_TAG_MAX_LENGTH) String> tags

) implements PostData {}