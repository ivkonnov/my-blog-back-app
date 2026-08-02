package ru.yandex.practicum.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Tag {

    private Long id;

    private String name;

}
