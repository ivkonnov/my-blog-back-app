package ru.yandex.practicum.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.practicum.AbstractPostgresMvcTest;
import ru.yandex.practicum.domain.Tag;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class TagRepositoryTest extends AbstractPostgresMvcTest {

    @Autowired
    TagRepository tagRepository;

    @Test
    void saveTags_success() {
        List<String> tagNames = List.of("новый_тег_1", "новый_тег_первый");
        List<Long> tagIds = tagRepository.saveAll(tagNames);
        // проверяем, что количество тегов равно количеству добавленных тегов
        assertEquals(tagNames.size(), tagIds.size());

        List<Tag> savedTags = tagRepository.findAllById(tagIds);
        List<String> savedTagNames = savedTags.stream().map(Tag::getName).toList();

        // проверяем, что теги сохранены и найдены по id
        assertThat(savedTagNames).containsExactlyInAnyOrderElementsOf(tagNames);
    }

    @Test
    void findTagsByPostId_success() {
        Long postId = 2L;
        List<String> expectedTagNames = List.of("пост_2", "заметка");

        List<Tag> actualTags = tagRepository.findAllByPostId(postId);
        List<String> actualTagNames = actualTags.stream().map(Tag::getName).toList();

        // проверяем, что теги найдены по id поста
        assertThat(actualTagNames).containsExactlyInAnyOrderElementsOf(expectedTagNames);
    }

    @Test
    void cleanUnusedTags_success() {
        List<String> unUsedTagNames = List.of("непривязанный_тег_1", "непривязанный_тег_2");
        // добавляем теги без привязки к посту
        List<Long> unUsedTagIds = tagRepository.saveAll(unUsedTagNames);

        // удаляем неиспользуемые теги (непривязанные к постам)
        List<Long> deletedUnusedTagIds = tagRepository.cleanUnusedTags();
        // проверяем, что количество удаленных тегов равно количеству добавленных ранее неиспользуемых тегов
        assertEquals(unUsedTagIds.size(), deletedUnusedTagIds.size());
        // проверяем, что удаленные теги совпадают с неиспользуемыми тегами
        assertThat(deletedUnusedTagIds).containsExactlyInAnyOrderElementsOf(unUsedTagIds);

        List<Tag> unUsedTags = tagRepository.findAllById(unUsedTagIds);
        // проверяем, что неиспользуемые теги удалены
        assertTrue(unUsedTags.isEmpty());
    }

}
