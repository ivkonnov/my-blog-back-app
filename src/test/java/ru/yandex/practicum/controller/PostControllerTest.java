package ru.yandex.practicum.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.yandex.practicum.AbstractPostgresMvcTest;
import ru.yandex.practicum.configuration.WebConfiguration;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(WebConfiguration.class)
@WebAppConfiguration
public class PostControllerTest extends AbstractPostgresMvcTest {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    void addPost_success() throws Exception {
        String newPost = """
            {"title": "Название 2-ого поста", "text": "Контент 2-ого поста", "tags": ["пост_2", "пост_второй"]}
        """;

        mockMvc.perform(post("/api/posts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(newPost))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.title").value("Название 2-ого поста"))
                .andExpect(jsonPath("$.text").value("Контент 2-ого поста"))
                .andExpect(jsonPath("$.tags", hasSize(2)))
                .andExpect(jsonPath("$.likesCount").value(0L))
                .andExpect(jsonPath("$.commentsCount").value(0L));
    }

    @Test
    void addPost_notValid() throws Exception {
        String newEmptyPost = """
            {"title": "", "text": "", "tags": []}
        """;

        mockMvc.perform(post("/api/posts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(newEmptyPost))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title").value("Заголовок поста обязателен"))
                .andExpect(jsonPath("$.text").value("Текст поста обязателен"))
                .andExpect(jsonPath("$.tags").value("Должен быть указан хотя бы один тег"));
    }

    @Test
    void addPost_notValidMaxLength() throws Exception {
        String newNotValidPost = "{" +
                "\"title\": \"" + "a".repeat(129) + "\", " +
                "\"text\": \"" + "a".repeat(4097) + "\", " +
                "\"tags\": [\"" + "a".repeat(26) + "\"]" +
        "}";

        mockMvc.perform(post("/api/posts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(newNotValidPost))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title").value("Заголовок поста не должен превышать 128 символов"))
                .andExpect(jsonPath("$.text").value("Текст поста не должен превышать 4096 символов"))
                .andExpect(jsonPath("$.tags").value("Длина тега не должна превышать 25 символов"));
    }

    @Test
    void addPost_notValidMaxCountTags() throws Exception {
        String notValidMaxCountTags = IntStream.range(0,11)
                .mapToObj(i -> "\"a\"")
                .collect(Collectors.joining(","));

        String newNotValidPost = "{" +
                "\"title\": \"a\", " +
                "\"text\": \"a\", " +
                "\"tags\": [" + notValidMaxCountTags + "]" +
        "}";

        mockMvc.perform(post("/api/posts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(newNotValidPost))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.tags").value("Можно указать не более 10 тегов"));
    }

}
