package ru.yandex.practicum.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.domain.Post;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Slf4j
@Repository
public class JdbcNativePostRepository implements PostRepository {

    private final NamedParameterJdbcTemplate nameParamJdbcTemplate;

    public JdbcNativePostRepository(NamedParameterJdbcTemplate nameParamJdbcTemplate) {
        this.nameParamJdbcTemplate = nameParamJdbcTemplate;
    }

    @Override
    public Long save(Post post) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("title", post.getTitle())
                .addValue("text", post.getText());

        // Вставка поста
        Long postId = nameParamJdbcTemplate.queryForObject(
                "INSERT INTO posts (title, text) VALUES (:title, :text) RETURNING id",
                params,
                Long.class
        );
        log.info("Post saved postId: {} title: {}", postId, post.getTitle());

        // Вставка тегов
        List<Long> tagIds = saveTags(post.getTags());
        // Сохранение связи пост-теги
        savePostTagLinks(postId, tagIds);

        return postId;
    }

    @Override
    public Post update(Long postId, Post updatePost) {
        String updateTitle = updatePost.getTitle();
        String updateText = updatePost.getText();

        // Получение текущего поста
        Post currentPost = findById(postId).orElseThrow();
        String currentTitle = currentPost.getTitle();
        String currentText = currentPost.getText();

        // Обновление названия поста, если оно было изменено
        if (!currentTitle.equals(updateTitle)) {
            log.info("Update postId{} to new title {}", postId, updateTitle);
            updateTitle(postId, updateTitle);
        }
        // Обновление контента поста, если он был изменен
        if (!currentText.equals(updateText)) {
            log.info("Update postId {} to new text {}", postId, updateText);
            updateText(postId, updateText);
        }

        List<String> currentTags = currentPost.getTags();
        List<String> updateTags = updatePost.getTags();

        // Сравнение тегов не учитывая дубликаты и порядок
        boolean tagsEqual = new HashSet<>(currentTags).equals(new HashSet<>(updateTags));

        /*
         Особенность фронта - при обновлении поста передаётся пустой список тегов [] в 2-х случаях:
           1) если теги удалили
           2) если теги НЕ изменяли (поле с тегами НЕ редактировали)
         В обоих случаях пустой список тегов [] трактуем как - теги не изменяли (поле с тегами не редактировали).

         А если теги НЕ пустые, то это значит:
           1) что теги изменили
           2) либо поле с тегами редактировали, но теги в итоге оставили без изменений
         */
        if (!updateTags.isEmpty() && !tagsEqual) {
            // Обновление тегов
            log.info("Update postId {} to new tags {}", postId, updateTags);

            // Получение id текущих тегов
            List<Long> currentTagIds = getTagIds(postId);
            log.info("Current tagIds: {}", currentTagIds);

            // Вставка новых тегов
            List<Long> updateTagIds = saveTags(updateTags);
            log.info("Update tagIds: {}", updateTagIds);

            // Сохраняем новые связи поста с новыми тегами
            savePostTagLinks(postId, updateTagIds);

            // Определяем теги, которые были удалены из поста
            List<Long> deleteOldTagIds = currentTagIds.stream()
                    .filter(currentTagId -> !updateTagIds.contains(currentTagId))
                    .toList();

            if (!deleteOldTagIds.isEmpty()) {
                // Удаляем связи поста с удаленными тегами
                deletePostTagLinks(postId, deleteOldTagIds);
                log.info("Post-tag links deleted for postId: {} tagIds: {}", postId, deleteOldTagIds);

                // Удаляем отвязанные от поста теги, в случае если связь тега с постом была удалена и тег не привязан к другим постам
                deleteUnlinkedTags(deleteOldTagIds);
                log.info("Unlinked tags deleted tagIds: {}", deleteOldTagIds);
            }
        }

        return findById(postId).orElseThrow();
    }

    private void updateTitle(Long postId, String updateTitle) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("updateTitle", updateTitle)
                .addValue("postId", postId);

        nameParamJdbcTemplate.update(
                "UPDATE posts SET title = :updateTitle WHERE id = :postId",
                params
        );
    }

    private void updateText(Long postId, String updateText) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("updateText", updateText)
                .addValue("postId", postId);

        nameParamJdbcTemplate.update(
                "UPDATE posts SET text = :updateText WHERE id = :postId",
                params
        );
    }

    private List<Long> saveTags(List<String> tags) {
        List<MapSqlParameterSource> batchTagParams = tags.stream()
                .map(tag -> new MapSqlParameterSource("tag", tag))
                .toList();

        // Вставка тегов
        nameParamJdbcTemplate.batchUpdate(
                "INSERT INTO tags (name) VALUES (:tag) ON CONFLICT (name) DO NOTHING",
                batchTagParams.toArray(MapSqlParameterSource[]::new)
        );

        MapSqlParameterSource tagsParams = new MapSqlParameterSource()
                .addValue("tags", tags);

        // Получаем id тегов
        List<Long> tagIds = nameParamJdbcTemplate.queryForList(
                "SELECT id FROM tags WHERE name IN (:tags)",
                tagsParams,
                Long.class
        );
        log.info("Tags saved tagIds: {}", tagIds);

        return tagIds;
    }

    private void deletePostTagLinks(Long postId, List<Long> tagIds) {
        MapSqlParameterSource tagIdsParams = new MapSqlParameterSource()
                .addValue("postId", postId)
                .addValue("tagIds", tagIds);

        // Удаляем связи пост-теги
        nameParamJdbcTemplate.update(
                "DELETE FROM posts_tags WHERE post_id = :postId AND tag_id IN (:tagIds)",
                tagIdsParams
        );
    }

    private void savePostTagLinks(Long postId, List<Long> tagIds) {
        List<MapSqlParameterSource> batchPostIdTagIdParams = tagIds.stream()
                .map(tagId -> new MapSqlParameterSource()
                        .addValue("postId", postId)
                        .addValue("tagId", tagId))
                .toList();

       // Сохраняем связи пост-теги
        nameParamJdbcTemplate.batchUpdate(
                "INSERT INTO posts_tags (post_id, tag_id) VALUES (:postId, :tagId) ON CONFLICT DO NOTHING",
                batchPostIdTagIdParams.toArray(MapSqlParameterSource[]::new)
        );
        log.info("Post-tag links saved for postId: {} tagIds: {}", postId, tagIds);
    }

    private List<Long> getTagIds(Long postId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("postId", postId);

        return nameParamJdbcTemplate.queryForList(
                "SELECT tag_id FROM posts_tags WHERE post_id = :postId",
                params,
                Long.class
        );
    }

    private void deleteUnlinkedTags(List<Long> tagIds) {
        MapSqlParameterSource unlinkedTagIdsParams = new MapSqlParameterSource("tagIds", tagIds);
        nameParamJdbcTemplate.update(
                """
                    DELETE FROM tags t
                    WHERE t.id IN (:tagIds) AND NOT EXISTS(
                        SELECT 1 FROM posts_tags pt
                        WHERE t.id = pt.tag_id
                    )
                """,
                unlinkedTagIdsParams
        );
    }

    @Override
    public Optional<Post> findById(Long postId) {
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("postId", postId);

            // Получаем пост
            Post post = nameParamJdbcTemplate.queryForObject(
                """
                    SELECT p.id, p.title, p.text, p.likes_count, p.comments_count, ARRAY_AGG(t.name) AS tag_names
                    FROM posts p
                        JOIN posts_tags pt ON p.id = pt.post_id
                        JOIN tags t ON pt.tag_id = t.id
                    WHERE p.id = :postId
                    GROUP BY p.id
                """,
                params,
                (resultSet, rowNum) ->
                    Post.builder()
                            .id(resultSet.getLong("id"))
                            .title(resultSet.getString("title"))
                            .text(resultSet.getString("text"))
                            .tags(getResultTags(resultSet))
                            .likesCount(resultSet.getLong("likes_count"))
                            .commentsCount(resultSet.getLong("comments_count"))
                            .build()
            );
            return Optional.of(post);
        } catch (EmptyResultDataAccessException e) {
            log.warn("Post not found id: {}", postId);
            return Optional.empty();
        }
    }

    @Override
    public Long countPosts() {
        MapSqlParameterSource params = new MapSqlParameterSource();
        return nameParamJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM posts",
                params,
                Long.class
        );
    }

    @Override
    public Long countPostsByTitleAndTags(String title, List<String> tags) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("title", "%" + title + "%")
                .addValue("tags", tags.toArray(String[]::new));

        return nameParamJdbcTemplate.queryForObject(
                """
                    SELECT COUNT(*)
                    FROM (
                        SELECT p.id, ARRAY_AGG(t.name) AS tag_names
                        FROM posts p
                            JOIN posts_tags pt ON p.id = pt.post_id
                            JOIN tags t ON pt.tag_id = t.id
                        WHERE p.title ILIKE :title
                        GROUP BY p.id
                        HAVING :tags <@ ARRAY_AGG(t.name)
                    ) AS subq
                """,
                params,
                Long.class
        );
    }

    @Override
    public Long countPostsByTitle(String title) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("title", "%" + title + "%");

        return nameParamJdbcTemplate.queryForObject(
                """
                    SELECT COUNT(*)
                    FROM (
                        SELECT p.id, ARRAY_AGG(t.name) AS tag_names
                        FROM posts p
                            JOIN posts_tags pt ON p.id = pt.post_id
                            JOIN tags t ON pt.tag_id = t.id
                        WHERE p.title ILIKE :title
                        GROUP BY p.id
                    )
                """,
                params,
                Long.class
        );
    }

    @Override
    public Long countPostsByTags(List<String> tags) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("tags", tags.toArray(String[]::new));

        return nameParamJdbcTemplate.queryForObject(
                """
                    SELECT COUNT(*)
                    FROM (
                        SELECT p.id, ARRAY_AGG(t.name) AS tag_names
                        FROM posts p
                            JOIN posts_tags pt ON p.id = pt.post_id
                            JOIN tags t ON pt.tag_id = t.id
                        GROUP BY p.id
                        HAVING :tags <@ ARRAY_AGG(t.name)
                    )
                """,
                params,
                Long.class
        );
    }

    @Override
    public List<Post> findPagePosts(int limit, int offset) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limit", limit)
                .addValue("offset", offset);

        return nameParamJdbcTemplate.query(
                """
                    SELECT p.id, p.title, p.text, p.likes_count, p.comments_count, ARRAY_AGG(t.name) AS tag_names
                    FROM posts p
                        JOIN posts_tags pt ON p.id = pt.post_id
                        JOIN tags t ON pt.tag_id = t.id
                    GROUP BY p.id
                    ORDER BY p.id
                    LIMIT :limit
                    OFFSET :offset
                """,
                params,
                (resultSet, rowNum) ->
                    Post.builder()
                            .id(resultSet.getLong("id"))
                            .title(resultSet.getString("title"))
                            .text(resultSet.getString("text"))
                            .tags(getResultTags(resultSet))
                            .likesCount(resultSet.getLong("likes_count"))
                            .commentsCount(resultSet.getLong("comments_count"))
                            .build()
        );
    }

    // пользователь может ввести запрос в любом регистре, поэтому использую ILIKE
    // для агрегирования тегов выбирал между ARRAY_AGG или JSONB_AGG, выбрал первое, потому что имеем по сути обычный массив строк
    // и если всё что в запросе фильтруем по "И", то проверяем и вхождение каждого тега из строки поиска <@ в массив тегов поста
    @Override
    public List<Post> findPagePostsByTitleAndTags(String title, List<String> tags, int limit, int offset) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("title", "%" + title + "%")
                .addValue("tags", tags.toArray(String[]::new))
                .addValue("limit", limit)
                .addValue("offset", offset);

        return nameParamJdbcTemplate.query(
                """    
                    SELECT p.id, p.title, p.text, p.likes_count, p.comments_count, ARRAY_AGG(t.name) AS tag_names
                    FROM posts p
                        JOIN posts_tags pt ON p.id = pt.post_id
                        JOIN tags t ON pt.tag_id = t.id
                    WHERE p.title ILIKE :title
                    GROUP BY p.id
                    HAVING :tags <@ ARRAY_AGG(t.name)
                    ORDER BY p.id
                    LIMIT :limit
                    OFFSET :offset
                """,
            params,
            (resultSet, rowNum) ->
                Post.builder()
                        .id(resultSet.getLong("id"))
                        .title(resultSet.getString("title"))
                        .text(resultSet.getString("text"))
                        .tags(getResultTags(resultSet))
                        .likesCount(resultSet.getLong("likes_count"))
                        .commentsCount(resultSet.getLong("comments_count"))
                        .build()
        );
    }

    @Override
    public List<Post> findPagePostsByTitle(String title, int limit, int offset) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("title", "%" + title + "%")
                .addValue("limit", limit)
                .addValue("offset", offset);

        return nameParamJdbcTemplate.query(
                """    
                    SELECT p.id, p.title, p.text, p.likes_count, p.comments_count, ARRAY_AGG(t.name) AS tag_names
                    FROM posts p
                        JOIN posts_tags pt ON p.id = pt.post_id
                        JOIN tags t ON pt.tag_id = t.id
                    WHERE p.title ILIKE :title
                    GROUP BY p.id
                    ORDER BY p.id
                    LIMIT :limit
                    OFFSET :offset
                """,
                params,
                (resultSet, rowNum) ->
                    Post.builder()
                            .id(resultSet.getLong("id"))
                            .title(resultSet.getString("title"))
                            .text(resultSet.getString("text"))
                            .tags(getResultTags(resultSet))
                            .likesCount(resultSet.getLong("likes_count"))
                            .commentsCount(resultSet.getLong("comments_count"))
                            .build()
        );
    }

    @Override
    public List<Post> findPagePostsByTags(List<String> tags, int limit, int offset) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("tags", tags.toArray(String[]::new))
                .addValue("limit", limit)
                .addValue("offset", offset);

        return nameParamJdbcTemplate.query(
                """    
                    SELECT p.id, p.title, p.text, p.likes_count, p.comments_count, ARRAY_AGG(t.name) AS tag_names
                    FROM posts p
                        JOIN posts_tags pt ON p.id = pt.post_id
                        JOIN tags t ON pt.tag_id = t.id
                    GROUP BY p.id
                    HAVING :tags <@ ARRAY_AGG(t.name)
                    ORDER BY p.id
                    LIMIT :limit
                    OFFSET :offset
                """,
                params,
                (resultSet, rowNum) ->
                    Post.builder()
                            .id(resultSet.getLong("id"))
                            .title(resultSet.getString("title"))
                            .text(resultSet.getString("text"))
                            .tags(getResultTags(resultSet))
                            .likesCount(resultSet.getLong("likes_count"))
                            .commentsCount(resultSet.getLong("comments_count"))
                            .build()
        );
    }

    @Override
    public boolean updateImage(Long postId, byte[] image) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("postId", postId)
                .addValue("image", image);

        int updated = nameParamJdbcTemplate.update(
                "UPDATE posts SET image = :image WHERE id = :postId",
                params
        );
        return updated > 0;
    }

    @Override
    public Optional<byte[]> findImageById(Long postId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("postId", postId);

        return nameParamJdbcTemplate.query(
                "SELECT image FROM posts WHERE id = :postId",
                params,
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
    public boolean existsById(Long postId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("postId", postId);

        Integer postsCount = nameParamJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM posts WHERE id = :postId",
                params,
                Integer.class
        );
        return postsCount != null && postsCount > 0;
    }

    private List<String> getResultTags(ResultSet resultSet) throws SQLException {
        List<String> tags = new ArrayList<>();
        Array array = resultSet.getArray("tag_names");
        if (array != null) {
            String[] tagsArray = (String[]) array.getArray();
            tags = Arrays.asList(tagsArray);
        }
        return tags;
    }

}