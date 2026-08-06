package ru.yandex.practicum.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import ru.yandex.practicum.AbstractPostgresMvcTest;
import ru.yandex.practicum.configuration.PostServiceMockFailureConfig;
import ru.yandex.practicum.configuration.WebConfiguration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.practicum.exception.ErrorMessages.MSG_IMAGE_UPDATE_FAILED;

@SpringJUnitConfig(classes = {
        WebConfiguration.class,
        PostServiceMockFailureConfig.class
})
@ActiveProfiles("test")
@WebAppConfiguration
public class PostControllerMockServiceFailureTest extends AbstractPostgresMvcTest {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Nested
    class UpdateImage {
        @Test
        void updateImage_ImageUpdateFailed() throws Exception {
            MockMultipartFile image = new MockMultipartFile("image", "image.jpg", "image/jpeg", JPEG_IMAGE_STUB);

            mockMvc.perform(multipart("/api/posts/{postId}/image", 1L)
                            .file(image)
                            .with(request -> {
                                request.setMethod("PUT");
                                return request;
                            }))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value(MSG_IMAGE_UPDATE_FAILED));
        }
    }

}
