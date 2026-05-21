-- Таблица с поcтами
CREATE TABLE IF NOT EXISTS posts (
    id             BIGSERIAL PRIMARY KEY,
    title          VARCHAR(128) NOT NULL,
    text           VARCHAR(4096) NOT NULL,
    likes_count    BIGINT DEFAULT 0,
    comments_count BIGINT DEFAULT 0,
    image          BYTEA
);

-- Таблица с тегами
CREATE TABLE IF NOT EXISTS tags (
    id   BIGSERIAL PRIMARY KEY,
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

CREATE INDEX IF NOT EXISTS idx_tags_name ON tags(name);
CREATE INDEX IF NOT EXISTS idx_posts_tags_post_id ON posts_tags(post_id);
