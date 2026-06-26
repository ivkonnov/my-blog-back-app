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
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (post_id, tag_id),
    FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags (id)
);

-- Таблица с комментариями
CREATE TABLE IF NOT EXISTS comments (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    text VARCHAR(460) NOT NULL,
    post_id BIGINT NOT NULL,
    FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_posts_title ON posts(title);
CREATE INDEX IF NOT EXISTS idx_tags_name ON tags(name);
CREATE INDEX IF NOT EXISTS idx_posts_tags_post_id ON posts_tags(post_id);
CREATE INDEX IF NOT EXISTS idx_posts_tags_tag_id ON posts_tags(tag_id);
