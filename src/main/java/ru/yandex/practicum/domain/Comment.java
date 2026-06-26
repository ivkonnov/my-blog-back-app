package ru.yandex.practicum.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Comment {

    private Long id;

    private String text;

    private Long postId;

}
