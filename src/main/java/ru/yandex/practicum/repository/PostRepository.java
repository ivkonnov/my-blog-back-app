package ru.yandex.practicum.repository;

import ru.yandex.practicum.domain.Post;
import java.util.Optional;

public interface PostRepository {
    Long save(Post post);
    Optional<Post> findById(Long id);
}
