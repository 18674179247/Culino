//! OpenAPI 文档构建
//!
//! 定义根级文档、安全方案，并合并所有子模块的文档。

use utoipa::OpenApi;

/// 根级 OpenAPI 文档定义，用于合并各子模块的文档
#[derive(OpenApi)]
#[openapi(
    info(title = "Menu Backend API", version = "0.1.0"),
    security(("bearer" = [])),
    modifiers(&SecurityAddon),
)]
struct ApiDoc;

/// 安全方案修改器，为 OpenAPI 文档添加 Bearer Token 认证
struct SecurityAddon;

impl utoipa::Modify for SecurityAddon {
    fn modify(&self, openapi: &mut utoipa::openapi::OpenApi) {
        let components = openapi.components.get_or_insert_with(Default::default);
        components.add_security_scheme(
            "bearer",
            utoipa::openapi::security::SecurityScheme::Http(
                utoipa::openapi::security::Http::new(
                    utoipa::openapi::security::HttpAuthScheme::Bearer,
                ),
            ),
        );
    }
}

/// 构建完整的 OpenAPI 文档，合并所有子模块
pub fn build_api_doc() -> utoipa::openapi::OpenApi {
    let mut doc = ApiDoc::openapi();
    doc.merge(menu_user::UserApi::openapi());
    doc.merge(menu_ingredient::IngredientApi::openapi());
    doc.merge(menu_recipe::RecipeApi::openapi());
    doc.merge(menu_social::SocialApi::openapi());
    doc.merge(menu_tool::ToolApi::openapi());
    doc.merge(menu_upload::UploadApi::openapi());
    doc.merge(menu_ai::AiApi::openapi());
    doc
}
