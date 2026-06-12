package ru.yandex.practicum.repository;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import ru.yandex.practicum.AbstractPostgresMvcTest;
import ru.yandex.practicum.domain.Post;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(JdbcNativePostRepository.class)
public class JdbcNativePostRepositoryTest extends AbstractPostgresMvcTest {

    @Autowired
    PostRepository postRepository;

    @Nested
    class SearchPosts {
        @BeforeAll
        static void setUp(ApplicationContext context) {
            // генерируем 19 постов
            setUpAddPosts(context, 19);
        }

        @ParameterizedTest
        @CsvSource({
                "10, 0, 10", // 1-ая страница с 10 постами
                "10, 10, 9"  // 2-ая страница с оставшимися 9 постами
        })
        void searchPagePosts_success(int limit, int offset, int expectedPostsSize) {
            List<Post> posts = postRepository.findPagePosts(limit, offset);
            assertEquals(expectedPostsSize, posts.size());
        }

        @ParameterizedTest
        @CsvSource({
                "публикац, 10, 0, 4",  // 1-ая страница с 4 постами с названием содержащим "публикац"
                "пост, 10, 0, 10",     // 1-ая страница с 10 постами с названием содержащим "пост"
                "пост, 10, 10, 5"      // 2-ая страница с оставшимися 5 постами с названием содержащим "пост"
        })
        void searchPagePostsByTitle_success(String title, int limit, int offset, int expectedPostsSize) {
            List<Post> posts = postRepository.findPagePostsByTitle(title, limit, offset);
            assertEquals(expectedPostsSize, posts.size());

            for (Post post : posts) {
                String titlePost = post.getTitle();
                assertTrue(titlePost.toLowerCase().contains(title.toLowerCase()));
            }
        }

        @ParameterizedTest
        @MethodSource("provideSearchPostsByTags")
        void searchPagePostsByTags_success(List<String> tags, int limit, int offset, int expectedPostsSize) {
            List<Post> posts = postRepository.findPagePostsByTags(tags, limit, offset);
            assertEquals(expectedPostsSize, posts.size());

            for (Post post : posts) {
                List<String> postTags = post.getTags();
                assertTrue(postTags.containsAll(tags));
            }
        }

        private static Stream<Arguments> provideSearchPostsByTags() {
            return Stream.of(
                    // 1-ая страница c 5-ю постами с тегом "заметка"
                    Arguments.of(new ArrayList<>(List.of("заметка")), 5, 0, 5),
                    // 2-ая страница с оставшимися 4-мя постами с тегом "заметка"
                    Arguments.of(new ArrayList<>(List.of("заметка")), 5, 5, 4),

                    // 1-ая страница с 5-ю постами с тегом "лонгрид"
                    Arguments.of(new ArrayList<>(List.of("лонгрид")), 5, 0, 5),
                    // 2-ая страница с оставшимся 1-м постом с тегом "лонгрид"
                    Arguments.of(new ArrayList<>(List.of("лонгрид")), 5, 5, 1),

                    // 1-ая страница с 3-мя постами с тегами "заметка" и "лонгрид"
                    Arguments.of(new ArrayList<>(List.of("заметка", "лонгрид")), 5, 0, 3),

                    // 1-ая страница с 1-м постом с тегом "пост_2"
                    Arguments.of(new ArrayList<>(List.of("пост_2")), 5, 0, 1)
            );
        }

        @ParameterizedTest
        @MethodSource("provideSearchPostsByTitleAndTags")
        void searchPagePostsByTitleAndTags_success(String title, List<String> tags, int limit, int offset, int expectedPostsSize) {
            List<Post> posts = postRepository.findPagePostsByTitleAndTags(title, tags, limit, offset);
            assertEquals(expectedPostsSize, posts.size());

            for (Post post : posts) {
                String titlePost = post.getTitle();
                assertTrue(titlePost.toLowerCase().contains(title.toLowerCase()));

                List<String> postTags = post.getTags();
                assertTrue(postTags.containsAll(tags));
            }
        }

        private static Stream<Arguments> provideSearchPostsByTitleAndTags() {
            return Stream.of(
                    // 1-ая страница c 5-ю постами с названием содержащим "назван" и тегом "заметка"
                    Arguments.of("назван", new ArrayList<>(List.of("заметка")), 5, 0, 5),
                    // 2-ая страница с оставшимися 4-мя постами с названием содержащим "назван" и тегом "заметка"
                    Arguments.of("назван", new ArrayList<>(List.of("заметка")), 5, 5, 4),

                    // 1-ая страница c 5-ю постами с названием содержащим "назван" и тегом "лонгрид"
                    Arguments.of("назван", new ArrayList<>(List.of("лонгрид")), 5, 0, 5),
                    // 2-ая страница с оставшимся 1-м постом с названием содержащим "назван" и тегом "лонгрид"
                    Arguments.of("назван", new ArrayList<>(List.of("лонгрид")), 5, 5, 1),

                    // 1-ая страница с 3-мя постами c названием содержащим "назван" и тегами "заметка" и "лонгрид"
                    Arguments.of("назван", new ArrayList<>(List.of("заметка", "лонгрид")), 5, 0, 3),

                    // 1-ая страница с 4-мя постами c названием содержащим "публикац" и тегом "заметка"
                    Arguments.of("публикац", new ArrayList<>(List.of("заметка")), 5, 0, 4),

                    // 1-ая страница с 1-м постом с названием содержащим "публикац" и тегом "лонгрид"
                    Arguments.of("публикац", new ArrayList<>(List.of("лонгрид")), 5, 0, 1),

                    // 1-ая страница с 1-м постом с названием содержащим "публикац" и тегами "заметка" и "лонгрид"
                    Arguments.of("публикац", new ArrayList<>(List.of("заметка", "лонгрид")), 5, 0, 1),

                    // 1-ая страница с 1-м постом с названием содержащим "пост" и тегом "пост_2"
                    Arguments.of("пост", new ArrayList<>(List.of("пост_2")), 5, 0, 1)

            );
        }

        @ParameterizedTest
        @ValueSource(ints = 19)
            // Всего 19 постов в БД
        void countPosts_success(int expectedPostsSize) {
            Long countPosts = postRepository.countPosts();
            assertEquals(expectedPostsSize, countPosts);
        }

        @ParameterizedTest
        @CsvSource({
                "назван, 19",   // 19 постов с названием содержащим "назван"
                "публикац, 4",  // 4 поста с названием содержащим "публикац"
                "пост, 15"      // 15 постов с названием содержащим "пост"
        })
        void countPostsByTitle_success(String title, int expectedPostsSize) {
            Long countPosts = postRepository.countPostsByTitle(title);
            assertEquals(expectedPostsSize, countPosts);
        }

        @ParameterizedTest
        @MethodSource("provideCountPostsByTags")
        void countPostsByTags_success(List<String> tags, int expectedPostsSize) {
            Long countPosts = postRepository.countPostsByTags(tags);
            assertEquals(expectedPostsSize, countPosts);
        }

        private static Stream<Arguments> provideCountPostsByTags() {
            return Stream.of(
                    // 9 постов с тегом "заметка"
                    Arguments.of(new ArrayList<>(List.of("заметка")), 9),
                    // аналогично для других тегов
                    Arguments.of(new ArrayList<>(List.of("лонгрид")), 6),
                    Arguments.of(new ArrayList<>(List.of("заметка", "лонгрид")), 3),
                    Arguments.of(new ArrayList<>(List.of("пост_2")), 1)
            );
        }

        @ParameterizedTest
        @MethodSource("provideCountPostsByTitleAndTags")
        void countPostsByTitleAndTags_success(String title, List<String> tags, int expectedPostsSize) {
            Long countPosts = postRepository.countPostsByTitleAndTags(title, tags);
            assertEquals(expectedPostsSize, countPosts);
        }

        private static Stream<Arguments> provideCountPostsByTitleAndTags() {
            return Stream.of(
                    // 9 постов с названием содержащим "назван" и тегом "заметка"
                    Arguments.of("назван", new ArrayList<>(List.of("заметка")), 9),
                    // 6 постов с названием содержащим "назван" и тегом "лонгрид"
                    Arguments.of("назван", new ArrayList<>(List.of("лонгрид")), 6),
                    // 3 поста с названием содержащим "назван" и тегами "заметка" и "лонгрид"
                    Arguments.of("назван", new ArrayList<>(List.of("заметка", "лонгрид")), 3),

                    // по аналогии для других комбинаций
                    Arguments.of("публикац", new ArrayList<>(List.of("заметка")), 4),
                    Arguments.of("публикац", new ArrayList<>(List.of("лонгрид")), 1),
                    Arguments.of("публикац", new ArrayList<>(List.of("заметка", "лонгрид")), 1),

                    Arguments.of("пост", new ArrayList<>(List.of("заметка")), 5),
                    Arguments.of("пост", new ArrayList<>(List.of("лонгрид")), 5),
                    Arguments.of("пост", new ArrayList<>(List.of("заметка", "лонгрид")), 2),
                    Arguments.of("пост", new ArrayList<>(List.of("пост_2")), 1)
            );
        }
    }

    @Nested
    class AddPost {
        @Test
        void addPost_success() {
            List<String> tags = List.of("пост_n", "пост_n-ый");
            Post newPost = new Post("Название n-ого поста", "Контент n-ого поста", tags);

            Long postId = postRepository.save(newPost);
            assertNotNull(postId);

            Optional<Post> postOptional = postRepository.findById(postId);
            assertTrue(postOptional.isPresent());

            Post post = postOptional.get();
            assertEquals(postId, post.getId());
            assertEquals(newPost.getTitle(), post.getTitle());
            assertEquals(newPost.getText(), post.getText());
            assertEquals(newPost.getLikesCount(), post.getLikesCount());
            assertEquals(newPost.getCommentsCount(), post.getCommentsCount());
            assertEquals(tags.size(), post.getTags().size());
            assertTrue(post.getTags().containsAll(tags));
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
        void getPost_success() {
            Optional<Post> postOptional = postRepository.findById(1L);
            assertTrue(postOptional.isPresent());

            Post post = postOptional.get();
            assertEquals(1L, post.getId());
            assertEquals("Название 1-ого поста", post.getTitle());
            assertEquals("Контент 1-ого поста", post.getText());
            assertEquals(1, post.getTags().size());
            assertTrue(post.getTags().contains("пост_1"));
            assertEquals(0, post.getLikesCount());
            assertEquals(0, post.getCommentsCount());
        }

        @Test
        void getPost_returnEmpty_whenNotExists() {
            Optional<Post> postOptional = postRepository.findById(999L);
            assertTrue(postOptional.isEmpty());
        }

        @Test
        void existsById_true_and_false() {
            assertTrue(postRepository.existsById(1L));
            assertFalse(postRepository.existsById(999L));
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
        void updateAndGetImage_success() {
            byte[] newImage = new byte[]{1, 2, 3, 4};
            assertTrue(postRepository.updateImage(1L, newImage));

            Optional<byte[]> imageFromDbOptional = postRepository.findImageById(1L);
            assertTrue(imageFromDbOptional.isPresent());
            byte[] imageFromDb = imageFromDbOptional.get();
            assertArrayEquals(newImage, imageFromDb);
        }

        @Test
        void getImage_returnEmpty_whenNotSet() {
            Optional<byte[]> postOptional = postRepository.findImageById(1L);
            assertTrue(postOptional.isEmpty());
        }
    }

}