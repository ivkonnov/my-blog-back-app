package ru.yandex.practicum.dto;

public class PostValidationConstants {

    public static final int TITLE_MAX_LENGTH = 128;
    public static final int TEXT_MAX_LENGTH = 4096;
    public static final int TAGS_MIN_COUNT = 1;
    public static final int TAGS_MAX_COUNT = 10;
    public static final int TAG_MAX_LENGTH = 25;

    public static final String MSG_TITLE_MAX_LENGTH = "Заголовок поста не должен превышать " + TITLE_MAX_LENGTH + " символов";
    public static final String MSG_TEXT_MAX_LENGTH = "Текст поста не должен превышать " + TEXT_MAX_LENGTH + " символов";
    public static final String MSG_TAG_MAX_LENGTH = "Длина тега не должна превышать " + TAG_MAX_LENGTH + " символов";
    public static final String MSG_TAGS_MIN_MAX_COUNT = "Можно указать от " + TAGS_MIN_COUNT + " до " + TAGS_MAX_COUNT + " тегов";

    public static final String MSG_ID_REQUIRED = "id поста обязателен";
    public static final String MSG_TITLE_REQUIRED = "Заголовок поста обязателен";
    public static final String MSG_TEXT_REQUIRED = "Текст поста обязателен";
    public static final String MSG_TAGS_NOT_NULL = "Теги обязательны";
}
