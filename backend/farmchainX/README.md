# 🚜 FarmChainX Backend

The powerful backend API engine for **FarmChainX**, built with **Spring Boot 3.5** and **Java 21**. It orchestrates the entire supply chain logic, handles secure authentication, manages data persistence, and integrates with AI services.

## 🌟 Capabilities

- **Secure Authentication**: Robust JWT-based authentication with Role-Based Access Control (RBAC) (Farmer, Distributor, Retailer, Consumer, Admin).
- **Supply Chain Management**: End-to-end logic for product creation, procurement, inventory management, and order fulfillment.
- **Traceability**: API endpoints to fetch complete product history and provenance data.
- **Image Processing**: Integration with **Cloudinary** for secure storage of product images and AI analysis results.
- **QR Code Services**: Server-side logic for verifying and generating product codes.
- **Documentation**: Built-in Swagger/OpenAPI UI for easy API testing and exploration.

## 🔧 Technology Stack

- **Language**: Java 21 LTS
- **Framework**: Spring Boot 3.5.6 (Snapshot)
- **Database**: MySQL 8.0
- **ORM**: Hibernate / Spring Data JPA
- **Security**: Spring Security + JWT (jjwt)
- **Utilities**: Lombok, Maven, Dotenv

## 🚀 Getting Started

### Prerequisites
- Java Development Kit (JDK) 21
- MySQL Server running locally on port 3306 (default configuration)
- Maven (bundled `mvnw` included)

### Configuration
Update `src/main/resources/application.properties` with your database credentials:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/farmchainx_db?useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=your_password
```

### Running the Application
1. **Build the project**:
   ```bash
   ./mvnw clean install
   ```
2. **Run the server**:
   ```bash
   ./mvnw spring-boot:run
   ```
   The API will be available at `http://localhost:8080`.

## 📚 API Documentation

Once the server is running, access the interactive API definition via Swagger UI:
- **Swagger UI**: `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

## 🏗️ Architecture

- **Controller Layer**: REST endpoints handling HTTP requests.
- **Service Layer**: Business logic and transaction management.
- **Repository Layer**: Data access using Spring Data JPA interfaces.
- **Entity Layer**: JPA entities representing database tables (Product, User, Batch, Shipment).

---
*Powered by Spring Boot & Java 21*
