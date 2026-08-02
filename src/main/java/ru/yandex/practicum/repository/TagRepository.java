package ru.yandex.practicum.repository;

import ru.yandex.practicum.domain.Tag;

import java.util.List;

public interface TagRepository {

    List<Long> saveAll(List<String> names);

    List<Tag> findAllById(List<Long> tagIds);

    List<Tag> findAllByPostId(Long postId);

    List<Long> cleanUnusedTags();

}
