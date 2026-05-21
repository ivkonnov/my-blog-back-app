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

import java.util.List;

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

    @BeforeEach
    void setUp() {
        // Очистка таблиц и восстановление первичных ключей перед каждым тестом
        jdbcTemplate.execute("TRUNCATE TABLE posts_tags, posts, tags RESTART IDENTITY");

        Post post = new Post("Название 1-ого поста", "Контент 1-ого поста", List.of("пост_1", "пост_первый"));

        Long postId = jdbcTemplate.queryForObject(
                "INSERT INTO posts (title, text) VALUES (?, ?) RETURNING id", Long.class,
                post.getTitle(), post.getText()
        );
        log.info("Post for tests saved postId: {} title: {}", postId, post.getTitle());

        List<Long> tagIds = jdbcTemplate.queryForList(
                "INSERT INTO tags (name) VALUES (?), (?) RETURNING id",
                Long.class,
                post.getTags().toArray()
        );
        log.info("Tags for tests saved tagIds: {}", tagIds);

        for (Long tagId : tagIds) {
            jdbcTemplate.update(
                    "INSERT INTO posts_tags (post_id, tag_id) VALUES (?, ?)",
                    postId, tagId);
        }
        log.info("Post-tag links for tests saved for postId: {} tagIds: {}", postId, tagIds);

    }

}