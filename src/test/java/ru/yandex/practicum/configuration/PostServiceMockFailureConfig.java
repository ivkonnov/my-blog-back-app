package ru.yandex.practicum.configuration;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import ru.yandex.practicum.service.PostService;

@Profile("test")
@Configuration
public class PostServiceMockFailureConfig {

    // Возвращает мок для PostService, у которого updateImage всегда возвращает false
    @Bean
    public PostService postService() {
        PostService postServiceMock = Mockito.mock(PostService.class);
        Mockito.doReturn(false).when(postServiceMock).updateImage(Mockito.anyLong(), Mockito.any(byte[].class));
        return postServiceMock;
    }

}
