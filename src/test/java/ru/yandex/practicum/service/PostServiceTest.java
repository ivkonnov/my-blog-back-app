package ru.yandex.practicum.service;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.practicum.AbstractPostgresMvcTest;
import ru.yandex.practicum.dto.*;
import ru.yandex.practicum.exception.CommentCountUpdateException;
import ru.yandex.practicum.exception.ImageNotFoundException;
import ru.yandex.practicum.exception.PostNotFoundException;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static ru.yandex.practicum.util.PostPreviewDisplay.*;

public class PostServiceTest extends AbstractPostgresMvcTest {

    @Autowired
    private PostService postService;

    @Autowired
    private CommentService commentService;

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
        ) {
            // регулярное выражение для проверки длины превью поста
            String regexExpectedTextPreviewMaxLength = "^.{0," + (POST_TEXT_PREVIEW_MAX_LENGTH + ELLIPSIS.length()) + "}$";

            PagePostsDto pagePostsDto = postService.findPagePostsBySearch(search, pageNumber, pageSize);
            assertThat(pagePostsDto.posts()).hasSize(expectedPostsSize);
            assertThat(pagePostsDto.posts()).allSatisfy(post -> assertThat(post.title()).contains(expectedPartOfTitleFromQuery));
            assertThat(pagePostsDto.posts()).allSatisfy(post -> assertThat(post.text()).matches(regexExpectedTextPreviewMaxLength));
            assertThat(pagePostsDto.posts()).allSatisfy(post -> assertThat(post.tags()).containsAll(expectedTagsFromQuery));

            assertThat(pagePostsDto.hasPrev()).isEqualTo(pageNumber > 1);
            assertThat(pagePostsDto.hasNext()).isEqualTo(pageNumber < expectedLastPage);
            assertThat(pagePostsDto.lastPage()).isEqualTo(expectedLastPage);
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
        @Test
        void addPost_success() {
            NewPostDto newPostDto = new NewPostDto("Название n-ого поста", "Контент n-ого поста", List.of("тег1", "тег2"));
            PostDto postDto = postService.addPost(newPostDto);
            assertNotNull(postDto);
            assertTrue(postDto.id() > 0);
            assertEquals(newPostDto.title(), postDto.title());
            assertEquals(newPostDto.text(), postDto.text());
            assertEquals(newPostDto.tags(), postDto.tags());
        }
    }

    @Nested
    class GetPost {
        @Test
        void getPost_success() {
            Long postId = 2L;
            PostDto postDto = postService.getPost(postId);
            assertNotNull(postDto);
            assertEquals(postId, postDto.id());
            assertEquals("Название 2-ого поста", postDto.title());
            assertEquals("Контент 2-ого поста", postDto.text());
            assertEquals(List.of("пост_2", "заметка"), postDto.tags());
        }

        @Test
        void getPost_postNotFound() {
            Long postId = POST_ID_NOT_FOUND;
            PostNotFoundException ex = assertThrows(
                    PostNotFoundException.class,
                    () -> postService.getPost(postId)
            );
            assertEquals(postId, ex.getPostId());
        }

        @Test
        void existsById_true_and_false() {
            assertTrue(postService.existsById(1L));
            assertFalse(postService.existsById(POST_ID_NOT_FOUND));
        }
    }

    @Nested
    class UpdatePost {
        @Test
        void updatePost_success() {
            Long postId = 2L;
            UpdatePostDto updatePostDto = new UpdatePostDto(postId, "Новое название", "Новый контент", List.of("новый_тег"));
            PostDto postDto = postService.updatePost(postId, updatePostDto);
            assertNotNull(postDto);
            assertEquals(postId, postDto.id());
            assertEquals(updatePostDto.title(), postDto.title());
            assertEquals(updatePostDto.text(), postDto.text());
            assertEquals(updatePostDto.tags(), postDto.tags());
        }

        @Test
        void updatePost_postNotFound() {
            Long postId = POST_ID_NOT_FOUND;
            UpdatePostDto updatePostDto = new UpdatePostDto(postId, "Новое название", "Новый контент", List.of("новый_тег"));
            PostNotFoundException ex = assertThrows(
                    PostNotFoundException.class,
                    () -> postService.updatePost(postId, updatePostDto)
            );
            assertEquals(postId, ex.getPostId());
        }
    }

    @Nested
    class DeletePost {
        @Test
        void deletePost_success() {
            Long postId = 2L;
            postService.deletePost(postId);

            PostNotFoundException ex = assertThrows(
                    PostNotFoundException.class,
                    () -> postService.getPost(postId)
            );
            assertEquals(postId, ex.getPostId());

            List<CommentDto> comments = commentService.getComments(postId);
            assertTrue(comments.isEmpty());
        }

        @Test
        void deletePost_postNotFound() {
            Long postId = POST_ID_NOT_FOUND;
            PostNotFoundException ex = assertThrows(
                    PostNotFoundException.class,
                    () -> postService.deletePost(postId)
            );
            assertEquals(postId, ex.getPostId());
        }
    }

    @Nested
    class AddLike {
        @Test
        void addLike_success() {
            for (int i = 1; i < 10; i++) {
                Long likes = postService.addLike(1L);
                assertEquals(i, likes);
            }
        }

        @Test
        void addLike_postNotFound() {
            Long postId = POST_ID_NOT_FOUND;
            PostNotFoundException ex = assertThrows(
                    PostNotFoundException.class,
                    () -> postService.addLike(postId)
            );
            assertEquals(postId, ex.getPostId());
        }

    }

    @Nested
    class UpdateAndGetImage {
        @Test
        void updateAndGetImage_success() {
            Long postId = 1L;
            byte[] jpegStub = new byte[]{(byte) 137, 80, 78, 71};
            boolean isUpdated = postService.updateImage(postId, jpegStub);
            assertTrue(isUpdated);

            byte[] image = postService.getImage(postId);
            assertArrayEquals(jpegStub, image);
        }

        @Test
        void updateImage_postNotFound() {
            Long postId = POST_ID_NOT_FOUND;
            PostNotFoundException ex = assertThrows(
                    PostNotFoundException.class,
                    () -> postService.updateImage(postId, new byte[]{})
            );
            assertEquals(postId, ex.getPostId());
        }

        @Test
        void getImage_postNotFound() {
            Long postId = POST_ID_NOT_FOUND;
            PostNotFoundException ex = assertThrows(
                    PostNotFoundException.class,
                    () -> postService.getImage(postId)
            );
            assertEquals(postId, ex.getPostId());
        }

        @Test
        void getImage_imageNotFound() {
            Long postId = 2L;
            ImageNotFoundException ex = assertThrows(
                    ImageNotFoundException.class,
                    () -> postService.getImage(postId)
            );
            assertEquals(postId, ex.getPostId());
        }
    }

    @Nested
    class CommentCounter {
        @Test
        void incrementCommentsCount() {
            Long postId = 2L;

            for (int i = 1; i < 10; i++) {
                postService.incrementCommentsCount(postId);
                assertEquals(i, postService.getPost(postId).commentsCount());
            }
        }

        @Test
        void incrementCommentsCount_commentCountUpdateException() {
            Long postId = POST_ID_NOT_FOUND;
            CommentCountUpdateException ex = assertThrows(
                    CommentCountUpdateException.class,
                    () -> postService.incrementCommentsCount(postId)
            );
            assertEquals(postId, ex.getPostId());
        }

        @Test
        void decrementCommentsCount() {
            Long postId = 2L;

            for (int i = 1; i <= 10; i++)
                postService.incrementCommentsCount(postId);

            for (int i = 9; i >= 0; i--) {
                postService.decrementCommentsCount(postId);
                assertEquals(i, postService.getPost(postId).commentsCount());
            }
        }

        @Test
        void decrementCommentsCount_commentCountUpdateException() {
            Long postId = POST_ID_NOT_FOUND;
            CommentCountUpdateException ex = assertThrows(
                    CommentCountUpdateException.class,
                    () -> postService.decrementCommentsCount(postId)
            );
            assertEquals(postId, ex.getPostId());
        }
    }

}
