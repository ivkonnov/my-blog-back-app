package ru.yandex.practicum.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Post {

    private Long id;
    private String title;
    private String text;
    private List<String> tags = new ArrayList<>();
    private Long likesCount = 0L;
    private Long commentsCount = 0L;
    private byte[] image;

    public Post() {}

    public Post(String title, String text, List<String> tags) {
        this.title = title;
        this.text = text;
        this.tags = tags;
    }

    public Post(Long id, String title, String text, Long likesCount, Long commentsCount) {
        this.id = id;
        this.title = title;
        this.text = text;
        this.likesCount = likesCount;
        this.commentsCount = commentsCount;
    }

    public Post(Long id, String title, String text, List<String> tags, Long likesCount, Long commentsCount) {
        this.id = id;
        this.title = title;
        this.text = text;
        this.tags = tags;
        this.likesCount = likesCount;
        this.commentsCount = commentsCount;
    }
}
