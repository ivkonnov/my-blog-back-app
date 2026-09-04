package ru.yandex.practicum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Slf4j
@SpringBootTest
@Transactional
public abstract class AbstractPostgresMvcTest {

    protected static final Long POST_ID_NOT_FOUND = 999L;

    protected static final Long COMMENT_ID_NOT_FOUND = 999L;

    protected static final String COMMENT_TEXT = "Комментарий ";

    protected static final String NEW_COMMENT_TEXT = "Новый комментарий";

    protected static final int COUNT_COMMENTS_POST_1 = 10;

    protected static final byte[] JPEG_IMAGE_STUB = new byte[]{(byte) 137, 80, 78, 71};

    @ServiceConnection
    private static final PostgreSQLContainer postgresContainer =
            new PostgreSQLContainer("postgres:18-alpine");

}