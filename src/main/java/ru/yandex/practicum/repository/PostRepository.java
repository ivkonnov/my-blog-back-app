package ru.yandex.practicum.repository;

import ru.yandex.practicum.domain.Post;

import java.util.List;
import java.util.Optional;

public interface PostRepository {

    Long save(Post post);

    Optional<Post> findById(Long postId);

    Post update(Long postId, Post post);

    Long addLike(Long postId);

    Long countPosts();

    Long countPostsByTitleAndTags(String title, List<String> tags);

    Long countPostsByTitle(String title);

    Long countPostsByTags(List<String> tags);

    List<Post> findPagePosts(int limit, int offset);

    List<Post> findPagePostsByTitleAndTags(String title, List<String> tags, int limit, int offset);

    List<Post> findPagePostsByTitle(String title, int limit, int offset);

    List<Post> findPagePostsByTags(List<String> tags, int limit, int offset);

    boolean updateImage(Long postId, byte[] image);

    Optional<byte[]> findImageById(Long postId);

    boolean incrementCommentsCount(Long postId);

    boolean existsById(Long id);
}
