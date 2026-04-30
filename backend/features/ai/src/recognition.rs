use anyhow::{Context, Result};

use crate::deepseek::DeepSeekClient;
use crate::model::{RecognizeRecipeResp, RecognizedIngredient};

pub struct RecognitionService {
    deepseek: DeepSeekClient,
}

impl RecognitionService {
    pub fn new(api_key: String) -> Result<Self> {
        let deepseek = DeepSeekClient::new(api_key)?;
        Ok(Self { deepseek })
    }

    pub async fn recognize_from_image(
        &self,
        image_url: &str,
        existing_title: Option<&str>,
    ) -> Result<RecognizeRecipeResp> {
        let response = self
            .deepseek
            .recognize_recipe(image_url, existing_title)
            .await?;

        self.parse_recognition_response(&response)
    }

    fn parse_recognition_response(&self, response: &str) -> Result<RecognizeRecipeResp> {
        let json_str = if let Some(start) = response.find('{') {
            if let Some(end) = response.rfind('}') {
                &response[start..=end]
            } else {
                response
            }
        } else {
            response
        };

        let parsed: serde_json::Value =
            serde_json::from_str(json_str).context("Failed to parse AI response as JSON")?;

        Ok(RecognizeRecipeResp {
            title: parsed["title"].as_str().unwrap_or("").to_string(),
            description: parsed["description"].as_str().map(String::from),
            difficulty: parsed["difficulty"].as_i64().map(|v| v as i16),
            cooking_time: parsed["cooking_time"].as_i64().map(|v| v as i32),
            servings: parsed["servings"].as_i64().map(|v| v as i16),
            ingredients: parsed["ingredients"]
                .as_array()
                .map(|arr| {
                    arr.iter()
                        .filter_map(|v| {
                            Some(RecognizedIngredient {
                                name: v["name"].as_str()?.to_string(),
                                amount: v["amount"].as_str().unwrap_or("适量").to_string(),
                            })
                        })
                        .collect()
                })
                .unwrap_or_default(),
            seasonings: parsed["seasonings"]
                .as_array()
                .map(|arr| {
                    arr.iter()
                        .filter_map(|v| {
                            Some(RecognizedIngredient {
                                name: v["name"].as_str()?.to_string(),
                                amount: v["amount"].as_str().unwrap_or("适量").to_string(),
                            })
                        })
                        .collect()
                })
                .unwrap_or_default(),
            steps: parsed["steps"]
                .as_array()
                .map(|arr| {
                    arr.iter()
                        .filter_map(|v| v.as_str().map(String::from))
                        .collect()
                })
                .unwrap_or_default(),
            confidence: parsed["confidence"].as_f64().unwrap_or(0.0),
        })
    }
}
