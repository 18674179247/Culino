//! 路由构建
//!
//! 挂载 Swagger UI 和所有业务模块，添加中间件层。

use axum::extract::DefaultBodyLimit;
use axum::http::{HeaderName, Method, StatusCode};
use axum::response::Json;
use axum::Router;
use menu_common::config::{AppConfig, RunMode};
use menu_common::state::AppState;
use serde_json::json;
use std::time::Duration;
use tower_governor::governor::GovernorConfigBuilder;
use tower_governor::GovernorLayer;
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
        .route("/nutrition/analyze/:recipe_id", axum::routing::post(menu_ai::analyze_nutrition))
        .route("/nutrition/:recipe_id", axum::routing::get(menu_ai::get_nutrition))
        // 推荐系统
        .route("/recommend/personalized", axum::routing::get(menu_ai::personalized_recommendations))
        .route("/recommend/similar/:recipe_id", axum::routing::get(menu_ai::similar_recommendations))
        .route("/recommend/trending", axum::routing::get(menu_ai::trending_recommendations))
        .route("/recommend/health/:goal", axum::routing::get(menu_ai::health_goal_recommendations))
        // 用户偏好
        .route("/preference/analyze", axum::routing::post(menu_ai::analyze_preference))
        .route("/preference/profile", axum::routing::get(menu_ai::get_preference_profile))
        // 行为日志
        .route("/behavior/log", axum::routing::post(menu_ai::log_behavior))
        .layer(TimeoutLayer::with_status_code(StatusCode::REQUEST_TIMEOUT, Duration::from_secs(120)))
}

/// 构建 CORS 层，根据配置决定允许的 Origin
fn build_cors(config: &AppConfig) -> CorsLayer {
    let cors = CorsLayer::new()
        .allow_methods([Method::GET, Method::POST, Method::PUT, Method::DELETE, Method::OPTIONS])
        .allow_headers([
            axum::http::header::AUTHORIZATION,
            axum::http::header::CONTENT_TYPE,
        ]);

    if config.cors_origins.is_empty() {
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

    // 限流配置：每个 IP 每秒 5 次请求，突发最多 10 次
    let rate_limit_config = GovernorConfigBuilder::default()
        .per_second(5)
        .burst_size(10)
        .finish()
        .unwrap();

    // 用户认证路由单独挂限流
    let user_auth = Router::new()
        .route("/register", axum::routing::post(menu_user::handler::register))
        .route("/login", axum::routing::post(menu_user::handler::login))
        .layer(GovernorLayer { config: rate_limit_config.into() });

    // 用户其他路由（不限流）
    let user_other = Router::new()
        .route("/me", axum::routing::get(menu_user::handler::me).put(menu_user::handler::update_profile))
        .route("/logout", axum::routing::post(menu_user::handler::logout));

    // v1 API 路由
    let v1 = Router::new()
        .nest("/user", user_auth.merge(user_other))
        .nest("/ingredient", menu_ingredient::routes())
        .nest("/recipe", menu_recipe::routes())
        .nest("/social", menu_social::routes())
        .nest("/tool", menu_tool::routes())
        .nest("/upload", menu_upload::routes())
        .nest("/ai", ai_routes());

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
        .layer(TimeoutLayer::with_status_code(StatusCode::REQUEST_TIMEOUT, Duration::from_secs(30)))
        .layer(TraceLayer::new_for_http())
        .with_state(state)
}
