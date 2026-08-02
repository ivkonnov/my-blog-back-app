package ru.yandex.practicum.repository;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.practicum.AbstractPostgresMvcTest;
import ru.yandex.practicum.domain.Post;
import ru.yandex.practicum.domain.Tag;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class PostRepositoryTest extends AbstractPostgresMvcTest {

    @Autowired
    PostRepository postRepository;

    @Autowired
    TagRepository tagRepository;

    @Nested
    class FindPagePostsBySearch {
        @ParameterizedTest
        @CsvSource({
                "10, 0, 10", // 1-ая страница с 10 постами
                "10, 10, 9"  // 2-ая страница с оставшимися 9 постами
        })
        void searchPagePosts_success(
                int limit,
                int offset,
                int expectedPostsSize
        ) {
            List<Post> posts = postRepository.findPagePosts(limit, offset);
            assertEquals(expectedPostsSize, posts.size());
        }

        @ParameterizedTest
        @CsvSource({
                "публикац, 10, 0, 4",      // 1-ая страница с 4 постами с названием содержащим "публикац"
                "пост, 10, 0, 10",         // 1-ая страница с 10 постами с названием содержащим "пост"
                "пост, 10, 10, 5"          // 2-ая страница с оставшимися 5 постами с названием содержащим "пост"
        })
        void searchPagePostsByTitle_success(
                String title,
                int limit,
                int offset,
                int expectedPostsSize
        ) {
            List<Post> posts = postRepository.findPagePostsByTitle(title, limit, offset);
            assertEquals(expectedPostsSize, posts.size());

            for (Post post : posts) {
                String titlePost = post.getTitle();
                assertTrue(titlePost.toLowerCase().contains(title.toLowerCase()));
            }
        }

        @ParameterizedTest
        @MethodSource("provideSearchPostsByTags")
        void searchPagePostsByTags_success(
                List<String> tags,
                int limit,
                int offset,
                int expectedPostsSize
        ) {
            List<Post> posts = postRepository.findPagePostsByTags(tags, limit, offset);
            assertEquals(expectedPostsSize, posts.size());

            for (Post post : posts) {
                List<String> postTags = post.getTags();
                assertTrue(postTags.containsAll(tags));
            }
        }

        private static Stream<Arguments> provideSearchPostsByTags() {
            return Stream.of(
                    // 1-ая страница с 5-ю постами с тегом "заметка"
                    Arguments.of(List.of("заметка"), 5, 0, 5),
                    // 2-ая страница с оставшимися 4-мя постами с тегом "заметка"
                    Arguments.of(List.of("заметка"), 5, 5, 4),

                    // 1-ая страница с 5-ю постами с тегом "лонгрид"
                    Arguments.of(List.of("лонгрид"), 5, 0, 5),
                    // 2-ая страница с оставшимся 1-м постом с тегом "лонгрид"
                    Arguments.of(List.of("лонгрид"), 5, 5, 1),

                    // 1-ая страница с 3-мя постами с тегами "заметка" и "лонгрид"
                    Arguments.of(List.of("заметка", "лонгрид"), 5, 0, 3),

                    // 1-ая страница с 1-м постом с тегом "пост_2"
                    Arguments.of(List.of("пост_2"), 5, 0, 1)
            );
        }

        @ParameterizedTest
        @MethodSource("provideSearchPostsByTitleAndTags")
        void searchPagePostsByTitleAndTags_success(
                String title,
                List<String> tags,
                int limit,
                int offset,
                int expectedPostsSize
        ) {
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
                    // 1-ая страница с 5-ю постами с названием содержащим "назван" и тегом "заметка"
                    Arguments.of("назван", List.of("заметка"), 5, 0, 5),
                    // 2-ая страница с оставшимися 4-мя постами с названием содержащим "назван" и тегом "заметка"
                    Arguments.of("назван", List.of("заметка"), 5, 5, 4),

                    // 1-ая страница с 5-ю постами с названием содержащим "назван" и тегом "лонгрид"
                    Arguments.of("назван", List.of("лонгрид"), 5, 0, 5),
                    // 2-ая страница с оставшимся 1-м постом с названием содержащим "назван" и тегом "лонгрид"
                    Arguments.of("назван", List.of("лонгрид"), 5, 5, 1),

                    // 1-ая страница с 3-мя постами с названием содержащим "назван" и тегами "заметка" и "лонгрид"
                    Arguments.of("назван", List.of("заметка", "лонгрид"), 5, 0, 3),

                    // 1-ая страница с 4-мя постами с названием содержащим "публикац" и тегом "заметка"
                    Arguments.of("публикац", List.of("заметка"), 5, 0, 4),

                    // 1-ая страница с 1-м постом с названием содержащим "публикац" и тегом "лонгрид"
                    Arguments.of("публикац", List.of("лонгрид"), 5, 0, 1),

                    // 1-ая страница с 1-м постом с названием содержащим "публикац" и тегами "заметка" и "лонгрид"
                    Arguments.of("публикац", List.of("заметка", "лонгрид"), 5, 0, 1),

                    // 1-ая страница с 1-м постом с названием содержащим "пост" и тегом "пост_2"
                    Arguments.of("пост", List.of("пост_2"), 5, 0, 1)

            );
        }

        @ParameterizedTest
        @ValueSource(ints = 19)  // Всего 19 постов в БД
        void countPosts_success(int expectedPostsSize) {
            Long countPosts = postRepository.countPosts();
            assertEquals(expectedPostsSize, countPosts);
        }

        @ParameterizedTest
        @CsvSource({
                "назван, 19",      // 19 постов с названием содержащим "назван"
                "публикац, 4",     // 4 поста с названием содержащим "публикац"
                "пост, 15",        // 15 постов с названием содержащим "пост"
        })
        void countPostsByTitle_success(String title, int expectedPostsSize) {
            Long countPosts = postRepository.countPostsByTitle(title);
            assertEquals(expectedPostsSize, countPosts);
        }

        @ParameterizedTest
        @MethodSource("provideCountPostsByTags")
        void countPostsByTags_success(
                List<String> tags,
                int expectedPostsSize
        ) {
            Long countPosts = postRepository.countPostsByTags(tags);
            assertEquals(expectedPostsSize, countPosts);
        }

        private static Stream<Arguments> provideCountPostsByTags() {
            return Stream.of(
                    // 9 постов с тегом "заметка"
                    Arguments.of(List.of("заметка"), 9),
                    // аналогично для других тегов
                    Arguments.of(List.of("лонгрид"), 6),
                    Arguments.of(List.of("заметка", "лонгрид"), 3),
                    Arguments.of(List.of("пост_2"), 1)
            );
        }

        @ParameterizedTest
        @MethodSource("provideCountPostsByTitleAndTags")
        void countPostsByTitleAndTags_success(
                String title,
                List<String> tags,
                int expectedPostsSize
        ) {
            Long countPosts = postRepository.countPostsByTitleAndTags(title, tags);
            assertEquals(expectedPostsSize, countPosts);
        }

        private static Stream<Arguments> provideCountPostsByTitleAndTags() {
            return Stream.of(
                    // 9 постов с названием содержащим "назван" и тегом "заметка"
                    Arguments.of("назван", List.of("заметка"), 9),
                    // 6 постов с названием содержащим "назван" и тегом "лонгрид"
                    Arguments.of("назван", List.of("лонгрид"), 6),
                    // 3 поста с названием содержащим "назван" и тегами "заметка" и "лонгрид"
                    Arguments.of("назван", List.of("заметка", "лонгрид"), 3),

                    // по аналогии для других комбинаций
                    Arguments.of("публикац", List.of("заметка"), 4),
                    Arguments.of("публикац", List.of("лонгрид"), 1),
                    Arguments.of("публикац", List.of("заметка", "лонгрид"), 1),

                    Arguments.of("пост", List.of("заметка"), 5),
                    Arguments.of("пост", List.of("лонгрид"), 5),
                    Arguments.of("пост", List.of("заметка", "лонгрид"), 2),
                    Arguments.of("пост", List.of("пост_2"), 1)
            );
        }
    }

    @Nested
    class AddPost {
        @ParameterizedTest
        @MethodSource("provideAddPost")
        void addPost_success(Post newPost) {
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
        }

        private static Stream<Arguments> provideAddPost() {
            return Stream.of(
                    Arguments.of(Post.builder().title("Название n-ого поста").text("Контент n-ого поста").build())
            );
        }
    }

    @Nested
    class LinkPostTags {
        @Test
        void clearPostTags_success() {
            Long postId = 1L;
            List<Tag> tags = tagRepository.findAllByPostId(postId);
            assertFalse(tags.isEmpty());

            postRepository.clearPostTags(postId);
            List<Tag> tagsEmpty = tagRepository.findAllByPostId(postId);
            assertTrue(tagsEmpty.isEmpty());
        }

        @Test
        void linkPostTags_success() {
            Long postId = 2L;
            List<Tag> tags = tagRepository.findAllByPostId(postId);
            assertFalse(tags.isEmpty());
            List<Long> expectedTagIds = tags.stream().map(Tag::getId).toList();

            postRepository.clearPostTags(postId);
            List<Tag> tagsEmpty = tagRepository.findAllByPostId(postId);
            assertTrue(tagsEmpty.isEmpty());

            postRepository.linkPostTags(postId, expectedTagIds);
            List<Tag> newTags = tagRepository.findAllByPostId(postId);
            List<Long> newTagIds = newTags.stream().map(Tag::getId).toList();
            assertEquals(expectedTagIds, newTagIds);
        }
    }

    @Nested
    class GetPost {
        @ParameterizedTest
        @MethodSource("provideGetPost")
        void getPost_success(
                Long postId,
                Post expectedPost
        ) {
            Optional<Post> postOptional = postRepository.findById(postId);
            assertTrue(postOptional.isPresent());

            Post post = postOptional.get();

            assertEquals(postId, post.getId());
            assertEquals(expectedPost.getTitle(), post.getTitle());
            assertEquals(expectedPost.getText(), post.getText());
            assertEquals(expectedPost.getLikesCount(), post.getLikesCount());
            assertEquals(expectedPost.getCommentsCount(), post.getCommentsCount());
        }

        private static Stream<Arguments> provideGetPost() {
            return Stream.of(
                    Arguments.of(1L, Post.builder().id(1L).title("Название 1-ого поста").text("Контент 1-ого поста").likesCount(0L).commentsCount(10L).build())
            );
        }

        @Test
        void getPost_returnEmpty_whenPostNotExists() {
            Optional<Post> postOptional = postRepository.findById(POST_ID_NOT_FOUND);
            assertTrue(postOptional.isEmpty());
        }

        @Test
        void existsById_true_and_false() {
            assertTrue(postRepository.existsById(1L));
            assertFalse(postRepository.existsById(POST_ID_NOT_FOUND));
        }
    }

    @Nested
    class UpdatePost {
        @ParameterizedTest
        @MethodSource("provideUpdatePost")
        void update_success(Long postId, Post updatedPost) {
            postRepository.update(postId, updatedPost);

            Optional<Post> postOptional = postRepository.findById(postId);
            assertTrue(postOptional.isPresent());

            Post updatedPostFromDb = postOptional.get();
            assertEquals(updatedPost.getId(), updatedPostFromDb.getId());
            assertEquals(updatedPost.getTitle(), updatedPostFromDb.getTitle());
            assertEquals(updatedPost.getText(), updatedPostFromDb.getText());
        }

        private static Stream<Arguments> provideUpdatePost() {
            return Stream.of(
                    // обновляем название и контент 1-ого поста
                    Arguments.of(1L, Post.builder().id(1L).title("Новое название").text("Новый контент").build())
            );
        }
    }

    @Nested
    class DeletePost {
        @Test
        void delete_success() {
            Long postId = 1L;
            postRepository.deleteById(postId);
            assertFalse(postRepository.existsById(postId));
        }
    }

    @Nested
    class Likes {
        @Test
        void addLike() {
            for (int i = 1; i <= 10; i++) {
                Long likes = postRepository.addLike(1L);
                assertEquals(i, likes);
            }
        }
    }

    @Nested
    class UpdateAndGetImage {
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

    @Nested
    class CommentCounter {
        @Test
        void incrementCommentsCount() {
            Long postId = 2L;

            for (int i = 1; i <= 10; i++) {
                boolean isIncrement = postRepository.incrementCommentsCount(postId);
                assertTrue(isIncrement);

                Optional<Post> postOptional = postRepository.findById(postId);
                assertTrue(postOptional.isPresent());

                Post post = postOptional.get();
                assertEquals(i, post.getCommentsCount());
            }
        }

        @Test
        void decrementCommentsCount() {
            Long postId = 2L;

            for (int i = 1; i <= 10; i++)
                postRepository.incrementCommentsCount(postId);

            for (int i = 9; i >= 0; i--) {
                boolean isDecrement = postRepository.decrementCommentsCount(postId);
                assertTrue(isDecrement);

                Optional<Post> postOptional = postRepository.findById(postId);
                assertTrue(postOptional.isPresent());

                Post post = postOptional.get();
                assertEquals(i, post.getCommentsCount());
            }
        }
    }
    
}