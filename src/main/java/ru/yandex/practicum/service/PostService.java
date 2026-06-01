package ru.yandex.practicum.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.domain.Post;
import ru.yandex.practicum.dto.NewPostDto;
import ru.yandex.practicum.dto.PagePostsDto;
import ru.yandex.practicum.dto.PostDto;
import ru.yandex.practicum.mapper.PostMapper;
import ru.yandex.practicum.repository.PostRepository;

import java.util.*;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final PostMapper postMapper;

    public PostService(PostRepository postRepository, PostMapper postMapper) {
        this.postRepository = postRepository;
        this.postMapper = postMapper;
    }

    @Transactional(readOnly = true)
    public PagePostsDto getPosts(
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
                if (word.startsWith("#")) tags.add(word.substring(1));
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
                posts.stream().map(postMapper::toPostDto).toList(),
                hasPrev,
                hasNext,
                lastPage
        );
    }

    @Transactional
    public PostDto addPost(NewPostDto newPostDto) {
        Post post = postMapper.toPost(newPostDto);
        Long id = postRepository.save(post);
        post.setId(id);
        return postMapper.toPostDto(post);
    }
}
