//! 路由构建
//!
//! 挂载 Swagger UI 和所有业务模块，添加中间件层。

use axum::Router;
use axum::extract::DefaultBodyLimit;
use axum::http::{HeaderName, Method, StatusCode};
use axum::response::Json;
use culino_common::config::{AppConfig, RunMode};
use culino_common::state::AppState;
use serde_json::json;
use std::time::Duration;
use tower_governor::{GovernorLayer, governor::GovernorConfigBuilder};
use tower_http::cors::{AllowOrigin, CorsLayer};
use tower_http::request_id::{MakeRequestUuid, PropagateRequestIdLayer, SetRequestIdLayer};
use tower_http::timeout::TimeoutLayer;
use tower_http::trace::TraceLayer;
use utoipa_swagger_ui::SwaggerUi;

/// 健康检查
async fn health() -> Json<serde_json::Value> {
    Json(json!({ "status": "ok" }))
}

/// AI 模块路由（独立 120s 超时，因为 AI API 调用耗时较长）
fn ai_routes() -> Router<AppState> {
    Router::new()
        // 营养分析
        .route(
            "/nutrition/analyze/{recipe_id}",
            axum::routing::post(culino_ai::analyze_nutrition),
        )
        .route(
            "/nutrition/{recipe_id}",
            axum::routing::get(culino_ai::get_nutrition),
        )
        // 推荐系统
        .route(
            "/recommend/personalized",
            axum::routing::get(culino_ai::personalized_recommendations),
        )
        .route(
            "/recommend/similar/{recipe_id}",
            axum::routing::get(culino_ai::similar_recommendations),
        )
        .route(
            "/recommend/trending",
            axum::routing::get(culino_ai::trending_recommendations),
        )
        .route(
            "/recommend/health/{goal}",
            axum::routing::get(culino_ai::health_goal_recommendations),
        )
        // 用户偏好
        .route(
            "/preference/analyze",
            axum::routing::post(culino_ai::analyze_preference),
        )
        .route(
            "/preference/profile",
            axum::routing::get(culino_ai::get_preference_profile),
        )
        // 行为日志
        .route(
            "/behavior/log",
            axum::routing::post(culino_ai::log_behavior),
        )
        // AI 菜谱识别
        .route(
            "/recipe/recognize",
            axum::routing::post(culino_ai::recognize_recipe),
        )
        // AI 购物清单解析
        .route(
            "/shopping-list/parse",
            axum::routing::post(culino_ai::parse_shopping_text),
        )
        .layer(TimeoutLayer::with_status_code(
            StatusCode::REQUEST_TIMEOUT,
            Duration::from_secs(120),
        ))
}

/// 构建 CORS 层。
/// - 生产：`cors_origins` 已在 config 中校验非空，此处直接使用白名单
/// - 开发：若白名单为空则允许所有（兼容本地调试）
fn build_cors(config: &AppConfig) -> CorsLayer {
    let cors = CorsLayer::new()
        .allow_methods([
            Method::GET,
            Method::POST,
            Method::PUT,
            Method::DELETE,
            Method::OPTIONS,
        ])
        .allow_headers([
            axum::http::header::AUTHORIZATION,
            axum::http::header::CONTENT_TYPE,
        ]);

    if config.cors_origins.is_empty() {
        // 仅 dev 走到这里（production 在 config 构造时已 panic）
        cors.allow_origin(AllowOrigin::any())
    } else {
        let origins: Vec<_> = config
            .cors_origins
            .iter()
            .filter_map(|o| o.parse().ok())
            .collect();
        cors.allow_origin(origins)
    }
}

/// 构建应用路由，挂载 Swagger UI 和所有业务模块
pub fn build_router(state: AppState, doc: utoipa::openapi::OpenApi) -> Router {
    let x_request_id = HeaderName::from_static("x-request-id");

    // ---------- 限流配置 ----------
    // 登录/注册：对抗暴力破解，每 IP 12 秒 1 次，突发 5（约 5/min 稳态）
    let auth_governor = GovernorConfigBuilder::default()
        .per_second(12)
        .burst_size(5)
        .finish()
        .expect("auth rate limit config invalid");

    // AI 接口：防刷 DeepSeek 配额，每 IP 6 秒 1 次，突发 5（约 10/min 稳态）
    let ai_governor = GovernorConfigBuilder::default()
        .per_second(6)
        .burst_size(5)
        .finish()
        .expect("ai rate limit config invalid");

    // 其他业务接口：全局宽松限流，每 IP 1 秒 1 次，突发 30
    let global_governor = GovernorConfigBuilder::default()
        .per_second(1)
        .burst_size(30)
        .finish()
        .expect("global rate limit config invalid");

    // 用户认证路由（公开：注册 / 登录）+ 严格限流
    let user_public = Router::new()
        .route(
            "/register",
            axum::routing::post(culino_user::handler::register),
        )
        .route("/login", axum::routing::post(culino_user::handler::login))
        .layer(GovernorLayer {
            config: std::sync::Arc::new(auth_governor),
        });

    // 用户其他路由（需登录）
    let user_protected = Router::new()
        .route(
            "/me",
            axum::routing::get(culino_user::handler::me).put(culino_user::handler::update_profile),
        )
        .route(
            "/me/stats",
            axum::routing::get(culino_user::handler::me_stats),
        )
        .route("/logout", axum::routing::post(culino_user::handler::logout))
        .route(
            "/invite-codes",
            axum::routing::get(culino_user::handler::list_invite_codes)
                .post(culino_user::handler::create_invite_code),
        )
        .route(
            "/invite-codes/{code}",
            axum::routing::delete(culino_user::handler::revoke_invite_code),
        );

    // v1 下受保护的路由（统一挂 auth 中间件 + 全局限流）
    let v1_protected = Router::new()
        .nest("/user", user_protected)
        .nest("/ingredient", culino_ingredient::routes())
        .nest("/recipe", culino_recipe::routes())
        .nest("/social", culino_social::routes())
        .nest("/tool", culino_tool::routes())
        .nest("/upload", culino_upload::routes())
        .nest(
            "/ai",
            ai_routes().layer(GovernorLayer {
                config: std::sync::Arc::new(ai_governor),
            }),
        )
        .layer(GovernorLayer {
            config: std::sync::Arc::new(global_governor),
        })
        .layer(axum::middleware::from_fn_with_state(
            state.clone(),
            culino_common::auth::require_auth_middleware,
        ));

    // v1 合并公开和受保护
    let v1 = Router::new().nest("/user", user_public).merge(v1_protected);

    let mut app = Router::new()
        .route("/health", axum::routing::get(health))
        .nest("/api/v1", v1);

    // 仅开发环境挂载 Swagger UI
    if state.config.run_mode != RunMode::Production {
        app = app.merge(SwaggerUi::new("/swagger-ui").url("/api-docs/openapi.json", doc));
    }

    app.layer(PropagateRequestIdLayer::new(x_request_id.clone()))
        .layer(SetRequestIdLayer::new(x_request_id, MakeRequestUuid))
        .layer(build_cors(&state.config))
        .layer(DefaultBodyLimit::max(1024 * 1024)) // 全局 1MB，upload 路由单独覆盖为 16MB
        .layer(TimeoutLayer::with_status_code(
            StatusCode::REQUEST_TIMEOUT,
            Duration::from_secs(30),
        ))
        .layer(TraceLayer::new_for_http())
        .with_state(state)
}
