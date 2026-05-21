package ru.yandex.practicum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record NewPostDto(

        // В задании не сказано, но логично, что нужно ограничить длину заголовка, текста, кол-во и длину тегов
        // В реальности нужно уточнить у PM, но для примера пока выставил примерные лимиты соцсетей

        @NotBlank(message = "Заголовок поста обязателен")
        @Size(max = TITLE_MAX_LENGTH, message = "Заголовок поста не должен превышать " + TITLE_MAX_LENGTH + " символов")
        String title,

        @NotBlank(message = "Текст поста обязателен")
        @Size(max = TEXT_MAX_LENGTH, message = "Текст поста не должен превышать " + TEXT_MAX_LENGTH + " символов")
        String text,

        @NotEmpty(message = "Должен быть указан хотя бы один тег")
        @Size(max = TAGS_MAX_COUNT, message = "Можно указать не более " + TAGS_MAX_COUNT + " тегов")
        List<@Size(max = TAG_MAX_LENGTH, message = "Длина тега не должна превышать " + TAG_MAX_LENGTH + " символов") String> tags
) {

        public static final int TITLE_MAX_LENGTH = 128;
        public static final int TEXT_MAX_LENGTH = 4096;
        public static final int TAGS_MAX_COUNT = 10;
        public static final int TAG_MAX_LENGTH = 25;

}

