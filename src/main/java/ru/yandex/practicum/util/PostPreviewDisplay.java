package ru.yandex.practicum.util;

public class PostPreviewDisplay {

    public static final int POST_TEXT_PREVIEW_MAX_LENGTH = 128;

    public static final String ELLIPSIS = "...";

    // если текст больше 128 символов, то он обрезается до 128 символов и добавляется «…»
    public static String getTextPreview(String previewText) {
        if (previewText != null && previewText.length() > POST_TEXT_PREVIEW_MAX_LENGTH)
            previewText = previewText.substring(0, POST_TEXT_PREVIEW_MAX_LENGTH) + ELLIPSIS;
        return previewText;

    }
}
