package ru.yandex.practicum.exception;

import static ru.yandex.practicum.validation.PostValidationLimits.*;

public class ErrorMessages {

    public static final String MSG_TITLE_MAX_LENGTH = "Заголовок поста не должен превышать " + TITLE_MAX_LENGTH + " символов";
    public static final String MSG_TEXT_MAX_LENGTH = "Текст поста не должен превышать " + TEXT_MAX_LENGTH + " символов";
    public static final String MSG_TAG_MAX_LENGTH = "Длина тега не должна превышать " + TAG_MAX_LENGTH + " символов";
    public static final String MSG_TAGS_MIN_MAX_COUNT = "Можно указать от " + TAGS_MIN_COUNT + " до " + TAGS_MAX_COUNT + " тегов";
    public static final String MSG_COMMENT_MAX_LENGTH = "Текст комментария не должен превышать " + COMMENT_MAX_LENGTH + " символов";

    public static final String MSG_POST_ID_REQUIRED = "id поста обязателен";
    public static final String MSG_TITLE_REQUIRED = "Заголовок поста обязателен";
    public static final String MSG_TEXT_REQUIRED = "Текст поста обязателен";
    public static final String MSG_TAGS_NOT_NULL = "Теги обязательны";
    public static final String MSG_COMMENT_REQUIRED = "Текст комментария обязателен";


    public static final String MSG_DUPLICATE_KEY_EXCEPTION = "Запись с такими данными уже существует";
    public static final String MSG_DATA_INTEGRITY_VIOLATION_EXCEPTION = "Нарушение целостности данных (возможно, указан несуществующий ID)";
    public static final String MSG_INTERNAL_SERVER_ERROR = "Произошла непредвиденная ошибка на сервере";

    public static final String MSG_POST_NOT_FOUND = "Пост не найден";
    public static final String MSG_COMMENT_NOT_FOUND = "Комментарий не найден";
    public static final String MSG_IMAGE_NOT_FOUND = "Изображение не найдено";

    public static final String MSG_COMMENT_COUNT_UPDATE_ERROR = "Не удалось обновить счётчик комментариев для поста";
    public static final String MSG_COMMENT_ID_REQUIRED = "id комментария обязателен";

}
