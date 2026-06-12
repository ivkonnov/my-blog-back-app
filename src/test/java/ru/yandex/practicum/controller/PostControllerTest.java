package ru.yandex.practicum.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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

    @Nested
    class SearchPosts {
        @BeforeAll
        static void setUp(ApplicationContext context) {
            // генерируем 19 постов
            setUpAddPosts(context, 19);
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
        void searchPosts_success(String search, int pageNumber, int pageSize, int expectedPostsSize, int expectedLastPage) throws Exception {
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
                    } else expectedTitleList.add(word);
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

    @Nested
    class AddPost {
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
            List<String> notValidMaxCountTags = IntStream.range(0, 11)
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
    }

    @Nested
    class GetPost {
        @BeforeAll
        static void setUp(ApplicationContext context) {
            // генерируем 1 пост
            setUpAddPosts(context, 1);
        }

        @Test
        void getPost_success() throws Exception {
            mockMvc.perform(get("/api/posts/{postId}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.title").value("Название 1-ого поста"))
                    .andExpect(jsonPath("$.text").value("Контент 1-ого поста"))
                    .andExpect(jsonPath("$.tags", hasSize(1)))
                    .andExpect(jsonPath("$.tags", hasItem("пост_1")))
                    .andExpect(jsonPath("$.likesCount").value(0))
                    .andExpect(jsonPath("$.commentsCount").value(0));
        }

        @Test
        void getPost_notFound() throws Exception {
            mockMvc.perform(get("/api/posts/{postId}", 999L))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class UpdateAndGetImage {
        @BeforeEach
        void setUp(ApplicationContext context) {
            // генерируем 1 пост
            setUpAddPosts(context, 1);
        }

        @Test
        void updateAndGetImage_success() throws Exception {
            byte[] jpegStub = new byte[]{(byte) 137, 80, 78, 71};
            MockMultipartFile image = new MockMultipartFile("image", "image.jpg", "image/jpeg", jpegStub);

            mockMvc.perform(MockMvcRequestBuilders.multipart("/api/posts/{postId}/image", 1L)
                            .file(image)
                            .with(request -> {
                                request.setMethod("PUT");
                                return request;
                            }))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/posts/{postId}/image", 1L))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                    .andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(content().bytes(jpegStub));

        }

        @Test
        void updateImage_emptyFile_badRequest() throws Exception {
            MockMultipartFile emptyImage = new MockMultipartFile("image", "image.jpg", "image/jpeg", new byte[0]);

            mockMvc.perform(multipart("/api/posts/{postId}/image", 1L)
                            .file(emptyImage)
                            .with(request -> {
                                request.setMethod("PUT");
                                return request;
                            }))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Empty image"));
        }

        @Test
        void updateImage_postNotFound_404() throws Exception {
            MockMultipartFile image = new MockMultipartFile("image", "image.jpg", "image/jpeg", new byte[]{1, 2, 3});

            mockMvc.perform(multipart("/api/posts/{postId}/image", 999L)
                            .file(image)
                            .with(request -> {
                                request.setMethod("PUT");
                                return request;
                            }))
                    .andExpect(status().isNotFound())
                    .andExpect(content().string("Post not found"));
        }

        @Test
        void getImage_postHasNoImage_404() throws Exception {
            mockMvc.perform(get("/api/posts/{postId}/image", 1L))
                    .andExpect(status().isNotFound());
        }

        @Test
        void getImage_postNotFound_404() throws Exception {
            mockMvc.perform(get("/api/posts/{postId}/image", 999L))
                    .andExpect(status().isNotFound());
        }
    }

}