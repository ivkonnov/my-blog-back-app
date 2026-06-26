package ru.yandex.practicum.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.domain.Comment;
import ru.yandex.practicum.dto.CommentDto;
import ru.yandex.practicum.dto.NewCommentDto;

@Component
public class CommentMapper {

    public Comment toComment(NewCommentDto newCommentDto) {
        return Comment.builder()
                .text(newCommentDto.text())
                .postId(newCommentDto.postId())
                .build();
    }

    public CommentDto toCommentDto(Comment comment) {
        return new CommentDto(
                comment.getId(),
                comment.getText(),
                comment.getPostId()
        );
    }

}
