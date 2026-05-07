ALTER TABLE recipe_nutrition
    ADD COLUMN IF NOT EXISTS serving_size TEXT;
ALTER TABLE recipe_nutrition
    ADD COLUMN IF NOT EXISTS traffic_light JSONB;
ALTER TABLE recipe_nutrition
    ADD COLUMN IF NOT EXISTS overall_rating VARCHAR (10);
ALTER TABLE recipe_nutrition
    ADD COLUMN IF NOT EXISTS summary TEXT;
