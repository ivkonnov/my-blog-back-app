package ru.yandex.practicum.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.domain.Tag;

import java.util.List;

@Repository
public class TagRepositoryImpl implements TagRepository {

    private final NamedParameterJdbcTemplate nameParamJdbcTemplate;

    public TagRepositoryImpl(NamedParameterJdbcTemplate nameParamJdbcTemplate) {
        this.nameParamJdbcTemplate = nameParamJdbcTemplate;
    }

    @Override
    public List<Long> saveAll(List<String> names) {
        List<MapSqlParameterSource> batchTagNamesParams = names.stream()
                .map(name -> new MapSqlParameterSource("name", name))
                .toList();

        // Вставка тегов
        nameParamJdbcTemplate.batchUpdate(
                "INSERT INTO tags (name) VALUES (:name) ON CONFLICT (name) DO NOTHING",
                batchTagNamesParams.toArray(MapSqlParameterSource[]::new)
        );

        MapSqlParameterSource tagsParams = new MapSqlParameterSource()
                .addValue("names", names);

        // Возвращаем id тегов
        return nameParamJdbcTemplate.queryForList(
                """
                    SELECT id
                    FROM tags WHERE name IN (:names)
                """,
                tagsParams,
                Long.class
        );
    }

    @Override
    public List<Tag> findAllById(List<Long> tagIds) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("tagIds", tagIds);

        return nameParamJdbcTemplate.query(
                """
                    SELECT id, name FROM tags WHERE id IN (:tagIds)
                """,
                params,
                (resultSet, rowNum) -> Tag.builder()
                        .id(resultSet.getLong("id"))
                        .name(resultSet.getString("name"))
                        .build()
        );
    }

    @Override
    public List<Tag> findAllByPostId(Long postId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("postId", postId);

        return nameParamJdbcTemplate.query(
                """
                    SELECT id, name
                    FROM tags t JOIN posts_tags pt ON t.id = pt.tag_id WHERE pt.post_id = :postId
                """,
                params,
                (resultSet, rowNum) -> Tag.builder()
                        .id(resultSet.getLong("id"))
                        .name(resultSet.getString("name"))
                        .build()

        );
    }

    @Override
    public List<Long> cleanUnusedTags() {
        return nameParamJdbcTemplate.query(
                """
                    DELETE FROM tags t
                    WHERE NOT EXISTS (SELECT 1 FROM posts_tags pt WHERE t.id = pt.tag_id)
                    RETURNING t.id
                """,
                (resultSet, rowNum) -> resultSet.getLong("id")
        );
    }

}
