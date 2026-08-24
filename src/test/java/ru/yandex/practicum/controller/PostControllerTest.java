package ru.yandex.practicum.controller;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.yandex.practicum.AbstractPostgresMvcTest;
import ru.yandex.practicum.dto.*;
import ru.yandex.practicum.service.PostService;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ru.yandex.practicum.util.PostPreviewUtil.*;
import static ru.yandex.practicum.validation.PostValidationLimits.*;
import static ru.yandex.practicum.exception.ErrorMessages.*;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class PostControllerTest extends AbstractPostgresMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoSpyBean
    PostService postService;

    @Nested
    class FindPagePostsBySearch {
        @ParameterizedTest
        @MethodSource("provideFindPagePostsBySearch")
        void findPagePostsBySearch_success(
                String search,
                int pageNumber,
                int pageSize,
                String expectedPartOfTitleFromQuery,
                List<String> expectedTagsFromQuery,
                int expectedPostsSize,
                int expectedLastPage
        ) throws Exception {
            // регулярное выражение для проверки длины превью поста
            String regexExpectedTextPreviewMaxLength = "^.{0," + (POST_TEXT_PREVIEW_MAX_LENGTH + ELLIPSIS.length()) + "}$";

            mockMvc.perform(get("/api/posts")
                            .param("search", search)
                            .param("pageNumber", String.valueOf(pageNumber))
                            .param("pageSize", String.valueOf(pageSize)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.posts", hasSize(expectedPostsSize)))
                    .andExpect(jsonPath("$.posts[*].title", everyItem(containsString(expectedPartOfTitleFromQuery))))
                    .andExpect(jsonPath("$.posts[*].text").value(everyItem(matchesRegex(regexExpectedTextPreviewMaxLength))))
                    .andExpect(jsonPath("$.posts[*].tags", everyItem(hasItems(expectedTagsFromQuery.toArray()))))
                    .andExpect(jsonPath("$.hasPrev").value(pageNumber > 1))
                    .andExpect(jsonPath("$.hasNext").value(pageNumber < expectedLastPage))
                    .andExpect(jsonPath("$.lastPage").value(expectedLastPage));
        }

        private static Stream<Arguments> provideFindPagePostsBySearch() {
            return Stream.of(
                    // поиск по частям названия и тегам
                    Arguments.of("-ого #заметка поста #лонгрид", 1, 5, "-ого поста", List.of("заметка", "лонгрид"), 2, 1),

                    // поиск по части названия
                    Arguments.of("-ого пост", 3, 5, "-ого пост", List.of(), 5, 3),

                    // поиск по тегам
                    Arguments.of("#заметка #лонгрид", 1, 5, "", List.of("заметка", "лонгрид"), 3, 1),

                    // если строка поиска пустая, то будут найдены все посты и из них будут взяты 5 постов со смещением для 2-ой страницы
                    Arguments.of("", 2, 5, "", List.of(), 5, 4),

                    // если постов с таким заголовком не существует, то вернется пустая страница
                    Arguments.of("несуществующий пост", 1, 5, "", List.of(), 0, 0),

                    // если тег начинается с опечатки ##, то лишние символы # игнорируются и поиск по тегу происходит корректно
                    Arguments.of("##заметка", 1, 5, "", List.of("заметка"), 5, 2),

                    // если тег пустой или введён случайно, то символ # игнорируем
                    Arguments.of("# #заметка", 1, 5, "", List.of("заметка"), 5, 2),

                    // если в строке поиска есть лишние пробелы, то пробелы игнорируются
                    Arguments.of("  #заметка   публикац", 1, 5, "публикац", List.of("заметка"), 4, 1)
            );
        }
    }

    @Nested
    class AddPost {
        @ParameterizedTest
        @MethodSource("provideAddPost")
        void addPost_success(
                NewPostDto newPost,
                Long expectedLikesCount,
                Long expectedCommentsCount
        ) throws Exception {
            String newPostJson = objectMapper.writeValueAsString(newPost);

            mockMvc.perform(post("/api/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(newPostJson))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.id", greaterThan(0)))
                    .andExpect(jsonPath("$.title").value(newPost.title()))
                    .andExpect(jsonPath("$.text").value(newPost.text()))
                    .andExpect(jsonPath("$.tags", hasSize(newPost.tags().size())))
                    .andExpect(jsonPath("$.tags", hasItems(newPost.tags().toArray())))
                    .andExpect(jsonPath("$.likesCount").value(expectedLikesCount))
                    .andExpect(jsonPath("$.commentsCount").value(expectedCommentsCount));
        }

        private static Stream<Arguments> provideAddPost() {
            String title = "Название n-ого поста";
            String text = "Контент n-ого поста";
            return Stream.of(
                    Arguments.of(new NewPostDto(title, text, List.of("пост_n")), 0L, 0L),
                    Arguments.of(new NewPostDto(title, text, List.of("пост_n", "заметка")), 0L, 0L)
            );
        }

        @ParameterizedTest
        @MethodSource("provideAddPostNotValid")
        void addPost_notValid(
                NewPostDto newNotValidPost,
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
            List<String> notValidMaxCountTags = IntStream.range(0, TAGS_MAX_COUNT + 1)
                    .mapToObj(i -> "\"tag\"")
                    .toList();

            return Stream.of(
                    Arguments.of(new NewPostDto(null, null, null), MSG_TAGS_NOT_NULL),
                    Arguments.of(new NewPostDto(null, null, notValidMinCountTags), MSG_TAGS_MIN_MAX_COUNT),
                    Arguments.of(new NewPostDto(null, null, notValidMaxCountTags), MSG_TAGS_MIN_MAX_COUNT)
            );
        }

        @Test
        void addPost_notValidMaxLength() throws Exception {
            List<String> tags = List.of("a".repeat(TAG_MAX_LENGTH + 1));
            NewPostDto newNotValidPost = new NewPostDto(
                    "a".repeat(TITLE_MAX_LENGTH + 1),
                    "a".repeat(TEXT_MAX_LENGTH + 1),
                    tags
            );
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
        @ParameterizedTest
        @MethodSource("provideGetPost")
        void getPost_success(
                Long postId,
                PostDto expectedPost
        ) throws Exception {
            mockMvc.perform(get("/api/posts/{postId}", postId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(postId))
                    .andExpect(jsonPath("$.title").value(expectedPost.title()))
                    .andExpect(jsonPath("$.text").value(expectedPost.text()))
                    .andExpect(jsonPath("$.tags", hasSize(expectedPost.tags().size())))
                    .andExpect(jsonPath("$.tags", hasItems(expectedPost.tags().toArray())))
                    .andExpect(jsonPath("$.likesCount").value(expectedPost.likesCount()))
                    .andExpect(jsonPath("$.commentsCount").value(expectedPost.commentsCount()));
        }

        private static Stream<Arguments> provideGetPost() {
            return Stream.of(
                    Arguments.of(1L, new PostDto(1L, "Название 1-ого поста", "Контент 1-ого поста", List.of("пост_1"), 0L, 10L)),
                    Arguments.of(2L, new PostDto(2L, "Название 2-ого поста", "Контент 2-ого поста", List.of("пост_2", "заметка"), 0L, 0L))
            );
        }

        @Test
        void getPost_postNotFound() throws Exception {
            mockMvc.perform(get("/api/posts/{postId}", POST_ID_NOT_FOUND))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value(MSG_POST_NOT_FOUND));
        }
    }

    @Nested
    class UpdatePost {
        @ParameterizedTest
        @MethodSource("provideUpdatePost")
        void updatePost_success(
                Long postId,
                UpdatePostDto updatePost,
                List<String> expectedUpdateTags
        ) throws Exception {
            String updatePostJson = objectMapper.writeValueAsString(updatePost);

            mockMvc.perform(put("/api/posts/{postId}", postId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updatePostJson))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(postId))
                    .andExpect(jsonPath("$.title").value(updatePost.title()))
                    .andExpect(jsonPath("$.text").value(updatePost.text()))
                    .andExpect(jsonPath("$.tags", hasSize(expectedUpdateTags.size())))
                    .andExpect(jsonPath("$.tags", hasItems(expectedUpdateTags.toArray())));
        }

        private static Stream<Arguments> provideUpdatePost() {
            return Stream.of(
                    // обновляем только название и контент 1-ого поста с передачей пустого списка тегов [], что означает без изменений тегов
                    Arguments.of(1L, new UpdatePostDto(1L, "Новое название", "Новый контент", List.of()), List.of("пост_1")),

                    // обновляем все поля: название, контент и тег 1-ого поста
                    Arguments.of(1L, new UpdatePostDto(1L, "Новое название", "Новый контент", List.of("новый_тег")), List.of("новый_тег")),
                    // обновляем только тег 1-ого поста
                    Arguments.of(1L, new UpdatePostDto(1L, "Название 1-ого поста", "Контент 1-ого поста", List.of("новый_тег")), List.of("новый_тег")),

                    // если обновить 2-ой пост, но ничего не изменяя
                    Arguments.of(2L, new UpdatePostDto(2L, "Название 2-ого поста", "Контент 2-ого поста", List.of("пост_2", "заметка")), List.of("пост_2", "заметка")),
                    // обновляем только теги 2-ого поста, удалив 1 тег
                    Arguments.of(2L, new UpdatePostDto(2L, "Название 2-ого поста", "Контент 2-ого поста", List.of("пост_2")), List.of("пост_2")),
                    // обновляем только теги 2-ого поста, добавив 1 тег
                    Arguments.of(2L, new UpdatePostDto(2L, "Название 2-ого поста", "Контент 2-ого поста", List.of("пост_2", "заметка", "новый_тег")), List.of("пост_2", "заметка", "новый_тег")),

                    // обновляем только теги 2-ого поста, удалив 1 тег и добавив 1 тег
                    Arguments.of(2L, new UpdatePostDto(2L, "Название 2-ого поста", "Контент 2-ого поста", List.of("пост_2", "новый_тег")), List.of("пост_2", "новый_тег"))
            );
        }

        @Test
        void updatePost_postNotFound() throws Exception {
            UpdatePostDto updatePost = new UpdatePostDto(POST_ID_NOT_FOUND,"Новое название", "Новый контент", List.of("новый_тег"));
            String updatePostJson = objectMapper.writeValueAsString(updatePost);

            mockMvc.perform(put("/api/posts/{postId}", POST_ID_NOT_FOUND)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updatePostJson))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value(MSG_POST_NOT_FOUND));
        }

        @Test
        void updatePost_mismatchPostId_badRequest() throws Exception {
            Long postId = 1L;
            Long pathVariablePostId = 2L;

            UpdatePostDto updatePost = new UpdatePostDto(postId, "Новое название", "Новый контент", List.of("новый_тег"));
            String updatePostJson = objectMapper.writeValueAsString(updatePost);

            mockMvc.perform(put("/api/posts/{postId}", pathVariablePostId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updatePostJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value(containsString(MSG_IDENTIFIER_MISMATCH)));
        }

        @ParameterizedTest
        @MethodSource("provideUpdatePostNotValid")
        void updatePost_notValid(
                Long postId,
                UpdatePostDto updateNotValidPost,
                String expectedMessageTagsNotValid
        ) throws Exception {
            String updateNotValidPostJson = objectMapper.writeValueAsString(updateNotValidPost);

            mockMvc.perform(put("/api/posts/{postId}", postId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateNotValidPostJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(MSG_POST_ID_REQUIRED))
                    .andExpect(jsonPath("$.title").value(MSG_TITLE_REQUIRED))
                    .andExpect(jsonPath("$.text").value(MSG_TEXT_REQUIRED))
                    .andExpect(jsonPath("$.tags").value(expectedMessageTagsNotValid));
        }

        private static Stream<Arguments> provideUpdatePostNotValid() {
            List<String> notValidMaxCountTags = IntStream.range(0, TAGS_MAX_COUNT + 1)
                    .mapToObj(i -> "\"tag\"")
                    .toList();

            return Stream.of(
                    Arguments.of(1L, new UpdatePostDto(null, null,null, null), MSG_TAGS_NOT_NULL),
                    Arguments.of(1L, new UpdatePostDto(null, "", "", notValidMaxCountTags), MSG_TAGS_MIN_MAX_COUNT)
            );
        }

        @Test
        void updatePost_notValidMaxLength() throws Exception {
            Long postId = 1L;
            List<String> tags = List.of("a".repeat(TAG_MAX_LENGTH + 1));
            UpdatePostDto updateNotValidPost = new UpdatePostDto(
                    postId,
                    "a".repeat(TITLE_MAX_LENGTH + 1),
                    "a".repeat(TEXT_MAX_LENGTH + 1),
                    tags
            );

            String updateNotValidPostJson = objectMapper.writeValueAsString(updateNotValidPost);

            mockMvc.perform(put("/api/posts/{postId}", postId)
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
    class DeletePost {
        @Test
        void deletePost_success() throws Exception {
            Long postId = 2L;

            mockMvc.perform(delete("/api/posts/{postId}", postId))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/posts/{postId}", postId))
                    .andExpect(status().isNotFound());

            mockMvc.perform(get("/api/posts/{postId}/comments", postId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    class addLike {
        @Test
        void addLike_success() throws Exception {
            for (int i = 1; i < 10; i++) {
                mockMvc.perform(post("/api/posts/{postId}/likes", 1L))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(content().string(String.valueOf(i)));
            }
        }

        @Test
        void addLike_postNotFound() throws Exception {
            for (int i = 1; i < 10; i++) {
                mockMvc.perform(post("/api/posts/{postId}/likes", POST_ID_NOT_FOUND))
                        .andExpect(status().isNotFound())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.message").value(MSG_POST_NOT_FOUND));
            }
        }
    }

    @Nested
    class UpdateAndGetImage {
        @Test
        void updateAndGetImage_success() throws Exception {
            Long postId = 1L;
            MockMultipartFile image = new MockMultipartFile("image", "image.jpg", "image/jpeg", JPEG_IMAGE_STUB);

            mockMvc.perform(MockMvcRequestBuilders.multipart("/api/posts/{postId}/image", postId)
                            .file(image)
                            .with(request -> {
                                request.setMethod("PUT");
                                return request;
                            }))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/posts/{postId}/image", postId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                    .andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(content().bytes(JPEG_IMAGE_STUB));

        }

        @Test
        void updateImage_ImageUpdateFailed() throws Exception {
            Long postId = 1L;

            MockMultipartFile image = new MockMultipartFile("image", "image.jpg", "image/jpeg", JPEG_IMAGE_STUB);

            Mockito.when(postService.updateImage(postId, JPEG_IMAGE_STUB)).thenReturn(false);

            mockMvc.perform(multipart("/api/posts/{postId}/image", postId)
                            .file(image)
                            .with(request -> {
                                request.setMethod("PUT");
                                return request;
                            }))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value(MSG_IMAGE_UPDATE_FAILED));
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
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value(MSG_IMAGE_EMPTY));
        }

        @Test
        void updateImage_IOException_ImageReadFailed() throws Exception {
            MockMultipartFile image = new MockMultipartFile(
                    "image",
                    "image.jpg",
                    "image/jpeg",
                    JPEG_IMAGE_STUB
            ) {
                @Override
                public byte[] getBytes() throws IOException {
                    throw new IOException("Simulating an image reading error message");
                }
            };

            mockMvc.perform(multipart("/api/posts/{postId}/image", 1L)
                            .file(image)
                            .with(request -> {
                                request.setMethod("PUT");
                                return request;
                            }))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value(containsString(MSG_IMAGE_READ_FAILED)));
        }

        @Test
        void updateImage_postNotFound_404() throws Exception {
            MockMultipartFile image = new MockMultipartFile("image", "image.jpg", "image/jpeg", JPEG_IMAGE_STUB);

            mockMvc.perform(multipart("/api/posts/{postId}/image", POST_ID_NOT_FOUND)
                            .file(image)
                            .with(request -> {
                                request.setMethod("PUT");
                                return request;
                            }))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value(MSG_POST_NOT_FOUND));
        }

        @Test
        void getImage_postHasNoImage_404() throws Exception {
            mockMvc.perform(get("/api/posts/{postId}/image", 1L))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value(MSG_IMAGE_NOT_FOUND));
        }

        @Test
        void getImage_postNotFound_404() throws Exception {
            mockMvc.perform(get("/api/posts/{postId}/image", POST_ID_NOT_FOUND))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value(MSG_POST_NOT_FOUND));
        }
    }

    @Nested
    class AddComments {
        @Test
        void addComment_and_checkIncrementCommentsCount_success() throws Exception {
            Long postId = 2L;
            
            for (int i = 1; i < 10; i++) {
                NewCommentDto newComment = new NewCommentDto(COMMENT_TEXT + i, postId);
                String newCommentJson = objectMapper.writeValueAsString(newComment);

                mockMvc.perform(post("/api/posts/{postId}/comments", postId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(newCommentJson))
                        .andExpect(status().isCreated())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.id").exists())
                        .andExpect(jsonPath("$.text").value(newComment.text()))
                        .andExpect(jsonPath("$.postId").value(newComment.postId()));

                // проверяем, что количество комментариев увеличилось на 1
                mockMvc.perform(get("/api/posts/{postId}", postId))
                        .andExpect(jsonPath("$.commentsCount").value(i));
            }
        }

        @Test
        void addComment_postNotFound() throws Exception {
            NewCommentDto newComment = new NewCommentDto(COMMENT_TEXT, POST_ID_NOT_FOUND);
            String newCommentJson = objectMapper.writeValueAsString(newComment);

            mockMvc.perform(post("/api/posts/{postId}/comments", POST_ID_NOT_FOUND)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(newCommentJson))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value(MSG_POST_NOT_FOUND));
        }

        @Test
        void addComment_notValid() throws Exception {
            NewCommentDto newComment = new NewCommentDto("", null);
            String newCommentJson = objectMapper.writeValueAsString(newComment);

            mockMvc.perform(post("/api/posts/{postId}/comments", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(newCommentJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.text").value(MSG_COMMENT_REQUIRED))
                    .andExpect(jsonPath("$.postId").value(MSG_POST_ID_REQUIRED));
        }

        @Test
        void addComment_notValidMaxLength() throws Exception {
            Long postId = 1L;
            
            NewCommentDto newComment = new NewCommentDto("a".repeat(COMMENT_MAX_LENGTH + 1), postId);
            String newCommentJson = objectMapper.writeValueAsString(newComment);

            mockMvc.perform(post("/api/posts/{postId}/comments", postId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(newCommentJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.text").value(MSG_COMMENT_MAX_LENGTH));
        }

        @Test
        void addComment_mismatchPostId_badRequest() throws Exception {
            Long postId = 1L;
            Long pathVariablePostId = 2L;

            NewCommentDto newComment = new NewCommentDto(COMMENT_TEXT, postId);
            String newCommentJson = objectMapper.writeValueAsString(newComment);

            mockMvc.perform(post("/api/posts/{postId}/comments", pathVariablePostId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(newCommentJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value(containsString(MSG_IDENTIFIER_MISMATCH)));
        }
    }

    @Nested
    class getComments {
        @Test
        void getComment_success() throws Exception {
            Long postId = 1L;
            Long commentId = 1L;

            mockMvc.perform(get("/api/posts/{postId}/comments/{commentId}", postId, commentId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(commentId))
                    .andExpect(jsonPath("$.text").value(COMMENT_TEXT + commentId))
                    .andExpect(jsonPath("$.postId").value(postId));
        }

        @Test
        void getComment_postNotFound() throws Exception {
            mockMvc.perform(get("/api/posts/{postId}/comments/{commentId}", POST_ID_NOT_FOUND, 1L))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value(MSG_POST_NOT_FOUND));
        }

        @Test
        void getComment_commentNotFound() throws Exception {
            mockMvc.perform(get("/api/posts/{postId}/comments/{commentId}", 1L, COMMENT_ID_NOT_FOUND))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value(MSG_COMMENT_NOT_FOUND));
        }

        @Test
        void getComments_success() throws Exception {
            mockMvc.perform(get("/api/posts/{postId}/comments", 1L))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(COUNT_COMMENTS_POST_1)))
                    .andExpect(jsonPath("$[*].id", everyItem(isA(Number.class))))
                    .andExpect(jsonPath("$[*].text", everyItem(containsString(COMMENT_TEXT))))
                    .andExpect(jsonPath("$[*].postId", everyItem(is(1))));
        }

        @ParameterizedTest
        @ValueSource(longs = {2L, 999L})
        void getComments_returnEmpty(Long postId) throws Exception {
            mockMvc.perform(get("/api/posts/{postId}/comments", postId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", empty()));
        }

        @Test
        void getComments_typeMismatch_badRequest() throws Exception {
            mockMvc.perform(get("/api/posts/{postId}/comments", "undefined"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$[0].message").value(containsString(MSG_INVALID_PARAMETER_FORMAT)));
        }
    }

    @Nested
    class UpdateComment {
        @Test
        void updateComment_success() throws Exception {
            Long postId = 1L;
            Long commentId = 1L;
            
            UpdateCommentDto updateCommentDto = new UpdateCommentDto(commentId, NEW_COMMENT_TEXT, postId);
            String updateCommentJson = objectMapper.writeValueAsString(updateCommentDto);

            mockMvc.perform(put("/api/posts/{postId}/comments/{commentId}", postId, commentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateCommentJson))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(updateCommentDto.id()))
                    .andExpect(jsonPath("$.text").value(updateCommentDto.text()))
                    .andExpect(jsonPath("$.postId").value(updateCommentDto.postId()));
        }

        @Test
        void updateComment_postNotFound() throws Exception {
            Long commentId = 1L;

            UpdateCommentDto updateCommentDto = new UpdateCommentDto(commentId, NEW_COMMENT_TEXT, POST_ID_NOT_FOUND);
            String updateCommentJson = objectMapper.writeValueAsString(updateCommentDto);

            mockMvc.perform(put("/api/posts/{postId}/comments/{commentId}", POST_ID_NOT_FOUND, commentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateCommentJson))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value(MSG_POST_NOT_FOUND));
        }

        @Test
        void updateComment_commentNotFound() throws Exception {
            Long postId = 1L;

            UpdateCommentDto updateCommentDto = new UpdateCommentDto(COMMENT_ID_NOT_FOUND, NEW_COMMENT_TEXT, postId);
            String updateCommentJson = objectMapper.writeValueAsString(updateCommentDto);

            mockMvc.perform(put("/api/posts/{postId}/comments/{commentId}", postId, COMMENT_ID_NOT_FOUND)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateCommentJson))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value(MSG_COMMENT_NOT_FOUND));
        }

        @Test
        void updateComment_mismatchPostId_badRequest() throws Exception {
            Long commentId = 1L;

            Long postId = 1L;
            Long pathVariablePostId = 2L;

            UpdateCommentDto updateCommentDto = new UpdateCommentDto(commentId, NEW_COMMENT_TEXT, postId);
            String updateCommentJson = objectMapper.writeValueAsString(updateCommentDto);

            mockMvc.perform(put("/api/posts/{postId}/comments/{commentId}", pathVariablePostId, commentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateCommentJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value(containsString(MSG_IDENTIFIER_MISMATCH)));
        }

        @Test
        void updateComment_mismatchCommentId_badRequest() throws Exception {
            Long commentId = 1L;
            Long pathVariableCommentId = 2L;

            Long postId = 1L;

            UpdateCommentDto updateCommentDto = new UpdateCommentDto(commentId, NEW_COMMENT_TEXT, postId);
            String updateCommentJson = objectMapper.writeValueAsString(updateCommentDto);

            mockMvc.perform(put("/api/posts/{postId}/comments/{commentId}", postId, pathVariableCommentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateCommentJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value(containsString(MSG_IDENTIFIER_MISMATCH)));
        }
    }

    @Nested
    class DeleteComment {
        @Test
        void deleteComment_and_checkDecrementCommentsCount_success() throws Exception {
            Long postId = 1L;

            for (int commentId = 10; commentId >= 1; commentId--) {
                mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}", postId, commentId))
                        .andExpect(status().isOk());

                // проверяем, что количество комментариев уменьшилось на 1
                int expectedCommentsCount = commentId - 1;
                mockMvc.perform(get("/api/posts/{postId}", postId))
                        .andExpect(jsonPath("$.commentsCount").value(expectedCommentsCount));
            }
        }
    }
}