-- 将食材和调料的 amount 字段从 DECIMAL 改为 VARCHAR，支持自由文本（如 "500g"、"适量"、"2勺"）
ALTER TABLE recipe_ingredients ALTER COLUMN amount TYPE VARCHAR(50) USING amount::text;
ALTER TABLE recipe_seasonings ALTER COLUMN amount TYPE VARCHAR(50) USING amount::text;
