package ru.yandex.practicum.dto;

import java.util.List;

public interface PostData {
    String title();
    String text();
    List<String> tags();
}
