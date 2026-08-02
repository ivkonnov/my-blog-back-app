package ru.yandex.practicum.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.repository.TagRepository;

import java.util.List;

@Slf4j
@Component
public class UnusedTagCleanScheduler {

    private final TagRepository tagRepository;

    public UnusedTagCleanScheduler(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    // Каждый день в полночь определяем и удаляем неиспользуемые теги (отвязанные от всех постов), чтобы не копился мусор
    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanUnusedTags() {
        List<Long> unUsedTagIds = tagRepository.cleanUnusedTags();
        if (!unUsedTagIds.isEmpty()) {
            log.info("Deleted unused tagIds: {}", unUsedTagIds);
        } else {
            log.info("No unused tags for delete");
        }
    }
}
