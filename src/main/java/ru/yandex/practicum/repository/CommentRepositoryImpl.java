package ru.yandex.practicum.repository;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.domain.Comment;

import java.util.Optional;

@Repository
public class CommentRepositoryImpl implements CommentRepository {

    private final NamedParameterJdbcTemplate nameParamJdbcTemplate;

    public CommentRepositoryImpl(NamedParameterJdbcTemplate nameParamJdbcTemplate) {
        this.nameParamJdbcTemplate = nameParamJdbcTemplate;
    }

    @Override
    public Long save(Long postId, Comment comment) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("text", comment.getText())
                .addValue("postId", postId);

        return nameParamJdbcTemplate.queryForObject(
                "INSERT INTO comments (text, post_id) VALUES (:text, :postId) RETURNING id",
                params,
                Long.class
        );
    }

    @Override
    public Optional<Comment> findById(Long postId, Long commentId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("commentId", commentId)
                .addValue("postId", postId);

        try {
            Comment comment = nameParamJdbcTemplate.queryForObject(
                "SELECT * FROM comments WHERE id = :commentId AND post_id = :postId",
                params,
                (resultSet, rowNum) ->
                    Comment.builder()
                            .id(resultSet.getLong("id"))
                            .text(resultSet.getString("text"))
                            .postId(resultSet.getLong("post_id"))
                            .build()
            );
            return Optional.ofNullable(comment);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

}
