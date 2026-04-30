//! 用户模块
//!
//! 提供用户注册、登录、登出、个人信息管理等功能。
//!
//! 注意：用户路由在 `src/router.rs` 中手动构建（认证路由需要单独挂限流），
//! 因此本模块不提供 `routes()` 函数。

pub mod handler;
pub mod model;
pub mod repo;

use utoipa::OpenApi;

#[derive(OpenApi)]
#[openapi(
    paths(
        handler::register,
        handler::login,
        handler::logout,
        handler::me,
        handler::update_profile,
    ),
    components(schemas(
        model::LoginReq,
        model::RegisterReq,
        model::UpdateProfileReq,
        model::UserResponse,
        model::TokenResponse,
    ))
)]
pub struct UserApi;
