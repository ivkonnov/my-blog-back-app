-- Таблица с поcтами
CREATE TABLE IF NOT EXISTS posts (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title          VARCHAR(128) NOT NULL,
    text           VARCHAR(4096) NOT NULL,
    likes_count    BIGINT DEFAULT 0,
    comments_count BIGINT DEFAULT 0,
    image          BYTEA
);

-- Таблица с тегами
CREATE TABLE IF NOT EXISTS tags (
    id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(25) UNIQUE NOT NULL
);

-- Таблица связи постов и тегов
CREATE TABLE IF NOT EXISTS posts_tags (
    post_id BIGINT NOT NULL,
    tag_id  BIGINT NOT NULL,
    PRIMARY KEY (post_id, tag_id),
    FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags (id)
);

-- Таблица с комментариями
CREATE TABLE IF NOT EXISTS comments (
    id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    text    VARCHAR(460) NOT NULL,
    post_id BIGINT NOT NULL,
    FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE
);

-- Индексы
CREATE INDEX IF NOT EXISTS idx_posts_title ON posts(title);
CREATE INDEX IF NOT EXISTS idx_tags_name ON tags(name);
CREATE INDEX IF NOT EXISTS idx_posts_tags_post_id ON posts_tags(post_id);
CREATE INDEX IF NOT EXISTS idx_posts_tags_tag_id ON posts_tags(tag_id);
CREATE INDEX IF NOT EXISTS idx_comments_post_id ON comments(post_id);

/**
  Добавляем 19 тестовых постов:
    - 4 поста с названием "Название i-ой публикации" и контентом > 128 символов, из них:
      - 3 поста c тегами "пост_i" и "заметка"
      - 1 пост c тегами "пост_i", "заметка" и "лонгрид"

    - 15 постов с названием "Название i-ого поста", из них:
      - 7 постов только с тегом "пост_i"
      - 3 поста c тегами "пост_i" и "заметка"
      - 3 поста c тегами "пост_i" и "лонгрид"
      - 2 поста с тегами "пост_i", "заметка" и "лонгрид"
*/

-- Очищаем таблицы и восстанавливаем последовательности первичных ключей
TRUNCATE TABLE posts_tags, posts, tags RESTART IDENTITY CASCADE;

-- Тестовые данные
INSERT INTO posts (title, text) VALUES
('Название 1-ого поста', 'Контент 1-ого поста'),
('Название 2-ого поста', 'Контент 2-ого поста'),
('Название 3-ого поста', 'Контент 3-ого поста'),
('Название 4-ой публикации', 'Контент 4-ой публикации > 128 символов aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa'),
('Название 5-ого поста', 'Контент 5-ого поста'),
('Название 6-ого поста', 'Контент 6-ого поста'),
('Название 7-ого поста', 'Контент 7-ого поста'),
('Название 8-ой публикации', 'Контент 8-ой публикации > 128 символов aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa'),
('Название 9-ого поста', 'Контент 9-ого поста'),
('Название 10-ого поста', 'Контент 10-ого поста'),
('Название 11-ого поста', 'Контент 11-ого поста'),
('Название 12-ой публикации', 'Контент 12-ой публикации > 128 символов aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa'),
('Название 13-ого поста', 'Контент 13-ого поста'),
('Название 14-ого поста', 'Контент 14-ого поста'),
('Название 15-ого поста', 'Контент 15-ого поста'),
('Название 16-ой публикации', 'Контент 16-ой публикации > 128 символов aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa'),
('Название 17-ого поста', 'Контент 17-ого поста'),
('Название 18-ого поста', 'Контент 18-ого поста'),
('Название 19-ого поста', 'Контент 19-ого поста');

INSERT INTO tags (name) VALUES
('пост_1'), ('пост_2'), ('пост_3'), ('пост_4'), ('пост_5'), ('пост_6'), ('пост_7'), ('пост_8'), ('пост_9'), ('пост_10'),
('пост_11'), ('пост_12'), ('пост_13'), ('пост_14'), ('пост_15'), ('пост_16'), ('пост_17'), ('пост_18'), ('пост_19'),
('заметка'), -- id: 20
('лонгрид'); -- id: 21

-- Связываем посты и теги
INSERT INTO posts_tags (post_id, tag_id) VALUES
('1', '1'),                                 -- 1-й пост с тегом "пост_1"
('2', '2'), ('2', '20'),                    -- 2-й пост с тегами "пост_2" и "заметка"
('3', '3'), ('3', '21'),                    -- 3-й пост с тегами "пост_3" и "лонгрид"
('4', '4'), ('4', '20'),                    -- 4-й пост с тегами "пост_4" и "заметка"
('5', '5'),                                 -- 5-й пост с тегом "пост_5"
('6', '6'), ('6', '20'), ('6', '21'),       -- 6-й пост с тегами "пост_6", "заметка" и "лонгрид"
('7', '7'),                                 -- 7-й пост с тегом "пост_7"
('8', '8'), ('8', '20'),                    -- 8-й пост с тегами "пост_8" и "заметка"
('9', '9'), ('9', '21'),                    -- 9-й пост с тегами "пост_9" и "лонгрид"
('10', '10'), ('10', '20'),                 -- 10-й пост с тегами "пост_10" и "заметка"
('11', '11'),                               -- 11-й пост с тегом "пост_11"
('12', '12'), ('12', '20'), ('12', '21'),   -- 12-й пост с тегами "пост_12", "заметка" и "лонгрид"
('13', '13'),                               -- 13-й пост с тегом "пост_13"
('14', '14'), ('14', '20'),                 -- 14-й пост с тегами "пост_14" и "заметка"
('15', '15'), ('15', '21'),                 -- 15-й пост с тегами "пост_15" и "лонгрид"
('16', '16'), ('16', '20'),                 -- 16-й пост с тегами "пост_16" и "заметка"
('17', '17'),                               -- 17-й пост с тегом "пост_17"
('18', '18'), ('18', '20'), ('18', '21'),   -- 18-й пост с тегами "пост_18", "заметка" и "лонгрид"
('19', '19');                               -- 19-й пост с тегом "пост_19"

-- Комментарии для 1-го поста
INSERT INTO comments (text, post_id) VALUES
('Комментарий 1', 1),
('Комментарий 2', 1),
('Комментарий 3', 1),
('Комментарий 4', 1),
('Комментарий 5', 1),
('Комментарий 6', 1),
('Комментарий 7', 1),
('Комментарий 8', 1),
('Комментарий 9', 1),
('Комментарий 10', 1);

-- Обновляем счетчик комментариев после добавления 10 комментариев для 1-го поста
UPDATE posts SET comments_count = 10 WHERE id = 1;