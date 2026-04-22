# menu-upload

上传模块，提供图片上传到 S3 兼容对象存储（RustFS/MinIO）的功能。

## API

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/v1/upload/image` | 上传图片（multipart，≤5MB，image/*） | 是 |
| DELETE | `/api/v1/upload/image` | 删除已上传的图片 | 是 |

## 架构

```
handler.rs  →  common/s3.rs (S3 客户端封装)
```

无 Repo/Service 层，Handler 直接调用 common 中的 S3 工具函数。

上传后的文件按 `images/{date}/{uuid}.{ext}` 格式存储。
