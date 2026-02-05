# FinPay Platform

A modern financial payment platform built with microservices architecture using Spring Boot, MySQL, and Apache Kafka.

## Architecture Overview

```
                                    ┌─────────────────┐
                                    │   Frontend      │
                                    │   (React/Vite)  │
                                    └────────┬────────┘
                                             │
                                    ┌────────▼────────┐
                                    │   API Gateway   │
                                    │   (Port: 8080)  │
                                    └────────┬────────┘
                                             │
                    ┌────────────────────────┼────────────────────────┐
                    │                        │                        │
           ┌────────▼────────┐     ┌────────▼────────┐     ┌────────▼────────┐
           │  User Service   │     │ Payment Service │     │ Notification    │
           │  (Port: 8081)   │     │  (Port: 8082)   │     │ Service (8083)  │
           └────────┬────────┘     └────────┬────────┘     └────────┬────────┘
                    │                        │                        │
                    │              ┌─────────▼─────────┐              │
                    │              │   Apache Kafka    │◄─────────────┤
                    │              │   (Port: 9092)    │              │
                    │              └───────────────────┘              │
                    │                                                 │
           ┌────────▼────────────────────────────────────────────────▼────────┐
           │                         MySQL Database                           │
           │   finpay_users (3306) │ finpay_payments (3306) │ finpay_notif   │
           └──────────────────────────────────────────────────────────────────┘
                                             │
                                    ┌────────▼────────┐
                                    │ Service Registry│
                                    │ Eureka (8761)   │
                                    └─────────────────┘
```

## Services

| Service | Port | Description |
|---------|------|-------------|
| Service Registry | 8761 | Eureka Server for service discovery |
| API Gateway | 8080 | Spring Cloud Gateway for routing |
| User Service | 8081 | User management and authentication |
| Payment Service | 8082 | Payment processing and transactions |
| Notification Service | 8083 | Email, SMS, and push notifications |

## Technology Stack

- **Backend**: Spring Boot 4.0.2, Spring Cloud 2025.1.0
- **Database**: MySQL 8.0
- **Messaging**: Apache Kafka
- **Service Discovery**: Netflix Eureka
- **API Gateway**: Spring Cloud Gateway
- **Frontend**: React with TypeScript, Vite, Tailwind CSS
- **Java Version**: 25

## Prerequisites

- Java 25+
- Maven 3.9+
- Docker & Docker Compose
- Node.js 20+ (for frontend)

## Quick Start

### 1. Start Infrastructure Services

```bash
# Start MySQL, Kafka, and Zookeeper
docker-compose up -d
```

### 2. Start Backend Services

Start services in this order:

```bash
# Terminal 1 - Service Registry (start first)
cd backend/service-registry
mvn spring-boot:run

# Terminal 2 - API Gateway
cd backend/api-gateway
mvn spring-boot:run

# Terminal 3 - User Service
cd backend/user-service
mvn spring-boot:run

# Terminal 4 - Payment Service
cd backend/payment-service
mvn spring-boot:run

# Terminal 5 - Notification Service
cd backend/notification-service
mvn spring-boot:run
```

### 3. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

## API Endpoints

### User Service (`/api/v1/users`)
- `POST /` - Create user
- `GET /{id}` - Get user by ID
- `GET /email/{email}` - Get user by email
- `PUT /{id}` - Update user
- `DELETE /{id}` - Delete user
- `PATCH /{id}/status` - Update user status
- `POST /{id}/verify-email` - Verify email

### Payment Service (`/api/v1/payments`)
- `POST /` - Initiate payment
- `GET /{id}` - Get payment by ID
- `GET /reference/{ref}` - Get payment by reference
- `GET /user/{userId}` - Get user payments
- `POST /{id}/cancel` - Cancel payment
- `POST /{id}/refund` - Refund payment

### Notification Service (`/api/v1/notifications`)
- `POST /` - Create notification
- `GET /user/{userId}` - Get user notifications
- `GET /user/{userId}/unread` - Get unread notifications
- `POST /{id}/read` - Mark as read
- `GET /user/{userId}/preferences` - Get preferences
- `PUT /user/{userId}/preferences` - Update preferences

## Kafka Topics

| Topic | Producer | Consumer | Description |
|-------|----------|----------|-------------|
| `user-events` | User Service | Notification Service | User lifecycle events |
| `payment-events` | Payment Service | Notification Service | Payment status events |

## Configuration

### Environment Variables

```bash
# MySQL
MYSQL_USERNAME=root
MYSQL_PASSWORD=root

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Mail (for notifications)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email
MAIL_PASSWORD=your-password
MAIL_ENABLED=true
```

## Monitoring

- **Eureka Dashboard**: http://localhost:8761
- **Kafka UI**: http://localhost:8090 (when using docker-compose)
- **Actuator Endpoints**: `/actuator/health`, `/actuator/info`

## Development

### Building All Services

```bash
cd backend
for dir in service-registry api-gateway user-service payment-service notification-service; do
  cd $dir && mvn clean install && cd ..
done
```

### Running Tests

```bash
cd backend/user-service
mvn test
```

## License

MIT License
