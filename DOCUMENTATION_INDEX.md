# 📚 Documentation Index

## 📂 New Organized Structure

All documentation has been reorganized for easy navigation!

```
cashbee-backend/
├── 📁 docs/                              # All documentation
│   ├── 📄 README.md                      # Documentation index
│   │
│   ├── 📁 architecture/                  # Architecture & Design
│   │   ├── HEXAGONAL_ARCHITECTURE.md     # Clean Architecture guide
│   │   ├── PROJECT_SUMMARY.md            # Project overview
│   │   └── IMPLEMENTATION_SUMMARY.md     # Implementation details
│   │
│   ├── 📁 api/                           # API Documentation
│   │   ├── FRONTEND_API_DOCUMENTATION.md # Complete API docs
│   │   ├── FRONTEND_AFFILIATE_PLATFORM_API.md
│   │   └── FRONTEND_CASHBACK_POLICY_API.md
│   │
│   ├── 📁 database/                      # Database Documentation
│   │   └── cashbee-backend-database.md   # Schema and design
│   │
│   ├── 📁 setup/                         # Setup Guides
│   │   ├── DOCKER_SETUP.md               # Docker setup (MOVED HERE!)
│   │   └── KEYCLOAK_SETUP.md             # Auth setup
│   │
│   ├── 📁 guides/                        # Development Guides
│   │   ├── TESTING_GUIDE.md              # Testing guide
│   │   └── CONTRIBUTING.md               # Contributing guide
│   │
│   ├── 📁 fixes/                         # Bug Fixes & Troubleshooting
│   │   ├── QUICK_FIX_SUMMARY.md          # Quick reference (MOVED HERE!)
│   │   ├── FIX_TRACKING_CODE_NULL_ERROR.md
│   │   └── FIX_AFFILIATE_ID_ERROR.md
│   │
│   └── 📋 Planning & Progress
│       ├── IMPLEMENTATION_PLAN.md
│       ├── TODO.md
│       ├── QUICK_TODO.md
│       ├── MVP_MISSING_FEATURES.md
│       └── PHASE_3_PROGRESS_SUMMARY.md
│
├── 📁 docker/                            # Docker Configuration
│   ├── 📄 README.md                      # Docker docs
│   ├── 🐳 Dockerfile                     # Build file (MOVED HERE!)
│   ├── .dockerignore                     # Ignore file (MOVED HERE!)
│   └── 📁 mysql/
│       └── init/                         # MySQL init scripts
│
├── 🐳 docker-compose.yml                 # Compose file (STAYS AT ROOT)
├── .env.example                          # Env template (STAYS AT ROOT)
├── 📄 README.md                          # Main README
└── 📄 DOCUMENTATION_INDEX.md             # This file
```

## 🚀 Quick Access

### 🆕 Just Starting?
1. **[docs/README.md](docs/README.md)** - Start here for all documentation
2. **[docs/architecture/PROJECT_SUMMARY.md](docs/architecture/PROJECT_SUMMARY.md)** - Project overview
3. **[docs/setup/DOCKER_SETUP.md](docs/setup/DOCKER_SETUP.md)** - Set up environment

### 🐳 Want to Run with Docker?
1. **[docker/README.md](docker/README.md)** - Docker overview
2. **[docs/setup/DOCKER_SETUP.md](docs/setup/DOCKER_SETUP.md)** - Complete guide
3. Run: `docker-compose up -d`

### 🔧 Fixing Issues?
1. **[docs/fixes/QUICK_FIX_SUMMARY.md](docs/fixes/QUICK_FIX_SUMMARY.md)** - Quick fixes
2. **[docs/fixes/](docs/fixes/)** - All fix documentation

### 💻 Developing Features?
1. **[docs/architecture/HEXAGONAL_ARCHITECTURE.md](docs/architecture/HEXAGONAL_ARCHITECTURE.md)** - Architecture
2. **[docs/guides/CONTRIBUTING.md](docs/guides/CONTRIBUTING.md)** - Contributing guide
3. **[docs/guides/TESTING_GUIDE.md](docs/guides/TESTING_GUIDE.md)** - Testing guide

### 🌐 Using the API?
1. **[docs/api/FRONTEND_API_DOCUMENTATION.md](docs/api/FRONTEND_API_DOCUMENTATION.md)** - Complete API docs
2. **Swagger UI:** http://localhost:8911/swagger-ui.html (when running)

## 🔍 Find Files By Category

### Architecture & Design
- [docs/architecture/](docs/architecture/)

### API Documentation
- [docs/api/](docs/api/)

### Database
- [docs/database/](docs/database/)

### Setup & Configuration
- [docs/setup/](docs/setup/)

### Development Guides
- [docs/guides/](docs/guides/)

### Bug Fixes
- [docs/fixes/](docs/fixes/)

### Docker
- [docker/](docker/)

## 📝 What Changed?

### ✅ Moved Files

| Old Location | New Location | Reason |
|--------------|--------------|--------|
| `Dockerfile` (root) | `docker/Dockerfile` | Better organization |
| `.dockerignore` (root) | `docker/.dockerignore` | Keep with Dockerfile |
| `DOCKER_SETUP.md` (root) | `docs/setup/DOCKER_SETUP.md` | Group setup docs |
| `QUICK_FIX_SUMMARY.md` (root) | `docs/fixes/QUICK_FIX_SUMMARY.md` | Group fix docs |
| All other `.md` files | `docs/` with categories | Easy navigation |

### ⚡ Stayed at Root

| File | Why |
|------|-----|
| `docker-compose.yml` | Docker Compose convention |
| `.env.example` | Environment variables convention |
| `README.md` | Project entry point |
| `pom.xml` | Maven project file |

### 🔄 Updated References

- ✅ `docker-compose.yml` → Updated `dockerfile: docker/Dockerfile`
- ✅ Port fixed → `8911:8080` (as requested)
- ✅ All internal links updated

## 🎯 Next Steps

1. **Read documentation:** Start with [docs/README.md](docs/README.md)
2. **Set up Docker:** Follow [docs/setup/DOCKER_SETUP.md](docs/setup/DOCKER_SETUP.md)
3. **Run the app:** `docker-compose up -d`
4. **Test API:** http://localhost:8911/swagger-ui.html

---

**Organization Complete!** ✅
**Last Updated:** 2025-11-02
