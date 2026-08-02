package ru.yandex.practicum.repository;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.practicum.AbstractPostgresMvcTest;
import ru.yandex.practicum.domain.Comment;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class CommentRepositoryTest extends AbstractPostgresMvcTest {

    @Autowired
    CommentRepository commentRepository;

    @Nested
    class addComment {
        @Test
        void addComment_success() {
            for (int i = 1; i < 10; i++) {
                Comment newComment = Comment.builder()
                        .text(COMMENT_TEXT + i)
                        .postId(1L)
                        .build();

                Long commentId = commentRepository.save(newComment.getPostId(), newComment);
                assertNotNull(commentId);

                Optional<Comment> commentOptional = commentRepository.findById(newComment.getPostId(), commentId);
                assertTrue(commentOptional.isPresent());

                Comment comment = commentOptional.get();
                assertEquals(commentId, comment.getId());
                assertEquals(newComment.getText(), comment.getText());
                assertEquals(newComment.getPostId(), comment.getPostId());
            }
        }
    }

    @Nested
    class getComments {
        @Test
        void getComment_success() {
            Long postId = 1L;
            Long commentId = 1L;

            Optional<Comment> commentOptional = commentRepository.findById(postId, commentId);
            assertTrue(commentOptional.isPresent());

            Comment comment = commentOptional.get();
            assertEquals(commentId, comment.getId());
            assertEquals(COMMENT_TEXT + commentId, comment.getText());
        }

        @ParameterizedTest
        @MethodSource("provideGetComment_returnEmpty")
        void getComment_returnEmpty_whenNotExists(Long postId, Long commentId) {
            Optional<Comment> commentOptional = commentRepository.findById(postId, commentId);
            assertTrue(commentOptional.isEmpty());
        }

        private static Stream<Arguments> provideGetComment_returnEmpty() {
            return Stream.of(
                    Arguments.of(POST_ID_NOT_FOUND, 1L),
                    Arguments.of(1L, COMMENT_ID_NOT_FOUND)
            );
        }

        @Test
        void getComments_success() {
            Long postId = 1L;
            List<Comment> comments = commentRepository.findAllByPostId(postId);
            assertNotNull(comments);
            assertFalse(comments.isEmpty());

            int expectedCommentId = 1;
            for (Comment comment : comments) {
                assertEquals(expectedCommentId, comment.getId());
                assertEquals(COMMENT_TEXT + expectedCommentId, comment.getText());
                assertEquals(postId, comment.getPostId());
                expectedCommentId++;
            }
        }

        @Test
        void getComments_returnEmpty() {
            Long postId = 2L;
            List<Comment> comments = commentRepository.findAllByPostId(postId);
            assertNotNull(comments);
            assertTrue(comments.isEmpty());
        }

        @Test
        void existsById_true_and_false() {
            Long postId = 1L;
            assertTrue(commentRepository.existsById(postId, 1L));
            assertFalse(commentRepository.existsById(postId, COMMENT_ID_NOT_FOUND));
        }
    }

    @Nested
    class UpdateComment {
        @Test
        void updateComment_success() {
            Long postId = 1L;
            Long commentId = 1L;

            Comment comment = Comment.builder()
                    .id(1L)
                    .text(NEW_COMMENT_TEXT)
                    .postId(1L)
                    .build();

            int updated = commentRepository.update(postId, commentId, comment);
            assertEquals(1, updated);

            Optional<Comment> commentOptional = commentRepository.findById(postId, commentId);
            assertTrue(commentOptional.isPresent());

            Comment updatedComment = commentOptional.get();
            assertEquals(comment.getId(), updatedComment.getId());
            assertEquals(comment.getText(), updatedComment.getText());
            assertEquals(comment.getPostId(), updatedComment.getPostId());
        }
    }

    @Nested
    class DeleteComment {
        @Test
        void deleteComment_success() {
            Long postId = 1L;
            Long commentId = 1L;
            int deleted = commentRepository.deleteById(postId, commentId);
            assertEquals(1, deleted);

            boolean isExists = commentRepository.existsById(postId, commentId);
            assertFalse(isExists);
        }
    }

}
