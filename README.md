<p align="center">
  <img src="assets/icon.svg" width="120" alt="Culino" />
</p>

<h1 align="center">Culino</h1>

<p align="center">
  AI-powered recipe management platform
  <br />
  <em>智能菜谱管理平台</em>
</p>

<p align="center">
  <a href="#features">Features</a> &bull;
  <a href="#tech-stack">Tech Stack</a> &bull;
  <a href="#getting-started">Getting Started</a> &bull;
  <a href="#architecture">Architecture</a> &bull;
  <a href="#license">License</a>
</p>

---

## Features

- **Recipe Management** — Create, edit, search, and get random recommendations
- **AI-Powered** — Nutrition analysis, personalized recommendations, recipe image recognition, shopping list parsing (DeepSeek API)
- **Ingredients & Seasonings** — Categorized browsing with tag management
- **Social** — Favorites, cooking records, likes, comments
- **Tools** — Shopping lists (batch add + AI text parsing), meal planning
- **Multi-Platform** — Android, iOS, Web (WASM)

## Tech Stack

### Backend (Rust)

| Component | Technology |
|-----------|-----------|
| Framework | Axum 0.8 |
| Database | PostgreSQL 16 + SQLx 0.8 |
| Cache | Redis 7 |
| Auth | JWT + Argon2 |
| Storage | S3-compatible (RustFS / MinIO) |
| AI | DeepSeek API |
| Docs | Utoipa + Swagger UI |

### Frontend (Kotlin Multiplatform)

| Component | Technology |
|-----------|-----------|
| UI | Compose Multiplatform 1.7 |
| Network | Ktor 3.1 |
| DI | kotlin-inject |
| Image | Coil 3 |
| Navigation | Navigation Compose |
| Storage | DataStore / localStorage |

## Getting Started

### Prerequisites

- Rust 1.86+
- Docker & Docker Compose
- JDK 17+
- Android Studio / Xcode (for mobile development)

### Backend

```bash
cd backend
docker compose up -d db redis rustfs
cp .env.example .env  # edit with your config
cargo run
# Swagger UI → http://localhost:3000/swagger-ui/
```

### Frontend

```bash
cd frontend

# Android
./gradlew :app:assembleDebug

# Web (WASM)
./gradlew :app:wasmJsBrowserDevelopmentRun

# iOS — open app/iosApp/iosApp.xcodeproj in Xcode
```

### One Command

```bash
./run-all.sh  # starts backend + frontend dev server
```

## Architecture

```
culino/
├── backend/                # Rust workspace
│   └── features/           # user, recipe, ingredient, social, tool, upload, ai
└── frontend/               # Kotlin Multiplatform
    ├── app/                # App shell (Android / iOS / Web entry points)
    └── src/
        ├── framework/      # Infrastructure (network, storage, media)
        ├── common/         # Shared (util, model, api, ui)
        └── feature/        # Business features (user, recipe, social, ingredient, tool)
```

Dependency flow: `feature → common → framework`

Each feature module follows Clean Architecture: `data → domain → presentation`

## API

All endpoints are prefixed with `/api/v1`. Full documentation available via Swagger UI when the backend is running.

| Module | Path | Description |
|--------|------|-------------|
| User | `/user` | Register, login, logout, profile |
| Recipe | `/recipe` | CRUD, search, random |
| Ingredient | `/ingredient` | Ingredients, seasonings, tags |
| Social | `/social` | Favorites, records, likes, comments |
| Tool | `/tool` | Shopping list, meal plan |
| Upload | `/upload` | Image upload/delete |
| AI | `/ai` | Nutrition, recommendations, recognition |

## License

[MIT](LICENSE) © 云山苍苍
