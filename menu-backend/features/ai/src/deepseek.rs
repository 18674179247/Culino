use anyhow::{Context, Result};
use serde::{Deserialize, Serialize};
use std::time::Duration;

/// DeepSeek API 客户端
pub struct DeepSeekClient {
    api_key: String,
    base_url: String,
    client: reqwest::Client,
}

/// DeepSeek API 请求
#[derive(Debug, Serialize)]
struct ChatCompletionRequest {
    model: String,
    messages: Vec<Message>,
    temperature: f32,
    max_tokens: u32,
}

#[derive(Debug, Serialize, Deserialize)]
struct Message {
    role: String,
    content: String,
}

/// DeepSeek API 响应
#[derive(Debug, Deserialize)]
struct ChatCompletionResponse {
    choices: Vec<Choice>,
}

#[derive(Debug, Deserialize)]
struct Choice {
    message: Message,
}

impl DeepSeekClient {
    /// 创建 DeepSeek 客户端
    pub fn new(api_key: String) -> Result<Self> {
        let client = reqwest::Client::builder()
            .timeout(Duration::from_secs(60))
            .build()
            .context("Failed to create HTTP client")?;

        Ok(Self {
            api_key,
            base_url: "https://api.deepseek.com/v1".to_string(),
            client,
        })
    }

    /// 调用 Chat Completion API
    pub async fn chat_completion(&self, prompt: String) -> Result<String> {
        let url = format!("{}/chat/completions", self.base_url);

        let request = ChatCompletionRequest {
            model: "deepseek-chat".to_string(),
            messages: vec![
                Message {
                    role: "system".to_string(),
                    content: "你是一个专业的营养师和美食专家，擅长分析菜谱的营养成分和健康价值。请用简洁、专业的语言回答。".to_string(),
                },
                Message {
                    role: "user".to_string(),
                    content: prompt,
                },
            ],
            temperature: 0.7,
            max_tokens: 2000,
        };

        tracing::debug!("Calling DeepSeek API: {}", url);

        let response = self
            .client
            .post(&url)
            .header("Authorization", format!("Bearer {}", self.api_key))
            .header("Content-Type", "application/json")
            .json(&request)
            .send()
            .await
            .context("Failed to send request to DeepSeek API")?;

        if !response.status().is_success() {
            let status = response.status();
            let error_text = response.text().await.unwrap_or_default();
            anyhow::bail!("DeepSeek API error: {} - {}", status, error_text);
        }

        let completion: ChatCompletionResponse = response
            .json()
            .await
            .context("Failed to parse DeepSeek API response")?;

        let content = completion
            .choices
            .first()
            .map(|c| c.message.content.clone())
            .context("No response from DeepSeek API")?;

        tracing::debug!("DeepSeek API response received: {} chars", content.len());

        Ok(content)
    }

    /// 分析菜谱营养成分
    pub async fn analyze_nutrition(
        &self,
        recipe_title: &str,
        ingredients: &str,
        seasonings: &str,
        servings: i16,
    ) -> Result<String> {
        let prompt = format!(
            r#"请分析以下菜谱的营养成分和健康信息：

菜名：{}
食材：{}
调料：{}
份数：{}

请以 JSON 格式返回以下信息（只返回 JSON，不要其他文字）：
{{
  "calories": 每份热量（千卡，数字）,
  "protein": 每份蛋白质（克，数字）,
  "fat": 每份脂肪（克，数字）,
  "carbohydrate": 每份碳水化合物（克，数字）,
  "fiber": 每份膳食纤维（克，数字）,
  "sodium": 每份钠（毫克，数字）,
  "analysis_text": "详细的营养分析说明（100-200字）",
  "health_score": 健康评分（1-100的整数）,
  "health_tags": ["健康标签1", "健康标签2"],
  "suitable_for": ["适合人群1", "适合人群2"],
  "cautions": ["注意事项1", "注意事项2"]
}}

注意：
1. 所有数值必须是合理的估算值
2. health_tags 例如：低脂、高蛋白、低钠、高纤维等
3. suitable_for 例如：减脂人群、健身人群、儿童、老人、孕妇等
4. cautions 例如：高盐、高糖、高热量、高胆固醇等（如果没有就返回空数组）
"#,
            recipe_title, ingredients, seasonings, servings
        );

        self.chat_completion(prompt).await
    }

    /// 生成推荐理由
    pub async fn generate_recommendation_reason(
        &self,
        recipe_title: &str,
        user_preferences: &str,
        recommendation_type: &str,
    ) -> Result<String> {
        let prompt = format!(
            r#"用户偏好：{}

推荐类型：{}
推荐菜谱：{}

请用一句话（20-30字）说明为什么推荐这道菜给用户。要简洁、吸引人。
"#,
            user_preferences, recommendation_type, recipe_title
        );

        self.chat_completion(prompt).await
    }
}
