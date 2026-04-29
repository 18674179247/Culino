CREATE TABLE recipe_likes (
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    recipe_id UUID REFERENCES recipes(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT now(),
    PRIMARY KEY (user_id, recipe_id)
);

CREATE INDEX idx_likes_recipe ON recipe_likes(recipe_id);

CREATE TABLE recipe_comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id UUID REFERENCES recipes(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL CHECK (char_length(content) BETWEEN 1 AND 1000),
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_comments_recipe ON recipe_comments(recipe_id, created_at DESC);
CREATE INDEX idx_comments_user ON recipe_comments(user_id);
