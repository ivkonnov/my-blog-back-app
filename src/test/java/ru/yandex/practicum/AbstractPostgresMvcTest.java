package ru.yandex.practicum;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.yandex.practicum.configuration.DataSourceConfiguration;
import ru.yandex.practicum.domain.Post;

import java.util.*;

import static ru.yandex.practicum.util.PostPreviewDisplay.POST_TEXT_PREVIEW_MAX_LENGTH;

@Slf4j
@Testcontainers
@SpringJUnitConfig(DataSourceConfiguration.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public abstract class AbstractPostgresMvcTest {

    @Container
    private static final PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:18");

    protected static NamedParameterJdbcTemplate nameParamJdbcTemplate;

    protected static final Long NOT_EXIST_POST_ID = 999L;

    protected static final Long NOT_EXIST_COMMENT_ID = 999L;

    protected static final String COMMENT_TEXT = "Комментарий";

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
    }

    @BeforeAll
    static void setUp(ApplicationContext context) {
        nameParamJdbcTemplate = context.getBean(NamedParameterJdbcTemplate.class);
    }

    /*
    Если сгенерируем 19 постов для тестов класса SearchPosts, то получим:
        1   title: Название 1-ого поста       text: Контент 1-ого поста       tags: [пост_1]
        2   title: Название 2-ого поста       text: Контент 2-ого поста       tags: [пост_2, заметка]
        3   title: Название 3-ого поста       text: Контент 3-ого поста       tags: [пост_3, лонгрид]
        4   title: Название 4-ой публикации   text: > 128 символов            tags: [пост_4, заметка]
        5   title: Название 5-ого поста       text: Контент 5-ого поста       tags: [пост_5]
        6   title: Название 6-ого поста       text: Контент 6-ого поста       tags: [пост_6, заметка, лонгрид]
        7   title: Название 7-ого поста       text: Контент 7-ого поста       tags: [пост_7]
        8   title: Название 8-ой публикации   text: > 128 символов            tags: [пост_8, заметка]
        9   title: Название 9-ого поста       text: Контент 9-ого поста       tags: [пост_9, лонгрид]
        10  title: Название 10-ого поста      text: Контент 10-ого поста      tags: [пост_10, заметка]
        11  title: Название 11-ого поста      text: Контент 11-ого поста      tags: [пост_11]
        12  title: Название 12-ой публикации  text: > 128 символов            tags: [пост_12, заметка, лонгрид]
        13  title: Название 13-ого поста      text: Контент 13-ого поста      tags: [пост_13]
        14  title: Название 14-ого поста      text: Контент 14-ого поста      tags: [пост_14, заметка]
        15  title: Название 15-ого поста      text: Контент 15-ого поста      tags: [пост_15, лонгрид]
        16  title: Название 16-ой публикации  text: > 128 символов            tags: [пост_16, заметка]
        17  title: Название 17-ого поста      text: Контент 17-ого поста      tags: [пост_17]
        18  title: Название 18-ого поста      text: Контент 18-ого поста      tags: [пост_18, заметка, лонгрид]
        19  title: Название 19-ого поста      text: Контент 19-ого поста      tags: [пост_19]

        Итого:
        - 4 поста с названием "Название i-ой публикации" и контентом > 128 символов, из них:
            - 3 поста c тегами "пост_i" и "заметка"
            - 1 пост c тегами "пост_i", "заметка" и "лонгрид"

        - 15 постов с названием "Название i-ого поста, из них:"
            - 7 постов только с тегом "пост_i"
            - 3 поста c тегами "пост_i" и "заметка"
            - 3 поста c тегами "пост_i" и "лонгрид"
            - 2 поста с тегами "пост_i", "заметка" и "лонгрид"
    */
    protected static void setUpGenAddPosts(int countPosts) {
        // Очистка таблиц и восстановление первичных ключей перед каждым тестом
        nameParamJdbcTemplate.update(
                "TRUNCATE TABLE posts_tags, posts, tags RESTART IDENTITY CASCADE",
                new MapSqlParameterSource()
        );

        List<Post> posts = generationPosts(countPosts);

        for (Post post : posts) {
            MapSqlParameterSource titleTextParams = new MapSqlParameterSource()
                    .addValue("title", post.getTitle())
                    .addValue("text", post.getText());

            // Сохраняем пост
            Long postId = nameParamJdbcTemplate.queryForObject(
                    "INSERT INTO posts (title, text) VALUES (:title, :text) RETURNING id",
                    titleTextParams,
                    Long.class
            );
            log.info("Post for tests saved postId: {} title: {}", postId, post.getTitle());

            List<MapSqlParameterSource> batchTagParams = post.getTags().stream()
                    .map(tag -> new MapSqlParameterSource("tag", tag))
                    .toList();

            // Сохраняем теги
            nameParamJdbcTemplate.batchUpdate(
                    "INSERT INTO tags (name) VALUES (:tag) ON CONFLICT (name) DO NOTHING",
                    batchTagParams.toArray(MapSqlParameterSource[]::new)
            );

            MapSqlParameterSource tagsParams = new MapSqlParameterSource()
                    .addValue("tags", post.getTags());

            // Получаем id тегов
            List<Long> tagIds = nameParamJdbcTemplate.queryForList(
                    "SELECT id FROM tags WHERE name IN (:tags)",
                    tagsParams,
                    Long.class
            );
            log.info("Tags for tests saved tagIds: {}", tagIds);

            // Сохраняем связи пост-тег
            for (Long tagId : tagIds) {
                MapSqlParameterSource postIdTagIdParams = new MapSqlParameterSource()
                        .addValue("postId", postId)
                        .addValue("tagId", tagId);

                nameParamJdbcTemplate.update(
                        "INSERT INTO posts_tags (post_id, tag_id) VALUES (:postId, :tagId)",
                        postIdTagIdParams
                );
            }
            log.info("Post-tag links for tests saved for postId: {} tagIds: {}", postId, tagIds);
        }
    }

    private static List<Post> generationPosts(int countPosts) {
        List<Post> posts = new ArrayList<>();

        for (int i = 1; i <= countPosts; i++) {
            // Название и контент поста по умолчанию
            String title = "Название " + i + "-ого поста";
            String text = "Контент " + i + "-ого поста";
            // Каждый пост содержит тег "пост_i" по умолчанию
            String tag = "пост_" + i;

            List<String> tags = new ArrayList<>();
            tags.add(tag);

            // Каждый 2-ой пост кроме тега "пост_i" будет содержать ещё тег "заметка"
            if (i % 2 == 0) tags.add("заметка");

            // Каждый 3-ий пост кроме тега "пост_i" будет содержать ещё тег "лонгрид"
            if (i % 3 == 0) tags.add("лонгрид");

            // Каждый 4-ый пост будет публикацией с контентом длиннее 128 символов
            if (i % 4 == 0) {
                title = title.replace("-ого поста", "-ой публикации");
                text =  "a".repeat(POST_TEXT_PREVIEW_MAX_LENGTH + 1);
            }

            Post post = Post.builder()
                    .title(title)
                    .text(text)
                    .tags(tags)
                    .build();

            posts.add(post);

            log.info("Generated post for tests title: {} text: {} tags: {}", post.getTitle(), post.getText(), post.getTags());
        }
        return posts;
    }

    protected static void setUpGenAddComments(Long postId, int countComments) {
        nameParamJdbcTemplate.update(
                "TRUNCATE TABLE comments RESTART IDENTITY",
                new MapSqlParameterSource()
        );

        List<MapSqlParameterSource> batchCommentsParams = new ArrayList<>();
        for (int i = 1; i <= countComments; i++) {
            MapSqlParameterSource commentParams = new MapSqlParameterSource()
                    .addValue("text", COMMENT_TEXT + i)
                    .addValue("postId", postId);

            batchCommentsParams.add(commentParams);
        }

        // Сохраняем теги
        nameParamJdbcTemplate.batchUpdate(
                "INSERT INTO comments (text, post_id) VALUES (:text, :postId)",
                batchCommentsParams.toArray(MapSqlParameterSource[]::new)
        );
    }

}