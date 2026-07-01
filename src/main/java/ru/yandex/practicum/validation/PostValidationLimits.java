package ru.yandex.practicum.validation;

public class PostValidationLimits {

    /*
         В задании не сказано, но логично, что необходимы лимиты, чтобы и в схеме БД указать ограничения
         и чтобы не было проблем, если вдруг передадутся слишком длинные значения.
         В реальности же нужно уточнять у проджекта или аналитика, но для примера пока выставил примерные лимиты соцсетей
     */

    public static final int TITLE_MAX_LENGTH = 128;
    public static final int TEXT_MAX_LENGTH = 4096;
    public static final int TAGS_MIN_COUNT = 1;
    public static final int TAGS_MAX_COUNT = 10;
    public static final int TAG_MAX_LENGTH = 25;
    public static final int COMMENT_MAX_LENGTH = 460;

}
