# 💼 Personal Finance Dashboard

A full-stack microservices application for managing personal finances with account management, transaction tracking, and analytics visualization.

## 🎥 Demo Video

[Watch Demo](https://youtube.com/link-here) _(Coming soon)_

## ✨ Features

- **User Authentication** - Secure JWT-based registration and login
- **Account Management** - Create up to 3 accounts (Savings, Checking, Credit)
- **Transaction Tracking** - Real-time money transfers with validation
- **Analytics Dashboard** - Visualize monthly spending and income with charts
- **Customer Profile** - View and edit personal information
- **Event-Driven Architecture** - Asynchronous processing via Apache Kafka

## 🛠️ Tech Stack

### Backend

- **Framework:** Spring Boot 3.5
- **Security:** Spring Security with JWT (RSA signing)
- **Communication:**
    - REST APIs (Spring Web)
    - gRPC (inter-service validation)
    - Apache Kafka (event streaming)
- **Databases:** PostgreSQL (5 separate databases - one per service)
- **API Gateway:** Spring Cloud Gateway
- **Caching:** Redis
- **Containerization:** Docker & Docker Compose

### Frontend

- **Framework:** React 18
- **Routing:** React Router v6
- **HTTP Client:** Axios
- **Charts:** Recharts
- **Styling:** CSS3

### Architecture

- Microservices (6 independent services)
- Event-Driven Architecture
- Service-to-Service Communication (REST + gRPC)
- Centralized API Gateway
- Database per Service pattern

## 🏗️ Architecture

### Microservices Overview

```
┌─────────────┐
│   Frontend  │
│   (React)   │
└──────┬──────┘
       │
       ▼
┌─────────────────────┐
│   API Gateway       │
│   (Port 9090)       │
└──────┬──────────────┘
       │
       ├──────────────────────────────────────┐
       │                                      │
       ▼                                      ▼
┌──────────────┐                     ┌──────────────┐
│ Auth Service │◄────── Kafka ──────►│Transaction   │
│ (Port 8085)  │                     │Service       │
└──────────────┘                     │(Port 8081)   │
       │                             └──────┬───────┘
       │ Kafka                              │
       ▼                                    │ gRPC
┌──────────────┐                            │
│  Customer    │                            ▼
│  Service     │                     ┌──────────────┐
│ (Port 8084)  │                     │Account       │
└──────────────┘                     │Service       │
       │                             │(Port 8082)   │
       │ Kafka                       └──────────────┘
       ▼                                    │
┌──────────────┐                            │ Kafka
│Analytics     │◄───────────────────────────┘
│Service       │
│(Port 8083)   │
└──────────────┘
```

### Services Description

| Service                 | Port | Database       | Responsibility                       |
| ----------------------- | ---- | -------------- | ------------------------------------ |
| **API Gateway**         | 9090 | Redis          | Routes requests, rate limiting       |
| **Auth Service**        | 8085 | auth_db        | User authentication & JWT management |
| **Customer Service**    | 8084 | customer_db    | Customer profile management          |
| **Account Service**     | 8082 | account_db     | Bank account CRUD operations         |
| **Transaction Service** | 8081 | transaction_db | Money transfers & validation         |
| **Analytics Service**   | 8083 | analytics_db   | Monthly spending/income tracking     |

### Communication Patterns

- **REST:** Frontend ↔ API Gateway ↔ Services
- **gRPC:** Transaction Service ↔ Account Service (balance validation)
- **Kafka:** Event streaming for async operations (auth → customer, transaction → analytics)

## 🚀 Installation & Setup

### Prerequisites

- Java 21
- Node.js 18+
- Docker & Docker Compose
- Maven

### 1. Clone the Repository

```bash
git clone https://github.com/greatadams/finance-dashboard.git

cd finance-dashboard
```

### 2. Start Infrastructure (Databases, Kafka, Redis)

```bash
docker-compose up -d
```

Wait ~30 seconds for services to be healthy.

### 3. Start Backend Services

**Terminal 1 - Auth Service:**

```bash
cd auth-service
mvn spring-boot:run
```

**Terminal 2 - Customer Service:**

```bash
cd customer-service
mvn spring-boot:run
```

**Terminal 3 - Account Service:**

```bash
cd account-service
mvn spring-boot:run
```

**Terminal 4 - Transaction Service:**

```bash
cd transaction-service
mvn spring-boot:run
```

**Terminal 5 - Analytics Service:**

```bash
cd analytics-service
mvn spring-boot:run
```

**Terminal 6 - API Gateway:**

```bash
cd api-gateway
mvn spring-boot:run
```

### 4. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

### 5. Access Application

- **Frontend:** http://localhost:5173
- **API Gateway:** http://localhost:9090
- **Kafka UI:** http://localhost:8090

### Default Test Account (Optional)

After starting services, register a new account through the UI.

## 📸 Screenshots

_(Add screenshots after recording demo video)_

## 🔑 Key Technical Highlights

- **gRPC Integration:** Real-time account balance validation during transactions
- **Event-Driven:** Kafka processes 100% of auth and transaction events asynchronously
- **Business Logic:** Enforces account limits (max 1 Savings, 1 Checking, 1 Credit per user)
- **Security:** JWT with RSA-512 signing, role-based access control
- **Idempotency:** Prevents duplicate transactions using unique keys
- **Database Isolation:** Each microservice has its own database (no shared data)

## 🎯 Future Enhancements

- [ ] Recurring transactions & scheduled payments
- [ ] Budget planning with spending limits
- [ ] Email notifications for transactions
- [ ] Multi-currency support
- [ ] Mobile app (React Native)
- [ ] Export transactions to CSV/PDF
- [ ] Bill reminders & payment tracking
- [ ] Investment portfolio tracking

## 👤 Author

**Your Name**

- LinkedIn: [great adamu](https://www.linkedin.com/in/great-adamu/)
- GitHub: [great adams](https://github.com/greatadams)
- Email: adamsgreat15@gmail.com

## 📄 License

This project is for educational purposes.

---

**⭐ If you found this project helpful, please give it a star!**
