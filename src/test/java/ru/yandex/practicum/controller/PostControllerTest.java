package ru.yandex.practicum.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ru.yandex.practicum.dto.PostValidationConstants.*;

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
        void searchPosts_success(
                String search,
                int pageNumber,
                int pageSize,
                int expectedPostsSize,
                int expectedLastPage
        ) throws Exception {
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
        @ParameterizedTest
        @MethodSource("provideAddPost")
        void addPost_success(Post newPost) throws Exception {
            String newPostJson = objectMapper.writeValueAsString(newPost);

            mockMvc.perform(post("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(newPostJson))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.title").value(newPost.getTitle()))
                    .andExpect(jsonPath("$.text").value(newPost.getText()))
                    .andExpect(jsonPath("$.tags", hasSize(newPost.getTags().size())))
                    .andExpect(jsonPath("$.tags", hasItems(newPost.getTags().toArray())))
                    .andExpect(jsonPath("$.likesCount").value(newPost.getLikesCount()))
                    .andExpect(jsonPath("$.commentsCount").value(newPost.getCommentsCount()));
        }

        private static Stream<Arguments> provideAddPost() {
            String title = "Название n-ого поста";
            String text = "Контент n-ого поста";
            return Stream.of(
                    Arguments.of(Post.builder().title(title).text(text).tags(List.of("пост_n")).build()),
                    Arguments.of(Post.builder().title(title).text(text).tags(List.of("пост_n", "заметка")).build())
            );
        }

        @ParameterizedTest
        @MethodSource("provideAddPostNotValid")
        void addPost_notValid(
                Post newNotValidPost,
                String expectedMessageTagsNotValid
        ) throws Exception {
            String newNotValidPostJson = objectMapper.writeValueAsString(newNotValidPost);

            mockMvc.perform(post("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(newNotValidPostJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.title").value(MSG_TITLE_REQUIRED))
                    .andExpect(jsonPath("$.text").value(MSG_TEXT_REQUIRED))
                    .andExpect(jsonPath("$.tags").value(expectedMessageTagsNotValid));
        }

        private static Stream<Arguments> provideAddPostNotValid() {
            List<String> notValidMinCountTags = List.of();
            List<String> notValidMaxCountTags = IntStream.range(0, 11)
                    .mapToObj(i -> "\"tag\"")
                    .toList();

            return Stream.of(
                    Arguments.of(Post.builder().title(null).text(null).tags(null).build(), MSG_TAGS_NOT_NULL),
                    Arguments.of(Post.builder().title("").text("").tags(notValidMinCountTags).build(), MSG_TAGS_MIN_MAX_COUNT),
                    Arguments.of(Post.builder().title("").text("").tags(notValidMaxCountTags).build(), MSG_TAGS_MIN_MAX_COUNT)
            );
        }

        @Test
        void addPost_notValidMaxLength() throws Exception {
            List<String> tags = List.of("a".repeat(26));
            Post newNotValidPost = Post.builder()
                    .title("a".repeat(129))
                    .text("a".repeat(4097))
                    .tags(tags)
                    .build();

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
    }

    @Nested
    class GetPost {
        @BeforeAll
        static void setUp(ApplicationContext context) {
            // генерируем 2 поста
            setUpAddPosts(context, 2);
        }

        @ParameterizedTest
        @MethodSource("provideGetPost")
        void getPost_success(
                Long postId,
                Post expectedPost
        ) throws Exception {
            mockMvc.perform(get("/api/posts/{postId}", postId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(postId))
                    .andExpect(jsonPath("$.title").value(expectedPost.getTitle()))
                    .andExpect(jsonPath("$.text").value(expectedPost.getText()))
                    .andExpect(jsonPath("$.tags", hasSize(expectedPost.getTags().size())))
                    .andExpect(jsonPath("$.tags", hasItems(expectedPost.getTags().toArray())))
                    .andExpect(jsonPath("$.likesCount").value(expectedPost.getLikesCount()))
                    .andExpect(jsonPath("$.commentsCount").value(expectedPost.getCommentsCount()));
        }

        private static Stream<Arguments> provideGetPost() {
            return Stream.of(
                    Arguments.of(1L, Post.builder().id(1L).title("Название 1-ого поста").text("Контент 1-ого поста").tags(List.of("пост_1")).likesCount(0L).commentsCount(0L).build()),
                    Arguments.of(2L, Post.builder().id(2L).title("Название 2-ого поста").text("Контент 2-ого поста").tags(List.of("пост_2", "заметка")).likesCount(0L).commentsCount(0L).build())
            );
        }

        @Test
        void getPost_postNotFound() throws Exception {
            mockMvc.perform(get("/api/posts/{postId}", 999L))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class UpdatePost {
        @BeforeEach
        void setUp(ApplicationContext context) {
            // генерируем 2 поста
            setUpAddPosts(context, 2);
        }

        @ParameterizedTest
        @MethodSource("provideUpdatePost")
        void updatePost_success(
                Long postId,
                Post updatePost,
                List<String> expectedUpdateTags
        ) throws Exception {
            String updatePostJson = objectMapper.writeValueAsString(updatePost);

            mockMvc.perform(put("/api/posts/{postId}", postId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updatePostJson))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(postId))
                    .andExpect(jsonPath("$.title").value(updatePost.getTitle()))
                    .andExpect(jsonPath("$.text").value(updatePost.getText()))
                    .andExpect(jsonPath("$.tags", hasSize(expectedUpdateTags.size())))
                    .andExpect(jsonPath("$.tags", hasItems(expectedUpdateTags.toArray())));
        }

        private static Stream<Arguments> provideUpdatePost() {
            return Stream.of(
                    // обновляем только название и контент 1-ого поста с передачей пустого списка тегов [], что означает без изменений тегов
                    Arguments.of(1L, Post.builder().id(1L).title("Новое название").text("Новый контент").tags(List.of()).build(), List.of("пост_1")),

                    // обновляем все поля: название, контент и тег 1-ого поста
                    Arguments.of(1L, Post.builder().id(1L).title("Новое название").text("Новый контент").tags(List.of("новый_тег")).build(), List.of("новый_тег")),
                    // обновляем только тег 1-ого поста
                    Arguments.of(1L, Post.builder().id(1L).title("Название 1-ого поста").text("Контент 1-ого поста").tags(List.of("новый_тег")).build(), List.of("новый_тег")),

                    // если обновить 2-ой пост, но ничего не изменяя
                    Arguments.of(2L, Post.builder().id(2L).title("Название 2-ого поста").text("Контент 2-ого поста").tags(List.of("пост_2", "заметка")).build(), List.of("пост_2", "заметка")),
                    // обновляем только теги 2-ого поста, удалив 1 тег
                    Arguments.of(2L, Post.builder().id(2L).title("Название 2-ого поста").text("Контент 2-ого поста").tags(List.of("пост_2")).build(), List.of("пост_2")),
                    // обновляем только теги 2-ого поста, добавив 1 тег
                    Arguments.of(2L, Post.builder().id(2L).title("Название 2-ого поста").text("Контент 2-ого поста").tags(List.of("пост_2", "заметка", "новый_тег")).build(), List.of("пост_2", "заметка", "новый_тег")),

                    // обновляем только теги 2-ого поста, удалив 1 тег и добавив 1 тег
                    Arguments.of(2L, Post.builder().id(2L).title("Название 2-ого поста").text("Контент 2-ого поста").tags(List.of("пост_2", "новый_тег")).build(), List.of("пост_2", "новый_тег"))
            );
        }

        @Test
        void updatePost_postNotFound() throws Exception {
            Post updatePost = Post.builder()
                    .id(999L)
                    .title("Новое название")
                    .text("Новый контент")
                    .tags(List.of("новый_тег"))
                    .build();

            String updatePostJson = objectMapper.writeValueAsString(updatePost);

            mockMvc.perform(put("/api/posts/{postId}", 999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updatePostJson))
                    .andExpect(status().isNotFound());
        }

        @Test
        void updatePost_diffId_badRequest() throws Exception {
            Post updatePost = Post.builder()
                    .id(1L)
                    .title("Новое название")
                    .text("Новый контент")
                    .tags(List.of("новый_тег"))
                    .build();

            String updatePostJson = objectMapper.writeValueAsString(updatePost);

            mockMvc.perform(put("/api/posts/{postId}", 999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updatePostJson))
                    .andExpect(status().isBadRequest());
        }

        @ParameterizedTest
        @MethodSource("provideUpdatePostNotValid")
        void updatePost_notValid(
                Post updateNotValidPost,
                String expectedMessageTagsNotValid
        ) throws Exception {
            String updateNotValidPostJson = objectMapper.writeValueAsString(updateNotValidPost);

            mockMvc.perform(put("/api/posts/{postId}", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateNotValidPostJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(MSG_ID_REQUIRED))
                    .andExpect(jsonPath("$.title").value(MSG_TITLE_REQUIRED))
                    .andExpect(jsonPath("$.text").value(MSG_TEXT_REQUIRED))
                    .andExpect(jsonPath("$.tags").value(expectedMessageTagsNotValid));
        }

        private static Stream<Arguments> provideUpdatePostNotValid() {
            List<String> notValidMaxCountTags = IntStream.range(0, 11)
                    .mapToObj(i -> "\"tag\"")
                    .toList();

            return Stream.of(
                    Arguments.of(Post.builder().title(null).text(null).tags(null).build(), MSG_TAGS_NOT_NULL),
                    Arguments.of(Post.builder().title("").text("").tags(notValidMaxCountTags).build(), MSG_TAGS_MIN_MAX_COUNT)
            );
        }

        @Test
        void updatePost_notValidMaxLength() throws Exception {
            List<String> tags = List.of("a".repeat(26));
            Post updateNotValidPost = Post.builder()
                    .id(1L)
                    .title("a".repeat(129))
                    .text("a".repeat(4097))
                    .tags(tags)
                    .build();

            String updateNotValidPostJson = objectMapper.writeValueAsString(updateNotValidPost);

            mockMvc.perform(put("/api/posts/{postId}", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateNotValidPostJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.title").value(MSG_TITLE_MAX_LENGTH))
                    .andExpect(jsonPath("$.text").value(MSG_TEXT_MAX_LENGTH))
                    .andExpect(jsonPath("$.tags").value(MSG_TAG_MAX_LENGTH));
        }
    }

    @Nested
    class Likes {
        @BeforeEach
        void setUp(ApplicationContext context) {
            // генерируем 1 пост
            setUpAddPosts(context, 1);
        }

        @Test
        void like_success() throws Exception {
            for (int i = 1; i < 10; i++) {
                mockMvc.perform(post("/api/posts/{postId}/likes", 1L))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(content().string(String.valueOf(i)));
            }
        }

        @Test
        void like_postNotFound() throws Exception {
            for (int i = 1; i < 10; i++) {
                mockMvc.perform(post("/api/posts/{postId}/likes", 999L))
                        .andExpect(status().isNotFound());
            }
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