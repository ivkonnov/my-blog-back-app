package ru.yandex.practicum.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
public class Post {

    private Long id;

    private String title;

    private String text;

    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Builder.Default
    private Long likesCount = 0L;

    @Builder.Default
    private Long commentsCount = 0L;

    private byte[] image;

}
