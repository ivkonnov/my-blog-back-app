package ru.yandex.practicum.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.domain.Post;
import ru.yandex.practicum.dto.NewPostDto;
import ru.yandex.practicum.dto.PostDto;
import ru.yandex.practicum.dto.PostPreviewDto;
import ru.yandex.practicum.dto.UpdatePostDto;

import static ru.yandex.practicum.util.PostPreviewUtil.getTextPreview;

@Component
public class PostMapper {

    public Post toPost(NewPostDto newPostDto) {
        return Post.builder()
                .title(newPostDto.title())
                .text(newPostDto.text())
                .tags(newPostDto.tags())
                .build();
    }

    public Post toPost(UpdatePostDto updatePostDto) {
        return Post.builder()
                .id(updatePostDto.id())
                .title(updatePostDto.title())
                .text(updatePostDto.text())
                .tags(updatePostDto.tags())
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

    public PostPreviewDto toPostPreviewDto(Post post) {
        return new PostPreviewDto(
                post.getId(),
                post.getTitle(),
                getTextPreview(post.getText()),
                post.getTags(),
                post.getLikesCount(),
                post.getCommentsCount()
        );
    }

}
