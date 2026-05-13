use anyhow::{Context, Result};
use serde::{Deserialize, Serialize};
use std::sync::OnceLock;
use std::time::Duration;

/// 全局共享的 reqwest::Client。
/// reqwest 官方推荐:一个进程用一个 Client,内部维护连接池;每次 new() 都会重建 TLS 栈、DNS 缓存和连接池,
/// 在高并发下造成不必要的资源消耗。通过 OnceLock 保证只构造一次。
fn http_client() -> &'static reqwest::Client {
    static CLIENT: OnceLock<reqwest::Client> = OnceLock::new();
    CLIENT.get_or_init(|| {
        reqwest::Client::builder()
            .timeout(Duration::from_secs(60))
            .build()
            .expect("Failed to build shared reqwest Client")
    })
}

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
    /// 创建 DeepSeek 客户端(复用进程级共享的 reqwest::Client)
    pub fn new(api_key: String) -> Result<Self> {
        Ok(Self {
            api_key,
            base_url: "https://api.deepseek.com/v1".to_string(),
            client: http_client().clone(),
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
  "cautions": ["注意事项1", "注意事项2"],
  "serving_size": "每份的份量描述（如：每份（约350g））",
  "traffic_light": {{
    "fat": "green/amber/red",
    "saturated_fat": "green/amber/red",
    "sugar": "green/amber/red",
    "sodium": "green/amber/red"
  }},
  "overall_rating": "green/amber/red（整体健康评级）",
  "summary": "2-3句话的整体营养评价总结"
}}

注意：
1. 所有数值必须是合理的估算值
2. health_tags 例如：低脂、高蛋白、低钠、高纤维等
3. suitable_for 例如：减脂人群、健身人群、儿童、老人、孕妇等
4. cautions 例如：高盐、高糖、高热量、高胆固醇等（如果没有就返回空数组）
5. traffic_light 红绿灯标识：green=低含量，amber=中等含量，red=高含量
6. overall_rating 综合红绿灯评级：green=健康，amber=适量食用，red=需注意
7. serving_size 格式示例："每份（约350g）"
8. summary 用2-3句话概括这道菜的营养特点和建议
"#,
            recipe_title, ingredients, seasonings, servings
        );

        self.chat_completion(prompt).await
    }

    /// 识别菜谱（根据菜名或图片生成完整菜谱）
    pub async fn recognize_recipe(
        &self,
        _image_url: &str,
        existing_title: Option<&str>,
    ) -> Result<String> {
        // Since DeepSeek chat model doesn't support vision, use the title or ask AI to generate
        // a complete recipe based on the dish name (which user confirms after upload)
        let prompt = if let Some(title) = existing_title {
            format!(
                r#"请根据菜名「{}」生成一份完整的菜谱。

关于份数（servings）的判断规则：
1. 优先从菜名中提取份数线索：如"单人餐"→1，"双人餐"→2，"三人份"→3，"家庭装"→4
2. 如果菜名中没有明确的份数线索，根据食材总量和菜品类型合理推断，普通家常菜默认为 1 份
3. 食材用量必须与份数匹配，1 份的食材量应该是单人合理食用量

关于标签（tags）的选择规则：
从以下标签中选择 1-3 个最匹配的：
- 菜系：川菜、粤菜、湘菜、鲁菜、江浙菜、西餐、日料、韩餐
- 口味：麻辣、清淡、酸甜、咸鲜、香辣
- 场景：快手菜、家常菜、宴客菜、早餐、夜宵、便当
- 饮食：减脂、高蛋白、素食

请以 JSON 格式返回（只返回 JSON，不要其他文字）：
{{
  "title": "菜名",
  "description": "菜品简介（50-100字）",
  "difficulty": 难度（1-5的整数，1最简单），
  "cooking_time": 烹饪时间（分钟，整数），
  "servings": 份数（整数，严格按照上述规则判断），
  "ingredients": [
    {{"name": "食材名", "amount": "用量（如500g、2个）"}}
  ],
  "seasonings": [
    {{"name": "调料名", "amount": "用量（如2勺、适量）"}}
  ],
  "steps": [
    "步骤1描述",
    "步骤2描述"
  ],
  "tags": ["标签1", "标签2"],
  "confidence": 0.9
}}"#,
                title
            )
        } else {
            r#"用户上传了一张菜品图片但未提供菜名。请返回一个空的菜谱模板。

请以 JSON 格式返回（只返回 JSON，不要其他文字）：
{
  "title": "",
  "description": "",
  "difficulty": 3,
  "cooking_time": 30,
  "servings": 1,
  "ingredients": [],
  "seasonings": [],
  "steps": [],
  "confidence": 0.0
}"#
            .to_string()
        };

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

    pub async fn parse_shopping_list(&self, text: &str) -> Result<String> {
        let prompt = format!(
            r#"请将以下购物清单文本解析为结构化数据。文本可能包含口语化表达、数量单位不规范等情况，请智能识别。

文本：{}

请以 JSON 格式返回（只返回 JSON 数组，不要其他文字）：
[
  {{"name": "物品名称", "amount": "数量（如2个、500g、一瓶）"}}
]

规则：
- 如果没有明确数量，amount 填 "适量"
- 合并重复项
- 去除无关内容（如"我要买"、"帮我加"等）
- 保持物品名称简洁"#,
            text
        );

        self.chat_completion(prompt).await
    }
}
