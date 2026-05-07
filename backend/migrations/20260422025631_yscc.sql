-- ============================================
-- 1. 用户体系
-- ============================================

-- 角色表
CREATE TABLE roles
(
    id          SERIAL PRIMARY KEY,
    code        VARCHAR(20) UNIQUE NOT NULL,
    name        VARCHAR(50)        NOT NULL,
    description VARCHAR(200)
);

-- 权限表
CREATE TABLE permissions
(
    id     SERIAL PRIMARY KEY,
    code   VARCHAR(50) UNIQUE NOT NULL,
    name   VARCHAR(100)       NOT NULL,
    module VARCHAR(30)        NOT NULL
);

-- 角色-权限关联
CREATE TABLE role_permissions
(
    role_id       INT REFERENCES roles (id) ON DELETE CASCADE,
    permission_id INT REFERENCES permissions (id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- 用户表
CREATE TABLE users
(
    id            UUID PRIMARY KEY          DEFAULT gen_random_uuid(),
    username      VARCHAR(50) UNIQUE NOT NULL,
    nickname      VARCHAR(50),
    password_hash VARCHAR(255)       NOT NULL,
    avatar        VARCHAR(500),
    role_id       INT REFERENCES roles (id) DEFAULT 2,
    is_active     BOOLEAN                   DEFAULT true,
    created_at    TIMESTAMPTZ               DEFAULT now(),
    updated_at    TIMESTAMPTZ               DEFAULT now()
);

-- ============================================
-- 2. 基础数据（食材 & 调料）
-- ============================================

-- 食材分类
CREATE TABLE ingredient_categories
(
    id         SERIAL PRIMARY KEY,
    name       VARCHAR(50) UNIQUE NOT NULL,
    sort_order INT DEFAULT 0
);

-- 食材库
CREATE TABLE ingredients
(
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(50) UNIQUE NOT NULL,
    category_id INT REFERENCES ingredient_categories (id),
    unit        VARCHAR(20) DEFAULT '克',
    image       VARCHAR(500),
    created_at  TIMESTAMPTZ DEFAULT now()
);

-- 调料库
CREATE TABLE seasonings
(
    id         SERIAL PRIMARY KEY,
    name       VARCHAR(50) UNIQUE NOT NULL,
    unit       VARCHAR(20) DEFAULT '克',
    image      VARCHAR(500),
    created_at TIMESTAMPTZ DEFAULT now()
);

-- ============================================
-- 3. 标签
-- ============================================

CREATE TABLE tags
(
    id         SERIAL PRIMARY KEY,
    name       VARCHAR(30) UNIQUE NOT NULL,
    type       VARCHAR(20)        NOT NULL,
    color      VARCHAR(7),
    sort_order INT DEFAULT 0
);

-- ============================================
-- 4. 菜谱核心
-- ============================================

-- 菜谱主表
CREATE TABLE recipes
(
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title        VARCHAR(200) NOT NULL,
    description  TEXT,
    cover_image  VARCHAR(500),
    difficulty   SMALLINT         DEFAULT 1,
    cooking_time INT,
    prep_time    INT,
    servings     SMALLINT         DEFAULT 2,
    source       VARCHAR(500),
    author_id    UUID REFERENCES users (id),
    status       SMALLINT         DEFAULT 1,
    created_at   TIMESTAMPTZ      DEFAULT now(),
    updated_at   TIMESTAMPTZ      DEFAULT now()
);

-- 菜谱-食材关联
CREATE TABLE recipe_ingredients
(
    id            SERIAL PRIMARY KEY,
    recipe_id     UUID REFERENCES recipes (id) ON DELETE CASCADE,
    ingredient_id INT REFERENCES ingredients (id),
    amount        DECIMAL(10, 2),
    unit          VARCHAR(20),
    note          VARCHAR(100),
    sort_order    INT DEFAULT 0,
    UNIQUE (recipe_id, ingredient_id)
);

-- 菜谱-调料关联
CREATE TABLE recipe_seasonings
(
    id           SERIAL PRIMARY KEY,
    recipe_id    UUID REFERENCES recipes (id) ON DELETE CASCADE,
    seasoning_id INT REFERENCES seasonings (id),
    amount       DECIMAL(10, 2),
    unit         VARCHAR(20),
    sort_order   INT DEFAULT 0,
    UNIQUE (recipe_id, seasoning_id)
);

-- 菜谱步骤
CREATE TABLE recipe_steps
(
    id          SERIAL PRIMARY KEY,
    recipe_id   UUID REFERENCES recipes (id) ON DELETE CASCADE,
    step_number INT  NOT NULL,
    content     TEXT NOT NULL,
    image       VARCHAR(500),
    duration    INT,
    UNIQUE (recipe_id, step_number)
);

-- 菜谱-标签关联
CREATE TABLE recipe_tags
(
    recipe_id UUID REFERENCES recipes (id) ON DELETE CASCADE,
    tag_id    INT REFERENCES tags (id) ON DELETE CASCADE,
    PRIMARY KEY (recipe_id, tag_id)
);

-- ============================================
-- 5. 互动功能
-- ============================================

-- 收藏
CREATE TABLE favorites
(
    user_id    UUID REFERENCES users (id) ON DELETE CASCADE,
    recipe_id  UUID REFERENCES recipes (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT now(),
    PRIMARY KEY (user_id, recipe_id)
);

-- 烹饪记录（评分 & 备注）
CREATE TABLE cooking_logs
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id  UUID REFERENCES recipes (id) ON DELETE CASCADE,
    user_id    UUID REFERENCES users (id),
    rating     SMALLINT CHECK (rating BETWEEN 1 AND 5),
    note       TEXT,
    cooked_at  DATE             DEFAULT CURRENT_DATE,
    created_at TIMESTAMPTZ      DEFAULT now(),
    updated_at TIMESTAMPTZ      DEFAULT now()
);

-- ============================================
-- 6. 工具功能
-- ============================================

-- 购物清单
CREATE TABLE shopping_lists
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID REFERENCES users (id),
    title      VARCHAR(100),
    status     SMALLINT         DEFAULT 1,
    created_at TIMESTAMPTZ      DEFAULT now()
);

-- 购物清单明细
CREATE TABLE shopping_list_items
(
    id         SERIAL PRIMARY KEY,
    list_id    UUID REFERENCES shopping_lists (id) ON DELETE CASCADE,
    name       VARCHAR(100) NOT NULL,
    amount     VARCHAR(50),
    is_checked BOOLEAN DEFAULT false,
    sort_order INT     DEFAULT 0
);

-- 周菜单计划
CREATE TABLE meal_plans
(
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id   UUID REFERENCES users (id),
    recipe_id UUID     REFERENCES recipes (id) ON DELETE SET NULL,
    plan_date DATE     NOT NULL,
    meal_type SMALLINT NOT NULL,
    note      VARCHAR(200),
    UNIQUE (user_id, plan_date, meal_type)
);

-- ============================================
-- 7. 索引
-- ============================================

CREATE INDEX idx_recipes_author ON recipes (author_id);
CREATE INDEX idx_recipes_status ON recipes (status);
CREATE INDEX idx_recipes_created ON recipes (created_at DESC);
CREATE INDEX idx_recipe_ingredients_recipe ON recipe_ingredients (recipe_id);
CREATE INDEX idx_recipe_seasonings_recipe ON recipe_seasonings (recipe_id);
CREATE INDEX idx_recipe_steps_recipe ON recipe_steps (recipe_id, step_number);
CREATE INDEX idx_cooking_logs_recipe ON cooking_logs (recipe_id);
CREATE INDEX idx_cooking_logs_user ON cooking_logs (user_id);
CREATE INDEX idx_favorites_user ON favorites (user_id);
CREATE INDEX idx_meal_plans_date ON meal_plans (user_id, plan_date);
CREATE INDEX idx_ingredients_name ON ingredients (name);
CREATE INDEX idx_recipes_title ON recipes USING gin(to_tsvector('simple', title));

-- ============================================
-- 8. 初始数据
-- ============================================

-- 角色
INSERT INTO roles (code, name, description)
VALUES ('admin', '管理员', '系统管理员，拥有所有权限'),
       ('user', '普通用户', '普通用户，可以创建和管理自己的菜谱');

-- 权限
INSERT INTO permissions (code, name, module)
VALUES ('user:manage', '管理用户', 'user'),
       ('recipe:create', '创建菜谱', 'recipe'),
       ('recipe:edit_own', '编辑自己的菜谱', 'recipe'),
       ('recipe:edit_all', '编辑所有菜谱', 'recipe'),
       ('recipe:delete_own', '删除自己的菜谱', 'recipe'),
       ('recipe:delete_all', '删除所有菜谱', 'recipe'),
       ('base_data:manage', '管理基础数据', 'system');

-- 角色-权限（admin 拥有所有权限）
INSERT INTO role_permissions (role_id, permission_id)
SELECT 1, id
FROM permissions;

-- 角色-权限（user 基础权限）
INSERT INTO role_permissions (role_id, permission_id)
SELECT 2, id
FROM permissions
WHERE code IN ('recipe:create', 'recipe:edit_own', 'recipe:delete_own');

-- 食材分类
INSERT INTO ingredient_categories (name, sort_order)
VALUES ('蔬菜', 1),
       ('肉类', 2),
       ('海鲜水产', 3),
       ('蛋奶', 4),
       ('豆制品', 5),
       ('主食', 6),
       ('水果', 7),
       ('干货', 8);

-- 常用调料
INSERT INTO seasonings (name, unit)
VALUES ('盐', '克'),
       ('生抽', '毫升'),
       ('老抽', '毫升'),
       ('料酒', '毫升'),
       ('醋', '毫升'),
       ('白糖', '克'),
       ('蚝油', '毫升'),
       ('豆瓣酱', '克'),
       ('花椒', '克'),
       ('干辣椒', '个'),
       ('八角', '个'),
       ('桂皮', '块'),
       ('姜', '片'),
       ('蒜', '瓣'),
       ('葱', '根'),
       ('香油', '毫升'),
       ('胡椒粉', '克'),
       ('五香粉', '克'),
       ('淀粉', '克'),
       ('食用油', '毫升');

-- 标签
INSERT INTO tags (name, type, sort_order)
VALUES ('川菜', 'cuisine', 1),
       ('粤菜', 'cuisine', 2),
       ('湘菜', 'cuisine', 3),
       ('鲁菜', 'cuisine', 4),
       ('江浙菜', 'cuisine', 5),
       ('西餐', 'cuisine', 6),
       ('日料', 'cuisine', 7),
       ('韩餐', 'cuisine', 8),
       ('麻辣', 'taste', 1),
       ('清淡', 'taste', 2),
       ('酸甜', 'taste', 3),
       ('咸鲜', 'taste', 4),
       ('香辣', 'taste', 5),
       ('快手菜', 'scene', 1),
       ('家常菜', 'scene', 2),
       ('宴客菜', 'scene', 3),
       ('早餐', 'scene', 4),
       ('夜宵', 'scene', 5),
       ('便当', 'scene', 6),
       ('减脂', 'diet', 1),
       ('高蛋白', 'diet', 2),
       ('素食', 'diet', 3);
