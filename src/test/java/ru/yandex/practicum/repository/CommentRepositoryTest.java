package ru.yandex.practicum.repository;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import ru.yandex.practicum.AbstractPostgresMvcTest;
import ru.yandex.practicum.domain.Comment;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(CommentRepositoryImpl.class)
public class CommentRepositoryTest extends AbstractPostgresMvcTest {

    @Autowired
    CommentRepository commentRepository;

    @BeforeAll
    static void setUpGenAddPosts(ApplicationContext context) {
        // генерируем и добавляем в базу данных 1 пост без комментариев
        setUpGenAddPosts(1);
    }

    @Nested
    class addComment {
        @Test
        void addComment_success() {
            for (int i = 1; i < 10; i++) {
                Comment newComment = Comment.builder()
                        .text("Комментарий " + i)
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
        @BeforeAll
        static void setUpAddComments() {
            // генерируем и добавляем в базу данных 10 комментариев к посту с id = 1
            setUpGenAddComments(1L, 10);
        }

        @Test
        void getComment_success() {
            Optional<Comment> commentOptional = commentRepository.findById(1L, 1L);
            assertTrue(commentOptional.isPresent());

            Comment comment = commentOptional.get();
            assertEquals(1L, comment.getId());
            assertEquals("Комментарий 1", comment.getText());
        }

        @ParameterizedTest
        @CsvSource({
                "1, 999",
                "999, 1"
        })
        void getComment_returnEmpty_whenNotExists(Long postId, Long commentId) {
            Optional<Comment> commentOptional = commentRepository.findById(postId, commentId);
            assertTrue(commentOptional.isEmpty());
        }
    }

}
