package ru.yandex.practicum.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.domain.Post;

import java.util.*;

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
            Post post = jdbcTemplate.queryForObject(
                """
                    SELECT p.id, p.title, p.text, p.likes_count, p.comments_count, ARRAY_AGG(t.name) AS tag_names
                    FROM posts p
                    JOIN posts_tags pt ON p.id = pt.post_id
                    JOIN tags t ON pt.tag_id = t.id
                    WHERE p.id = ?
                    GROUP BY p.id
                """,
                (resultSet, rowNum) -> {
                    String[] resultTags = (String[]) resultSet.getArray("tag_names").getArray();
                    List<String> tagList = Arrays.asList(resultTags);

                    return new Post(
                            resultSet.getLong("id"),
                            resultSet.getString("title"),
                            resultSet.getString("text"),
                            tagList,
                            resultSet.getLong("likes_count"),
                            resultSet.getLong("comments_count")
                    );
                },
                id
            );
            return Optional.of(post);
        } catch (EmptyResultDataAccessException e) {
            log.warn("Post not found id: {}", id);
            return Optional.empty();
        }
    }

    @Override
    public Long countPosts() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM posts",
                Long.class
        );
    }

    @Override
    public Long countPostsByTitleAndTags(String title, List<String> tags) {
        return jdbcTemplate.queryForObject(
                """
                    SELECT COUNT(*)
                    FROM (
                        SELECT p.id, ARRAY_AGG(t.name) AS tag_names
                        FROM posts p
                            JOIN posts_tags pt ON p.id = pt.post_id
                            JOIN tags t ON pt.tag_id = t.id
                        WHERE p.title ILIKE ?
                        GROUP BY p.id
                        HAVING ? <@ ARRAY_AGG(t.name)
                    )
                """,
                Long.class,
                "%" + title + "%",
                tags.toArray(String[]::new)
        );
    }

    @Override
    public Long countPostsByTitle(String title) {
        return jdbcTemplate.queryForObject(
                """
                    SELECT COUNT(*)
                    FROM (
                        SELECT p.id, ARRAY_AGG(t.name) AS tag_names
                        FROM posts p
                            JOIN posts_tags pt ON p.id = pt.post_id
                            JOIN tags t ON pt.tag_id = t.id
                        WHERE p.title ILIKE ?
                        GROUP BY p.id
                    )
                """,
                Long.class,
                "%" + title + "%"
        );
    }

    @Override
    public Long countPostsByTags(List<String> tags) {
        return jdbcTemplate.queryForObject(
                """
                    SELECT COUNT(*)
                    FROM (
                        SELECT p.id, ARRAY_AGG(t.name) AS tag_names
                        FROM posts p
                            JOIN posts_tags pt ON p.id = pt.post_id
                            JOIN tags t ON pt.tag_id = t.id
                        GROUP BY p.id
                        HAVING ? <@ ARRAY_AGG(t.name)
                    )
                """,
                Long.class,
                (Object) tags.toArray(String[]::new)
        );
    }

    @Override
    public List<Post> findPagePosts(int limit, int offset) {
        return jdbcTemplate.query(
                """
                    SELECT p.id, p.title, p.text, p.likes_count, p.comments_count, ARRAY_AGG(t.name) AS tag_names
                    FROM posts p
                    JOIN posts_tags pt ON p.id = pt.post_id
                    JOIN tags t ON pt.tag_id = t.id
                    GROUP BY p.id
                    ORDER BY p.id
                    LIMIT ?
                    OFFSET ?
                """,
                (resultSet, rowNum) -> {
                    String[] resultTags = (String[]) resultSet.getArray("tag_names").getArray();
                    List<String> tagList = Arrays.asList(resultTags);

                    return new Post(
                            resultSet.getLong("id"),
                            resultSet.getString("title"),
                            resultSet.getString("text"),
                            tagList,
                            resultSet.getLong("likes_count"),
                            resultSet.getLong("comments_count")
                    );
                },
                limit,
                offset
        );
    }

    // пользователь может ввести запрос в любом регистре, поэтому использую ILIKE
    // для агрегирования тегов выбирал между ARRAY_AGG или JSONB_AGG, выбрал первое, потому что имеем по сути обычный массив строк
    // и если всё что в запросе фильтруем по "И", то проверяем и вхождение каждого тега из строки поиска <@ в массив тегов поста
    @Override
    public List<Post> findPagePostsByTitleAndTags(String title, List<String> tags, int limit, int offset) {
        return jdbcTemplate.query(
                """    
                    SELECT p.id, p.title, p.text, p.likes_count, p.comments_count, ARRAY_AGG(t.name) AS tag_names
                    FROM posts p
                        JOIN posts_tags pt ON p.id = pt.post_id
                        JOIN tags t ON pt.tag_id = t.id
                    WHERE p.title ILIKE ?
                    GROUP BY p.id
                    HAVING ? <@ ARRAY_AGG(t.name)
                    ORDER BY p.id
                    LIMIT ?
                    OFFSET ?
                """,
            (resultSet, rowNum) -> {
                    String[] resultTags = (String[]) resultSet.getArray("tag_names").getArray();
                    List<String> tagList = Arrays.asList(resultTags);

                    return new Post(
                        resultSet.getLong("id"),
                        resultSet.getString("title"),
                        resultSet.getString("text"),
                        tagList,
                        resultSet.getLong("likes_count"),
                        resultSet.getLong("comments_count")
                    );
            },
            "%" + title + "%",
            tags.toArray(String[]::new),
            limit,
            offset
        );
    }

    @Override
    public List<Post> findPagePostsByTitle(String title, int limit, int offset) {
        return jdbcTemplate.query(
                """    
                    SELECT p.id, p.title, p.text, p.likes_count, p.comments_count, ARRAY_AGG(t.name) AS tag_names
                    FROM posts p
                        JOIN posts_tags pt ON p.id = pt.post_id
                        JOIN tags t ON pt.tag_id = t.id
                    WHERE p.title ILIKE ?
                    GROUP BY p.id
                    ORDER BY p.id
                    LIMIT ?
                    OFFSET ?
                """,
                (resultSet, rowNum) -> {
                    String[] resultTags = (String[]) resultSet.getArray("tag_names").getArray();
                    List<String> tagList = Arrays.asList(resultTags);

                    return new Post(
                            resultSet.getLong("id"),
                            resultSet.getString("title"),
                            resultSet.getString("text"),
                            tagList,
                            resultSet.getLong("likes_count"),
                            resultSet.getLong("comments_count")
                    );
                },
                "%" + title + "%",
                limit,
                offset
        );
    }

    @Override
    public List<Post> findPagePostsByTags(List<String> tags, int limit, int offset) {
        return jdbcTemplate.query(
                """    
                    SELECT p.id, p.title, p.text, p.likes_count, p.comments_count, ARRAY_AGG(t.name) AS tag_names
                    FROM posts p
                        JOIN posts_tags pt ON p.id = pt.post_id
                        JOIN tags t ON pt.tag_id = t.id
                    GROUP BY p.id
                    HAVING ? <@ ARRAY_AGG(t.name)
                    ORDER BY p.id
                    LIMIT ?
                    OFFSET ?
                """,
                (resultSet, rowNum) -> {
                    String[] resultTags = (String[]) resultSet.getArray("tag_names").getArray();
                    List<String> tagList = Arrays.asList(resultTags);

                    return new Post(
                            resultSet.getLong("id"),
                            resultSet.getString("title"),
                            resultSet.getString("text"),
                            tagList,
                            resultSet.getLong("likes_count"),
                            resultSet.getLong("comments_count")
                    );
                },
                tags.toArray(String[]::new),
                limit,
                offset
        );
    }

    @Override
    public boolean updateImage(Long id, byte[] image) {
        int updated = jdbcTemplate.update(
                "UPDATE posts SET image = ? WHERE id = ?",
                image, id
        );
        return updated > 0;
    }

    @Override
    public Optional<byte[]> findImageById(Long id) {
        return jdbcTemplate.query(
                "SELECT image FROM posts WHERE id = ?",
                preparedStatement -> preparedStatement.setLong(1, id),
                resultSet -> {
                    if (resultSet.next()) {
                        byte[] bytes = resultSet.getBytes("image");
                        if (bytes != null && bytes.length > 0) {
                            return Optional.of(bytes);
                        }
                    }
                    return Optional.empty();
                }
        );
    }

    @Override
    public boolean existsById(Long id) {
        Integer postsCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM posts WHERE id = ?",
                Integer.class,
                id
        );
        return postsCount != null && postsCount > 0;
    }

}