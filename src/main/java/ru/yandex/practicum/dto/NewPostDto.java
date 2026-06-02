package ru.yandex.practicum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record NewPostDto(

        // В задании не сказано, но логично, что нужно ограничить длину заголовка, текста, кол-во и длину тегов
        // В реальности нужно уточнить у PM, но для примера пока выставил примерные лимиты соцсетей

        @NotBlank(message = MSG_TITLE_REQUIRED)
        @Size(max = TITLE_MAX_LENGTH, message = MSG_TITLE_MAX_LENGTH)
        String title,

        @NotBlank(message = MSG_TEXT_REQUIRED)
        @Size(max = TEXT_MAX_LENGTH, message = MSG_TEXT_MAX_LENGTH)
        String text,

        @NotEmpty(message = MSG_TAGS_REQUIRED)
        @Size(max = TAGS_MAX_COUNT, message = MSG_TAGS_MAX_COUNT)
        List<@Size(max = TAG_MAX_LENGTH, message = MSG_TAG_MAX_LENGTH) String> tags
) {

        public static final int TITLE_MAX_LENGTH = 128;
        public static final int TEXT_MAX_LENGTH = 4096;
        public static final int TAGS_MAX_COUNT = 10;
        public static final int TAG_MAX_LENGTH = 25;

        public static final String MSG_TITLE_MAX_LENGTH = "Заголовок поста не должен превышать " + TITLE_MAX_LENGTH + " символов";
        public static final String MSG_TEXT_MAX_LENGTH = "Текст поста не должен превышать " + TEXT_MAX_LENGTH + " символов";
        public static final String MSG_TAG_MAX_LENGTH = "Длина тега не должна превышать " + TAG_MAX_LENGTH + " символов";
        public static final String MSG_TAGS_MAX_COUNT = "Можно указать не более " + TAGS_MAX_COUNT + " тегов";

        public static final String MSG_TITLE_REQUIRED = "Заголовок поста обязателен";
        public static final String MSG_TEXT_REQUIRED = "Текст поста обязателен";
        public static final String MSG_TAGS_REQUIRED = "Должен быть указан хотя бы один тег";

}

