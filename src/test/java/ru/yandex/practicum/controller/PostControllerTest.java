package ru.yandex.practicum.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ru.yandex.practicum.dto.NewPostDto.*;

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
                .andExpect(jsonPath("$.tags", hasItems(tags.toArray())))
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
                .andExpect(jsonPath("$.title").value(MSG_TITLE_REQUIRED))
                .andExpect(jsonPath("$.text").value(MSG_TEXT_REQUIRED))
                .andExpect(jsonPath("$.tags").value(MSG_TAGS_REQUIRED));
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
                .andExpect(jsonPath("$.title").value(MSG_TITLE_MAX_LENGTH))
                .andExpect(jsonPath("$.text").value(MSG_TEXT_MAX_LENGTH))
                .andExpect(jsonPath("$.tags").value(MSG_TAG_MAX_LENGTH));
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
                .andExpect(jsonPath("$.tags").value(MSG_TAGS_MAX_COUNT));
    }

    @ParameterizedTest
    @CsvSource({
            "-ого #заметка поста #лонгрид, 1, 5, 2, 1",
            "-ого пост, 3, 5, 5, 3",
            "#заметка #лонгрид, 1, 5, 3, 1",
            "'', 2, 5, 5, 4",                     // если строка поиска пустая, то из всех постов будут взяты 5 постов со смещением для 2-ой страницы
            "несуществующий пост, 1, 5, 0, 0",    // если постов с таким заголовком не существует, то вернется пустая страница
            "##заметка, 1, 5, 0, 0",              // если тег начинается с ##, то вернется пустая страница
            "# #заметка, 1, 5, 5, 2"              // если тег пустой или введён случайно, то символ # игнорируем
    })
    void getPosts_success(String search, int pageNumber, int pageSize, int expectedPostsSize, int expectedLastPage) throws Exception {
        LinkedList<String> expectedTitleList = new LinkedList<>();
        List<String> expectedTags = new ArrayList<>();
        String expectedTitle = "";

        // формируем ожидаемый список тегов и подстроку заголовка, которые должны быть в каждом посте
        if (!search.isEmpty()) {
            String[] words = search.split(" ");
            for (String word : words) {
                // формируем ожидаемый список тегов
                if (word.startsWith("#")) {
                    if (word.length() > 1) expectedTags.add(word.substring(1));
                }
                else expectedTitleList.add(word);
            }
            // собираем подстроку ожидаемого заголовка
            expectedTitle = String.join(" ", expectedTitleList);
        }

        mockMvc.perform(get("/api/posts")
                    .param("search", search)
                    .param("pageNumber", String.valueOf(pageNumber))
                    .param("pageSize", String.valueOf(pageSize)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.posts", hasSize(expectedPostsSize)))
                .andExpect(jsonPath("$.posts[*].title", everyItem(containsString(expectedTitle))))
                .andExpect(jsonPath("$.posts[*].tags", everyItem(hasItems(expectedTags.toArray()))))
                .andExpect(jsonPath("$.hasPrev").value(pageNumber > 1))
                .andExpect(jsonPath("$.hasNext").value(pageNumber < expectedLastPage))
                .andExpect(jsonPath("$.lastPage").value(expectedLastPage));
    }

}
