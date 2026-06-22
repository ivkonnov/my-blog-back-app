package ru.yandex.practicum.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.domain.Post;
import ru.yandex.practicum.dto.PostData;
import ru.yandex.practicum.dto.PostDto;

@Component
public class PostMapper {

    public Post toPost(PostData postData) {
        return Post.builder()
                .title(postData.title())
                .text(postData.text())
                .tags(postData.tags())
                .build();
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
