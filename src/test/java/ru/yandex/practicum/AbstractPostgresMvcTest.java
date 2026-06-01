package ru.yandex.practicum;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.containers.PostgreSQLContainer;
import ru.yandex.practicum.configuration.DataSourceConfiguration;
import ru.yandex.practicum.domain.Post;

import java.util.*;

@Slf4j
@SpringJUnitConfig(DataSourceConfiguration.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public abstract class AbstractPostgresMvcTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:18");

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
    }

    @BeforeAll
    static void startContainer() {
        postgresContainer.start();
    }

    @AfterAll
    static void stopContainer() {
        postgresContainer.stop();
    }

    /*
    Создаем 19 постов:
        - 4 поста с названием "Название i-ой публикации", из них:
            - 3 поста c тегами "пост_i" и "заметка"
            - 1 пост c тегами "пост_i", "заметка" и "лонгрид"

        - 15 постов с названием "Название i-ого поста, из них:"
            - 7 постов только с тегом "пост_i"
            - 3 поста c тегами "пост_i" и "заметка"
            - 3 поста c тегами "пост_i" и "лонгрид"
            - 2 поста с тегами "пост_i", "заметка" и "лонгрид"
    */
    @BeforeEach
    void setUp() {
        // Очистка таблиц и восстановление первичных ключей перед каждым тестом
        jdbcTemplate.execute("TRUNCATE TABLE posts_tags, posts, tags RESTART IDENTITY");

        for (int i = 1; i <= 19; i++) {
            String title = "Название " + i + "-ого поста";
            String text = "Контент " + i + "-ого поста";
            String tag = "пост_" + i;

            List<String> tags = new ArrayList<>();
            tags.add(tag);
            if (i % 2 == 0) tags.add("заметка");
            if (i % 3 == 0) tags.add("лонгрид");
            if (i % 4 == 0) {
                title = "Название " + i + "-ой публикации";
                text = "Контент " + i + "-ой публикации";
            }

            Post post = new Post(title, text, tags);

            // Сохраняем пост
            Long postId = jdbcTemplate.queryForObject(
                    "INSERT INTO posts (title, text) VALUES (?, ?) RETURNING id", Long.class,
                    post.getTitle(), post.getText()
            );
            log.info("Post for tests saved postId: {} title: {}", postId, post.getTitle());

            // Формируем для VALUES (?), (?), ...
            String placeholders = String.join(",", Collections.nCopies(post.getTags().size(), "(?)"));
            // Сохраняем теги
            jdbcTemplate.update(
                    "INSERT INTO tags (name) VALUES " + placeholders + " ON CONFLICT (name) DO NOTHING",
                    post.getTags().toArray()
            );

            // Формируем для IN (?, ?, ...)
            placeholders = String.join(",", Collections.nCopies(post.getTags().size(), "?"));

            // Получаем id тегов
            List<Long> tagIds = jdbcTemplate.queryForList(
                    "SELECT id FROM tags WHERE name IN (" + placeholders + ")",
                    Long.class,
                    post.getTags().toArray()
            );
            log.info("Tags for tests saved tagIds: {}", tagIds);

            // Сохраняем связи пост-тег
            for (Long tagId : tagIds) {
                jdbcTemplate.update(
                        "INSERT INTO posts_tags (post_id, tag_id) VALUES (?, ?)",
                        postId, tagId);
            }
            log.info("Post-tag links for tests saved for postId: {} tagIds: {}", postId, tagIds);
        }

    }

}