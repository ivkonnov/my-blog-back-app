package ru.yandex.practicum.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import ru.yandex.practicum.domain.Post;

import java.util.List;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(WebConfiguration.class)
@WebAppConfiguration
public class PostControllerTest extends AbstractPostgresMvcTest {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    void addPost_success() throws Exception {
        List<String> tags = List.of("пост_n", "пост_n-ый");
        Post newPost = new Post("Название n-ого поста", "Контент n-ого поста", tags);
        String newPostJson = objectMapper.writeValueAsString(newPost);

        mockMvc.perform(post("/api/posts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(newPostJson))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value(newPost.getTitle()))
                .andExpect(jsonPath("$.text").value(newPost.getText()))
                .andExpect(jsonPath("$.tags", hasSize(tags.size())))
                .andExpect(jsonPath("$.likesCount").value(newPost.getLikesCount()))
                .andExpect(jsonPath("$.commentsCount").value(newPost.getCommentsCount()));
    }

    @Test
    void addPost_notValid() throws Exception {
        Post newEmptyPost = new Post("", "", null);
        String newEmptyPostJson = objectMapper.writeValueAsString(newEmptyPost);

        mockMvc.perform(post("/api/posts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(newEmptyPostJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title").value("Заголовок поста обязателен"))
                .andExpect(jsonPath("$.text").value("Текст поста обязателен"))
                .andExpect(jsonPath("$.tags").value("Должен быть указан хотя бы один тег"));
    }

    @Test
    void addPost_notValidMaxLength() throws Exception {
        List<String> tags = List.of("a".repeat(26));
        Post newNotValidPost = new Post("a".repeat(129), "a".repeat(4097), tags);
        String newNotValidPostJson = objectMapper.writeValueAsString(newNotValidPost);

        mockMvc.perform(post("/api/posts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(newNotValidPostJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title").value("Заголовок поста не должен превышать 128 символов"))
                .andExpect(jsonPath("$.text").value("Текст поста не должен превышать 4096 символов"))
                .andExpect(jsonPath("$.tags").value("Длина тега не должна превышать 25 символов"));
    }

    @Test
    void addPost_notValidMaxCountTags() throws Exception {
        List<String> notValidMaxCountTags = IntStream.range(0,11)
                .mapToObj(i -> "\"tag\"")
                .toList();

        Post newNotValidPost = new Post("Название n-ого поста", "Контент n-ого поста", notValidMaxCountTags);
        String newNotValidPostJson = objectMapper.writeValueAsString(newNotValidPost);

        mockMvc.perform(post("/api/posts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(newNotValidPostJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.tags").value("Можно указать не более 10 тегов"));
    }

    @Test
    void getPosts_searchByTitleAndTags_1of1page_success() throws Exception {
        mockMvc.perform(get("/api/posts")
                    .param("search", "ого #заметка поста #лонгрид")
                    .param("pageNumber", "1")
                    .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.posts", hasSize(2)))
                .andExpect(jsonPath("$.hasPrev").value(false))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.lastPage").value(1));
    }

    @Test
    void getPosts_searchByTitle_3of3pages_success() throws Exception {
        mockMvc.perform(get("/api/posts")
                        .param("search", "-ого пост")
                        .param("pageNumber", "3")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.posts", hasSize(5)))
                .andExpect(jsonPath("$.hasPrev").value(true))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.lastPage").value(3));
    }

    @Test
    void getPosts_searchByTags_1of1page_success() throws Exception {
        mockMvc.perform(get("/api/posts")
                        .param("search", "#заметка #лонгрид")
                        .param("pageNumber", "1")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.posts", hasSize(3)))
                .andExpect(jsonPath("$.hasPrev").value(false))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.lastPage").value(1));
    }

    @Test
    void getPosts_ifSearchIsEmpty_2of4pages_success() throws Exception {
        mockMvc.perform(get("/api/posts")
                        .param("search","")
                        .param("pageNumber", "2")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.posts", hasSize(5)))
                .andExpect(jsonPath("$.hasPrev").value(true))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.lastPage").value(4));
    }

    @Test
    void getPosts_ifNotFoundPosts_returnEmptyPage_success() throws Exception {
        mockMvc.perform(get("/api/posts")
                        .param("search","несуществующий пост")
                        .param("pageNumber", "1")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.posts", hasSize(0)))
                .andExpect(jsonPath("$.hasPrev").value(false))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.lastPage").value(0));
    }
}
