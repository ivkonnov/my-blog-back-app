package ru.yandex.practicum.service;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.practicum.AbstractPostgresMvcTest;
import ru.yandex.practicum.dto.CommentDto;
import ru.yandex.practicum.dto.NewCommentDto;
import ru.yandex.practicum.dto.PostDto;
import ru.yandex.practicum.dto.UpdateCommentDto;
import ru.yandex.practicum.exception.CommentNotFoundException;
import ru.yandex.practicum.exception.PostNotFoundException;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class CommentServiceTest extends AbstractPostgresMvcTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private PostService postService;


    @Nested
    class AddComment {
        @Test
        void addComment_and_checkCommentsCount_success() {
            Long postId = 2L;

            for (int i = 1; i <= 10; i++) {
                NewCommentDto newComment = new NewCommentDto(COMMENT_TEXT + i, postId);
                CommentDto commentDto = commentService.addComment(postId, newComment);

                assertNotNull(commentDto);
                assertTrue(commentDto.id() > 0);
                assertEquals(COMMENT_TEXT + i, commentDto.text());
                assertEquals(postId, commentDto.postId());

                // проверяем, что количество комментариев увеличилось на 1
                PostDto postDto = postService.getPost(postId);
                assertEquals(i, postDto.commentsCount());
            }
        }

        @Test
        void addComment_exceptionPostNotFound() {
            Long postId = POST_ID_NOT_FOUND;

            NewCommentDto newComment = new NewCommentDto(COMMENT_TEXT, postId);
            PostNotFoundException ex = assertThrows(
                    PostNotFoundException.class,
                    () -> commentService.addComment(postId, newComment)
            );
            assertEquals(postId, ex.getPostId());
        }
    }


    @Nested
    class GetComments {
        @Test
        void getComment() {
            Long postId = 1L;
            Long commentId = 1L;

            CommentDto commentDto = commentService.getComment(postId, commentId);
            assertNotNull(commentDto);
            assertEquals(commentId, commentDto.id());
            assertEquals(COMMENT_TEXT + commentId, commentDto.text());
            assertEquals(postId, commentDto.postId());
        }

        @Test
        void getComments() {
            Long postId = 1L;

            List<CommentDto> comments = commentService.getComments(postId);
            assertNotNull(comments);
            assertEquals(COUNT_COMMENTS_POST_1, comments.size());

            Long expectedCommentId = 1L;
            for (int i = 0; i < COUNT_COMMENTS_POST_1; i++) {
                assertEquals(expectedCommentId, comments.get(i).id());
                assertEquals(COMMENT_TEXT + expectedCommentId, comments.get(i).text());
                assertEquals(postId, comments.get(i).postId());
                expectedCommentId++;
            }
        }

        @Test
        void getComment_exceptionPostNotFound() {
            Long postId = POST_ID_NOT_FOUND;

            PostNotFoundException ex = assertThrows(
                    PostNotFoundException.class,
                    () -> commentService.getComment(postId, 1L)
            );
            assertEquals(postId, ex.getPostId());
        }

        @Test
        void getComment_exceptionCommentNotFound() {
            Long postId = 1L;
            Long commentId = COMMENT_ID_NOT_FOUND;

            CommentNotFoundException ex = assertThrows(
                    CommentNotFoundException.class,
                    () -> commentService.getComment(postId, commentId)
            );
            assertEquals(postId, ex.getPostId());
            assertEquals(commentId, ex.getCommentId());
        }
    }

    @Nested
    class UpdateComment {
        @Test
        void updateComment() {
            Long postId = 1L;
            Long commentId = 1L;

            UpdateCommentDto updateCommentDto = new UpdateCommentDto(commentId, NEW_COMMENT_TEXT, postId);
            CommentDto commentDto = commentService.updateComment(postId, commentId, updateCommentDto);

            assertNotNull(commentDto);
            assertEquals(commentId, commentDto.id());
            assertEquals(NEW_COMMENT_TEXT, commentDto.text());
            assertEquals(postId, commentDto.postId());
        }

        @Test
        void updateComment_exceptionPostNotFound() {
            Long postId = POST_ID_NOT_FOUND;
            Long commentId = 1L;

            UpdateCommentDto updateCommentDto = new UpdateCommentDto(commentId, NEW_COMMENT_TEXT, postId);
            PostNotFoundException ex = assertThrows(
                    PostNotFoundException.class,
                    () -> commentService.updateComment(postId, commentId, updateCommentDto)
            );
            assertEquals(postId, ex.getPostId());
        }

        @Test
        void updateComment_exceptionCommentNotFound() {
            Long postId = 1L;
            Long commentId = COMMENT_ID_NOT_FOUND;

            UpdateCommentDto updateCommentDto = new UpdateCommentDto(commentId, NEW_COMMENT_TEXT, postId);

            CommentNotFoundException ex = assertThrows(
                    CommentNotFoundException.class,
                    () -> commentService.updateComment(postId, commentId, updateCommentDto)
            );
            assertEquals(postId, ex.getPostId());
            assertEquals(commentId, ex.getCommentId());
        }

    }

    @Nested
    class DeleteComment {
        @Test
        void deleteComment() {
            Long postId = 1L;

            for (long commentId = 10; commentId >= 1; commentId--) {
                commentService.deleteComment(postId, commentId);
                // проверяем, что количество комментариев уменьшилось на 1
                PostDto postDto = postService.getPost(postId);
                long expectedCommentsCount = commentId - 1;
                assertEquals(expectedCommentsCount, postDto.commentsCount());
            }
        }

        @Test
        void deleteComment_exceptionPostNotFound() {
            Long postId = POST_ID_NOT_FOUND;

            PostNotFoundException ex = assertThrows(
                    PostNotFoundException.class,
                    () -> commentService.deleteComment(postId, 1L)
            );
            assertEquals(postId, ex.getPostId());
        }

        @Test
        void deleteComment_exceptionCommentNotFound() {
            Long postId = 1L;
            Long commentId = COMMENT_ID_NOT_FOUND;

            CommentNotFoundException ex = assertThrows(
                    CommentNotFoundException.class,
                    () -> commentService.deleteComment(postId, commentId)
            );
            assertEquals(postId, ex.getPostId());
            assertEquals(commentId, ex.getCommentId());
        }
    }

}
