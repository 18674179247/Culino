-- ============================================
-- AI 功能扩展
-- ============================================

-- ============================================
-- 1. AI 营养分析表
-- ============================================

-- 菜谱营养分析表（AI 自动生成）
CREATE TABLE recipe_nutrition (
    recipe_id UUID PRIMARY KEY REFERENCES recipes(id) ON DELETE CASCADE,
    -- 营养成分（每份）
    calories DECIMAL(10,2),           -- 总热量（千卡/kcal）
    protein DECIMAL(10,2),            -- 蛋白质（克）
    fat DECIMAL(10,2),                -- 脂肪（克）
    carbohydrate DECIMAL(10,2),       -- 碳水化合物（克）
    fiber DECIMAL(10,2),              -- 膳食纤维（克）
    sodium DECIMAL(10,2),             -- 钠（毫克）

    -- AI 分析结果
    analysis_text TEXT,               -- AI 生成的营养分析文本
    health_score SMALLINT CHECK (health_score BETWEEN 1 AND 100), -- 健康评分 1-100
    health_tags TEXT[],               -- 健康标签：["低脂", "高蛋白", "低钠"]
    suitable_for TEXT[],              -- 适合人群：["减脂", "健身", "儿童", "老人"]
    cautions TEXT[],                  -- 注意事项：["高盐", "高糖", "高热量"]

    -- 时间戳
    generated_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- ============================================
-- 2. 用户偏好分析表
-- ============================================

-- 用户偏好画像表（AI 计算）
CREATE TABLE user_preferences (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,

    -- 偏好数据（JSONB 格式存储权重）
    -- 格式：{"川菜": 0.8, "粤菜": 0.6, ...}
    favorite_cuisines JSONB,          -- 喜欢的菜系及权重
    favorite_tastes JSONB,            -- 喜欢的口味及权重
    favorite_ingredients JSONB,       -- 喜欢的食材及权重（ingredient_id: weight）
    favorite_tags JSONB,              -- 喜欢的标签及权重

    -- 用户特征
    dietary_restrictions TEXT[],      -- 饮食限制：["素食", "无麸质", "低糖"]
    health_goals TEXT[],              -- 健康目标：["减脂", "增肌", "保持健康"]
    avg_cooking_time INT,             -- 平均烹饪时间偏好（分钟）
    difficulty_preference SMALLINT,   -- 难度偏好 1-5

    -- 统计数据
    total_favorites INT DEFAULT 0,    -- 总收藏数
    total_cooking_logs INT DEFAULT 0, -- 总烹饪记录数
    avg_rating DECIMAL(3,2),          -- 平均评分

    -- 时间戳
    last_analyzed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- ============================================
-- 3. AI 推荐记录表
-- ============================================

-- AI 推荐记录表
CREATE TABLE ai_recommendations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    recipe_id UUID REFERENCES recipes(id) ON DELETE CASCADE,

    -- 推荐信息
    recommendation_type VARCHAR(50) NOT NULL,  -- 'personalized', 'similar', 'trending', 'health_goal'
    score DECIMAL(5,2) NOT NULL,               -- 推荐分数 0-100
    reason TEXT,                               -- 推荐理由（AI 生成）

    -- 用户反馈
    clicked BOOLEAN DEFAULT false,
    clicked_at TIMESTAMPTZ,

    -- 时间戳
    created_at TIMESTAMPTZ DEFAULT now()
);

-- ============================================
-- 4. 用户行为日志表
-- ============================================

-- 用户行为分析表（用于训练推荐模型）
CREATE TABLE user_behavior_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    recipe_id UUID REFERENCES recipes(id) ON DELETE CASCADE,

    -- 行为类型
    action_type VARCHAR(30) NOT NULL,  -- 'view', 'favorite', 'unfavorite', 'cook', 'rate', 'search', 'share'

    -- 额外数据（JSONB 格式）
    -- 例如：{"rating": 5}, {"keyword": "宫保鸡丁"}, {"duration": 120}
    action_value JSONB,

    -- 时间戳
    created_at TIMESTAMPTZ DEFAULT now()
);

-- ============================================
-- 5. 索引优化
-- ============================================

-- 营养分析表索引
CREATE INDEX idx_nutrition_recipe ON recipe_nutrition(recipe_id);
CREATE INDEX idx_nutrition_health_score ON recipe_nutrition(health_score DESC);

-- 用户偏好表索引
CREATE INDEX idx_preferences_user ON user_preferences(user_id);
CREATE INDEX idx_preferences_updated ON user_preferences(updated_at DESC);

-- 推荐记录表索引
CREATE INDEX idx_recommendations_user ON ai_recommendations(user_id, created_at DESC);
CREATE INDEX idx_recommendations_recipe ON ai_recommendations(recipe_id);
CREATE INDEX idx_recommendations_type ON ai_recommendations(recommendation_type, score DESC);
CREATE INDEX idx_recommendations_clicked ON ai_recommendations(user_id, clicked);

-- 行为日志表索引
CREATE INDEX idx_behavior_logs_user ON user_behavior_logs(user_id, created_at DESC);
CREATE INDEX idx_behavior_logs_recipe ON user_behavior_logs(recipe_id, created_at DESC);
CREATE INDEX idx_behavior_logs_action ON user_behavior_logs(action_type, created_at DESC);

-- ============================================
-- 6. 触发器：自动更新时间戳
-- ============================================

-- 营养分析更新时间触发器
CREATE OR REPLACE FUNCTION update_nutrition_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_nutrition_timestamp
BEFORE UPDATE ON recipe_nutrition
FOR EACH ROW
EXECUTE FUNCTION update_nutrition_timestamp();

-- 用户偏好更新时间触发器
CREATE OR REPLACE FUNCTION update_preference_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_preference_timestamp
BEFORE UPDATE ON user_preferences
FOR EACH ROW
EXECUTE FUNCTION update_preference_timestamp();

-- ============================================
-- 7. 注释说明
-- ============================================

COMMENT ON TABLE recipe_nutrition IS 'AI 自动生成的菜谱营养分析数据';
COMMENT ON TABLE user_preferences IS '用户偏好画像，基于行为数据 AI 分析生成';
COMMENT ON TABLE ai_recommendations IS 'AI 推荐记录，用于追踪推荐效果';
COMMENT ON TABLE user_behavior_logs IS '用户行为日志，用于推荐系统训练';

COMMENT ON COLUMN recipe_nutrition.calories IS '每份热量（千卡）';
COMMENT ON COLUMN recipe_nutrition.health_score IS '健康评分 1-100，越高越健康';
COMMENT ON COLUMN user_preferences.favorite_cuisines IS 'JSONB 格式：{"川菜": 0.8, "粤菜": 0.6}';
COMMENT ON COLUMN ai_recommendations.recommendation_type IS '推荐类型：personalized(个性化)/similar(相似)/trending(热门)/health_goal(健康目标)';
COMMENT ON COLUMN user_behavior_logs.action_type IS '行为类型：view/favorite/unfavorite/cook/rate/search/share';
