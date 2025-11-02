# 🐳 Docker Configuration

Docker setup for CashBee Backend application.

## 📁 Folder Structure

```
docker/
├── README.md           # This file
├── Dockerfile          # Multi-stage build for CashBee Backend
├── .dockerignore      # Files to exclude from Docker build
└── mysql/
    └── init/          # MySQL initialization scripts (optional)
```

## 🚀 Quick Start

From the project root directory:

```bash
docker-compose up -d
```

## 📖 Documentation

See **[docs/setup/DOCKER_SETUP.md](../docs/setup/DOCKER_SETUP.md)** for complete Docker documentation including:

- Quick start guide
- Port configuration
- Docker commands reference
- Health checks
- Troubleshooting
- Environment variables
- Security notes

## 🎯 Services

| Service | Port | Container Name |
|---------|------|----------------|
| CashBee Backend | 8911 → 8080 | cashbee-backend |
| MySQL | 33067 → 3306 | cashbee-mysql |

## 📝 Files

### `Dockerfile`
Multi-stage Dockerfile for optimized Java 21 application:
- **Stage 1:** Maven build with dependency caching
- **Stage 2:** Lightweight runtime (Alpine + JRE)
- **Size:** ~350MB (optimized)
- **Features:** Non-root user, health checks, JVM tuning

### `.dockerignore`
Excludes unnecessary files from Docker build context:
- Build artifacts (`target/`, `build/`)
- IDE files (`.idea/`, `.vscode/`)
- Git files
- Logs
- Documentation

### `mysql/init/`
Optional MySQL initialization scripts. Add `.sql` files here to run on first database creation.

## 🔧 Customization

### Change Ports

Edit `docker-compose.yml` in project root:
```yaml
services:
  cashbee-backend:
    ports:
      - "8912:8080"  # Change host port
  mysql:
    ports:
      - "33068:3306"  # Change MySQL port
```

### Change JVM Settings

Edit `docker-compose.yml`:
```yaml
environment:
  JAVA_OPTS: -Xms1g -Xmx2g -XX:+UseG1GC
```

### Add Init Scripts

Create SQL files in `docker/mysql/init/`:
```bash
echo "INSERT INTO user (username, email) VALUES ('admin', 'admin@cashbee.vn');" > docker/mysql/init/01-seed-data.sql
```

Files are executed in alphabetical order.

## 🧪 Testing

```bash
# Build image
docker build -t cashbee-backend -f docker/Dockerfile .

# Test image
docker run -p 8911:8080 cashbee-backend

# Check health
curl http://localhost:8911/actuator/health
```

## 📚 Learn More

- **[Docker Documentation](https://docs.docker.com/)** - Official Docker docs
- **[Docker Compose](https://docs.docker.com/compose/)** - Compose reference
- **[Multi-stage Builds](https://docs.docker.com/build/building/multi-stage/)** - Build optimization

---

**See [docs/setup/DOCKER_SETUP.md](../docs/setup/DOCKER_SETUP.md) for complete documentation**
