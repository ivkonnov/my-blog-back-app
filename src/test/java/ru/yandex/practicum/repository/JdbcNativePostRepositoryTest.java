package ru.yandex.practicum.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import ru.yandex.practicum.AbstractPostgresMvcTest;
import ru.yandex.practicum.domain.Post;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(JdbcNativePostRepository.class)
public class JdbcNativePostRepositoryTest extends AbstractPostgresMvcTest {

    @Autowired
    PostRepository postRepository;

    @Test
    void save_addPost() {
        Post newPost = new Post("Название 2-ого поста", "Контент 2-ого поста", List.of("пост_2", "пост_второй"));
        Long postId = postRepository.save(newPost);
        Optional<Post> postOptional = postRepository.findById(postId);

        assertTrue(postOptional.isPresent());
        Post post = postOptional.get();

        assertEquals(postId, post.getId());
        assertEquals("Название 2-ого поста", post.getTitle());
        assertEquals("Контент 2-ого поста", post.getText());
        assertEquals(2, post.getTags().size());
        assertTrue(post.getTags().contains("пост_2"));
        assertTrue(post.getTags().contains("пост_второй"));
        assertEquals(0L, post.getLikesCount());
        assertEquals(0L, post.getCommentsCount());
    }
}
