package ru.yandex.practicum.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.domain.Post;
import ru.yandex.practicum.domain.Tag;
import ru.yandex.practicum.dto.*;
import ru.yandex.practicum.exception.CommentCountUpdateException;
import ru.yandex.practicum.exception.ImageNotFoundException;
import ru.yandex.practicum.exception.PostNotFoundException;
import ru.yandex.practicum.mapper.PostMapper;
import ru.yandex.practicum.repository.PostRepository;
import ru.yandex.practicum.repository.TagRepository;

import java.util.*;

@Slf4j
@Service
public class PostService {

    private final PostRepository postRepository;

    // решил не создавать TagService для TagRepository т.к. теги не являются самостоятельным бизнес‑объектом
    private final TagRepository tagRepository;

    private final PostMapper postMapper;

    public PostService(PostRepository postRepository, TagRepository tagRepository, PostMapper postMapper) {
        this.postRepository = postRepository;
        this.tagRepository = tagRepository;
        this.postMapper = postMapper;
    }

    @Transactional(readOnly = true)
    public PagePostsDto findPagePostsBySearch(
            String search,
            int pageNumber,
            int pageSize
    ) {
        String title = "";
        List<String> tags = new ArrayList<>();
        Long countPosts;

        // если поиск не пустой
        if (!search.isEmpty()) {
            String[] words = search.split(" ");
            LinkedList<String> titleList = new LinkedList<>();

            // формируем список тегов и заголовка
            for (String word : words) {
                if (word.isEmpty()) continue;
                if (word.startsWith("#")) {
                    word = word.replace("#", "");
                    if (!word.isEmpty()) tags.add(word);
                }
                else titleList.add(word);
            }
            // собираем заголовок согласно правилам поиска
            title = String.join(" ", titleList);

            // запрашиваем количество постов с фильтрами в зависимости от наличия тегов и заголовка
            if (!title.isEmpty() && !tags.isEmpty()) countPosts = postRepository.countPostsByTitleAndTags(title, tags);
            else if (!title.isEmpty()) countPosts = postRepository.countPostsByTitle(title);
            else countPosts = postRepository.countPostsByTags(tags);
        } else countPosts = postRepository.countPosts();

        int lastPage = countPosts > 0 ? (int) Math.ceil((double) countPosts /pageSize) : 0;
        boolean hasPrev = pageNumber > 1;
        boolean hasNext = pageNumber < lastPage;

        List<Post> posts = new ArrayList<>();

        // если посты есть, то запрашиваем их с фильтрами в зависимости от наличия тегов и заголовка
        if (countPosts > 0) {
            int offset = (pageNumber - 1) * pageSize;

            if (!title.isEmpty() && !tags.isEmpty()) posts = postRepository.findPagePostsByTitleAndTags(title, tags, pageSize, offset);
            else if (!title.isEmpty()) posts = postRepository.findPagePostsByTitle(title, pageSize, offset);
            else if (!tags.isEmpty()) posts = postRepository.findPagePostsByTags(tags, pageSize, offset);
            else posts = postRepository.findPagePosts(pageSize, offset);
        }

        return new PagePostsDto(
                posts.stream().map(postMapper::toPostPreviewDto).toList(),
                hasPrev,
                hasNext,
                lastPage
        );
    }

    @Transactional
    public PostDto addPost(NewPostDto newPostDto) {
        Post post = postMapper.toPost(newPostDto);
        Long postId = postRepository.save(post);
        post.setId(postId);
        log.info("Saved title and text for new postId: {}", postId);

        List<String> tags = post.getTags();
        if (!tags.isEmpty()) {
            // Вставка тегов
            List<Long> tagIds = tagRepository.saveAll(tags);
            // Сохранение связи пост-теги
            postRepository.linkPostTags(postId, tagIds);
            log.info("Post-tag links saved for new postId: {} and tagIds: {}", postId, tagIds);
        }
        log.info("New post saved with id: {} title: {}", postId, post.getTitle());
        return postMapper.toPostDto(post);
    }

    @Transactional(readOnly = true)
    public PostDto getPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        List<Tag> tags = tagRepository.findAllByPostId(postId);
        post.setTags(tags.stream().map(Tag::getName).toList());

        return postMapper.toPostDto(post);
    }

    @Transactional
    public PostDto updatePost(Long postId, UpdatePostDto updatePostDto) {
        if (!existsById(postId))
            throw new PostNotFoundException(postId);

        Post post = postMapper.toPost(updatePostDto);
        postRepository.update(postId, post);
        log.info("Updated title and text for postId: {}", postId);

        // Обновление тегов
        List<String> updateTags = updatePostDto.tags();
        if (!updateTags.isEmpty()) {
            // Вставка новых тегов
            List<Long> newTagIds = tagRepository.saveAll(updateTags);
            log.info("Updated tagIds: {} for postId: {}", newTagIds, postId);

            // Отвязываем удаленные теги от поста
            postRepository.clearPostTags(postId);
            log.info("Cleared post-tag links for postId: {}", postId);

            // Сохраняем новые связи поста с новыми тегами
            postRepository.linkPostTags(postId, newTagIds);
            log.info("Post-tag links updated for postId: {} and new tagIds: {}", postId, newTagIds);
        }
        log.info("Post updated with id: {} title: {}", postId, post.getTitle());
        return getPost(postId);
    }

    @Transactional
    public void deletePost(Long postId) {
        if (!existsById(postId))
            throw new PostNotFoundException(postId);

        postRepository.deleteById(postId);
        log.info("Deleted postId: {}", postId);
    }

    @Transactional
    public Long addLike(Long postId) {
        if (!existsById(postId))
            throw new PostNotFoundException(postId);

        return postRepository.addLike(postId);
    }

    @Transactional
    public boolean updateImage(Long postId, byte[] image) {
        if (!existsById(postId))
            throw new PostNotFoundException(postId);

        return postRepository.updateImage(postId, image);
    }

    @Transactional(readOnly = true)
    public byte[] getImage(Long postId) {
        if (!existsById(postId))
            throw new PostNotFoundException(postId);

        return postRepository.findImageById(postId)
                .orElseThrow(() -> new ImageNotFoundException(postId));
    }

    @Transactional(readOnly = true)
    public boolean existsById(Long postId) {
        return postRepository.existsById(postId);
    }

    public void incrementCommentsCount(Long postId) {
        boolean updatedCommentsCount = postRepository.incrementCommentsCount(postId);
        if (!updatedCommentsCount) {
            throw new CommentCountUpdateException(postId);
        }
    }

    public void decrementCommentsCount(Long postId) {
        boolean updatedCommentsCount =  postRepository.decrementCommentsCount(postId);
        if (!updatedCommentsCount) {
            throw new CommentCountUpdateException(postId);
        }
    }


}