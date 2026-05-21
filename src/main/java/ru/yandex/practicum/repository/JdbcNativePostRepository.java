package ru.yandex.practicum.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.domain.Post;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
public class JdbcNativePostRepository implements PostRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcNativePostRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Long save(Post post) {
        // Вставка поста
        Long postId = jdbcTemplate.queryForObject(
                "INSERT INTO posts (title, text) VALUES (?, ?) RETURNING id", Long.class,
                post.getTitle(),
                post.getText()
        );
        log.info("Post saved postId: {} title: {}", postId, post.getTitle());

        List<String> tags = post.getTags();
        // Вставка тегов
        List<Long> tagIds = saveTags(tags);
        // Сохранение связи пост-теги
        savePostTagLinks(postId, tagIds);

        return postId;
    }

    private List<Long> saveTags(List<String> tags) {
        // Вставка тегов
        jdbcTemplate.batchUpdate(
                "INSERT INTO tags (name) VALUES (?) ON CONFLICT (name) DO NOTHING",
                tags,
                100,
                (preparedStatement, tagName) -> preparedStatement.setString(1, tagName)
        );

        // Формируем для IN (?, ?, ...)
        String placeholders = String.join(",", Collections.nCopies(tags.size(), "?"));

        // Получаем id тегов
        List<Long> tagIds = jdbcTemplate.queryForList(
                "SELECT id FROM tags WHERE name IN (" + placeholders + ")",
                Long.class,
                tags.toArray()
        );
        log.info("Tags saved tagIds: {}", tagIds);

        return tagIds;
    }

    private void savePostTagLinks(Long postId, List<Long> tagIds) {
        // TODO: при добавлении ручки на обновление поста нужно будет удалять старые связи

        // Создаём связи пост-теги
        List<Object[]> batchArgs = tagIds.stream()
                .map(tagId -> new Object[]{postId, tagId})
                .toList();

       // Сохраняем связи пост-теги
        jdbcTemplate.batchUpdate(
                "INSERT INTO posts_tags (post_id, tag_id) VALUES (?, ?)",
                batchArgs
        );
        log.info("Post-tag links saved for postId: {} tagIds: {}", postId, tagIds);
   }

    @Override
    public Optional<Post> findById(Long id) {
        try {
            // Получаем пост
            Post post = jdbcTemplate.queryForObject("SELECT id, title, text, likes_count, comments_count FROM posts WHERE id = ?",
                    (resultSet, rowNum) ->
                            new Post(
                                    resultSet.getLong("id"),
                                    resultSet.getString("title"),
                                    resultSet.getString("text"),
                                    resultSet.getLong("likes_count"),
                                    resultSet.getLong("comments_count")
                            ),
                    id
            );

            // Получаем теги
            List<String> tags = jdbcTemplate.query("SELECT t.name FROM tags t JOIN posts_tags pt ON t.id = pt.tag_id WHERE pt.post_id = ?",
                    (resultSet, rowNum) -> resultSet.getString("name"),
                    id
            );
            // Устанавливаем теги в пост
            post.setTags(tags);

            log.info("Post found id: {} title: {}", id, post.getTitle());
            return Optional.of(post);
        } catch (EmptyResultDataAccessException e) {
            log.warn("Post not found id: {}", id);
            return Optional.empty();
        }
    }

}
