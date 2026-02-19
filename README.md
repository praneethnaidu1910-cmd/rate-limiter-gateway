# API Rate Limiter with Redis

A distributed rate limiting service built with Spring Boot and Redis, implementing token bucket algorithm to protect APIs from abuse and ensure fair resource allocation across clients.

## 🎯 Project Overview

This application demonstrates a production-ready approach to API rate limiting using Redis as a centralized counter store. It enforces configurable request quotas per client, automatically resets time windows, and provides detailed rate limit feedback through RESTful endpoints.

**Key Feature:** Rate limit state is shared and persists across application restarts, making it suitable for distributed deployment scenarios.

## 🏗️ Architecture
```
Client Request → Spring Boot API → Redis Counter → Allow/Deny Response
                      ↓
              Rate Limit Logic
              - Check counter
              - Enforce limit
              - Update counter
```

**Design Decisions:**
- **Redis for state management:** Centralized counter ensures consistency across potential multiple application instances
- **TTL-based expiration:** Automatic cleanup without manual intervention, counters reset after time window expires
- **Token bucket algorithm:** Fixed window implementation with configurable limits and time periods
- **RESTful API design:** Standard HTTP status codes (200, 429) for clear client communication

## ⚙️ Technical Stack

- **Backend:** Java 17, Spring Boot 3.2.x
- **Cache/State Store:** Redis 7 (Alpine)
- **Build Tool:** Maven
- **Containerization:** Docker, Docker Compose
- **Libraries:** Spring Web, Spring Data Redis, Lombok

## 🚀 Quick Start

### Prerequisites

- Java 17 or higher
- Docker and Docker Compose
- Maven (or use included wrapper)

### Installation & Setup

1. **Clone the repository**
```bash
   git clone https://github.com/praneethnaidu/rate-limiter-gateway.git
   cd rate-limiter-gateway
```

2. **Start Redis container**
```bash
   docker-compose up -d
```

3. **Run the application**
```bash
   ./mvnw spring-boot:run
```

4. **Verify it's running**
```bash
   curl http://localhost:8080/api/test?clientId=user123
```

The application will start on `http://localhost:8080`

## 📡 API Endpoints

### 1. Test Rate Limiting
```http
GET /api/test?clientId={clientId}
```

**Response (Success - 200 OK):**
```json
{
  "message": "Request successful",
  "clientId": "user123",
  "remaining": 9,
  "resetIn": "60 seconds",
  "timestamp": "2025-01-15T10:30:00"
}
```

**Response (Rate Limited - 429 Too Many Requests):**
```json
{
  "error": "Rate limit exceeded",
  "message": "Too many requests. Please try again later.",
  "clientId": "user123",
  "remaining": 0,
  "resetIn": "45 seconds",
  "timestamp": "2025-01-15T10:30:15"
}
```

### 2. Check Rate Limit Status
```http
GET /api/status?clientId={clientId}
```

**Response:**
```json
{
  "clientId": "user123",
  "remaining": 5,
  "resetIn": "30 seconds",
  "timestamp": "2025-01-15T10:30:30"
}
```

**Note:** Status endpoint does not consume a request from the quota.

## ⚙️ Configuration

Edit `src/main/resources/application.yml`:
```yaml
rate-limiter:
  default-limit: 10      # Maximum requests allowed
  window-seconds: 60     # Time window in seconds

spring:
  redis:
    host: localhost      # Redis host
    port: 6379          # Redis port
    timeout: 2000ms     # Connection timeout

server:
  port: 8080           # Application port
```

## 🧪 Testing

### Manual Testing with curl

**Test basic rate limiting:**
```bash
# Make 10 requests (should succeed)
for i in {1..10}; do
  curl "http://localhost:8080/api/test?clientId=testuser"
  echo ""
done

# Make 11th request (should fail with 429)
curl "http://localhost:8080/api/test?clientId=testuser"
```

**Test different clients:**
```bash
# User1 reaches limit
for i in {1..10}; do curl "http://localhost:8080/api/test?clientId=user1"; done

# User2 should still work
curl "http://localhost:8080/api/test?clientId=user2"
```

**Test window reset:**
```bash
# Exceed limit
curl "http://localhost:8080/api/test?clientId=user3"  # (repeat 10 times)

# Wait 60 seconds
sleep 60

# Should work again
curl "http://localhost:8080/api/test?clientId=user3"
```

### Verify Redis State
```bash
# Connect to Redis CLI
docker exec -it rate-limiter-redis redis-cli

# Check existing rate limit keys
127.0.0.1:6379> KEYS rate_limit:*

# View specific counter
127.0.0.1:6379> GET rate_limit:user123

# Check time remaining until reset
127.0.0.1:6379> TTL rate_limit:user123
```

## 📊 How It Works

### Rate Limiting Flow

1. **Request arrives** with `clientId` parameter
2. **Check Redis** for key `rate_limit:{clientId}`
3. **If counter < limit:**
   - Increment counter (or create if first request)
   - Set TTL to window duration
   - Return success (200)
4. **If counter >= limit:**
   - Return rate limit exceeded (429)
5. **After TTL expires:**
   - Redis automatically deletes key
   - Next request starts fresh counter

### Redis Data Structure
```
Key: rate_limit:user123
Value: "5"
TTL: 45 seconds

Key: rate_limit:user456
Value: "10"
TTL: 12 seconds
```

Each client gets an isolated counter with automatic expiration.

## 🐳 Docker Configuration

### docker-compose.yml
```yaml
version: '3.8'
services:
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    command: redis-server --appendonly yes

volumes:
  redis-data:
```

**Features:**
- Redis 7 on Alpine Linux (lightweight)
- AOF persistence enabled (append-only file)
- Data persists across container restarts
- Port 6379 exposed to host

## 🔧 Project Structure
```
rate-limiter/
├── src/
│   ├── main/
│   │   ├── java/com/praneeth/ratelimiter/
│   │   │   ├── RateLimiterApplication.java     # Main entry point
│   │   │   ├── config/
│   │   │   │   └── RedisConfig.java            # Redis configuration
│   │   │   ├── service/
│   │   │   │   └── RateLimiterService.java     # Core rate limiting logic
│   │   │   └── controller/
│   │   │       └── TestController.java         # REST API endpoints
│   │   └── resources/
│   │       └── application.yml                 # Application configuration
├── docker-compose.yml                          # Redis container setup
├── pom.xml                                     # Maven dependencies
└── README.md
```

## 🚦 Current Status & Roadmap

### ✅ Implemented (MVP)
- Token bucket rate limiting with fixed time windows
- Redis-based distributed counter with TTL
- RESTful API with proper HTTP status codes
- Docker containerization for Redis
- Configurable limits and time windows
- Rate limit status endpoint

### 🔄 Planned Enhancements (Phase 2)
- **Horizontal scaling:** Multiple Spring Boot instances with Nginx load balancing
- **Atomic operations:** Lua scripts to eliminate race conditions under concurrent load
- **Monitoring:** Prometheus metrics + Grafana dashboards for observability
- **Authentication:** JWT-based client identification
- **Tiered limits:** Support for multiple quota tiers (free, pro, enterprise)
- **AWS deployment:** EC2, ElastiCache, Application Load Balancer

### 🐛 Known Limitations
- **Race condition:** Under high concurrency, counter may exceed limit by 1-2 requests (will fix with Lua scripts)
- **Single instance:** Not yet tested with multiple application instances (Phase 2)
- **No authentication:** Client ID passed as query param (Phase 2: JWT tokens)



## 📚 Learning Resources

**Rate Limiting Algorithms:**
- Token Bucket vs Leaky Bucket vs Fixed Window
- Sliding Window Log algorithm

**Redis Concepts:**
- TTL (Time To Live) and key expiration
- Atomic operations with Lua scripting
- Redis persistence (RDB vs AOF)

**Distributed Systems:**
- Race conditions in concurrent environments
- CAP theorem and consistency models
- Horizontal scaling strategies

## 👤 Author

**Praneeth Naidu**
- GitHub: [@praneethnaidu](https://github.com/praneethnaidu1910-cmd)
- LinkedIn: [praneeth-naidu](https://www.linkedin.com/in/praneeth-naidu-6a732a241)
- Email: pnaidu@gmu.edu

## 📄 License

This project is open source and available for educational purposes.

---

**Built as part of a learning project to understand distributed rate limiting and Redis-based state management in Spring Boot applications.**
