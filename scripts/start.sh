#!/bin/bash

# ================================================================
# CashBee Application Start Script
# ================================================================
# Purpose: Build and run the CashBee backend application
# Usage: ./scripts/start.sh
# ================================================================

set -e  # Exit on error

echo "======================================"
echo "  CashBee Backend - Start Script"
echo "======================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check if Maven wrapper exists
if [ ! -f "./mvnw" ]; then
    echo -e "${RED}Error: Maven wrapper (mvnw) not found!${NC}"
    exit 1
fi

# Check Java version
echo -e "${YELLOW}Checking Java version...${NC}"
if ! command -v java &> /dev/null; then
    echo -e "${RED}Error: Java not found! Please install Java 17+${NC}"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo -e "${RED}Error: Java 17+ required, found version $JAVA_VERSION${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Java version OK${NC}"
echo ""

# Check MySQL connection
echo -e "${YELLOW}Checking MySQL connection...${NC}"
if command -v mysql &> /dev/null; then
    if mysql -h localhost -u cashbee_user -pcashbee_password -e "USE cashbee;" 2>/dev/null; then
        echo -e "${GREEN}✓ Database connection OK${NC}"
    else
        echo -e "${RED}Warning: Cannot connect to database${NC}"
        echo -e "${YELLOW}Please run: mysql -u root -p < scripts/setup-database.sql${NC}"
    fi
else
    echo -e "${YELLOW}MySQL client not found - skipping database check${NC}"
fi
echo ""

# Build application
echo -e "${YELLOW}Building application...${NC}"
./mvnw clean install -DskipTests

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Build successful${NC}"
else
    echo -e "${RED}✗ Build failed${NC}"
    exit 1
fi
echo ""

# Run application
echo -e "${YELLOW}Starting application...${NC}"
echo -e "${GREEN}Access points:${NC}"
echo "  - API:         http://localhost:8080"
echo "  - Swagger UI:  http://localhost:8080/swagger-ui.html"
echo "  - Health:      http://localhost:8080/actuator/health"
echo ""
echo -e "${YELLOW}Press Ctrl+C to stop${NC}"
echo ""

./mvnw spring-boot:run -pl cashbee-presentation
