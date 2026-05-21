package ru.yandex.practicum.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.domain.Post;
import ru.yandex.practicum.dto.NewPostDto;
import ru.yandex.practicum.dto.PostDto;
import ru.yandex.practicum.mapper.PostMapper;
import ru.yandex.practicum.repository.PostRepository;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final PostMapper postMapper;

    public PostService(PostRepository postRepository, PostMapper postMapper) {
        this.postRepository = postRepository;
        this.postMapper = postMapper;
    }

    @Transactional
    public PostDto addPost(NewPostDto newPostDto) {
        Post post = postMapper.toPost(newPostDto);
        Long id = postRepository.save(post);
        post.setId(id);
        return postMapper.toPostDto(post);
    }
}
