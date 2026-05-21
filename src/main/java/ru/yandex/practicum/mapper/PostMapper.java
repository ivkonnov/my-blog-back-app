package ru.yandex.practicum.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.domain.Post;
import ru.yandex.practicum.dto.NewPostDto;
import ru.yandex.practicum.dto.PostDto;

@Component
public class PostMapper {

    public Post toPost(NewPostDto newPostDto) {
        return new Post(
                newPostDto.title(),
                newPostDto.text(),
                newPostDto.tags()
        );
    }

    public PostDto toPostDto(Post post) {
        return new PostDto(
                post.getId(),
                post.getTitle(),
                post.getText(),
                post.getTags(),
                post.getLikesCount(),
                post.getCommentsCount()
        );
    }
}
