# 🌾 FarmChainX
### *The Future of Transparent Agriculture Supply Chain*

![Status](https://img.shields.io/badge/Status-Active_Development-success?style=for-the-badge)
![Tech](https://img.shields.io/badge/Stack-Angular_20_%2B_Spring_Boot_3.5-blue?style=for-the-badge)

**FarmChainX** is a next-generation supply chain platform that leverages **Blockchain transparency** and **AI-driven insights** to connect farmers, distributors, retailers, and consumers. By digitizing the journey from *seed to shelf*, we ensure fair pricing for farmers, optimized logistics for distributors, and verified quality for consumers.

---

## 🚀 Key Features

| Role | Capabilities |
|------|--------------|
| **👨‍🌾 Farmer** | Register harvests, upload verified photos, get AI quality scores, and sell directly to the network. |
| **🚚 Distributor** | Real-time marketplace for procurement, inventory management, and logistics tracking. |
| **🏬 Retailer** | Stock verification, provenance checks, and detailed product history for shelf items. |
| **🛒 Consumer** | Scan QR codes to see the full journey, verify organic status, and view eco-impact data. |
| **🔎 Auditor** | System-wide oversight, quality grading verification, and fraud detection. |

---

## 🛠️ Technology Ecosystem

### **Frontend Client** ([`/frontend`](./frontend))
A premium, responsive Single Page Application (SPA).
- **Framework**: Angular 20 (Bleeding Edge)
- **Design System**: Tailwind CSS 4 + Lucide Icons
- **Key Modules**: Role-based Dashboards, Interactive Charts, QR Scanner.

### **Backend Core** ([`/backend/farmchainX`](./backend/farmchainX))
A robust, secure, and high-performance API layer.
- **Framework**: Spring Boot 3.5 & Java 21
- **Database**: MySQL 8.0
- **Security**: JWT Authentication & Spring Security
- **Integration**: Cloudinary (Media), ZXing (QR), AI Microservices.

---

## 🏁 Getting Started

To run the full stack locally, you will need to start both the backend server and the frontend client.

### 1. Database Setup
Ensure **MySQL** is running and create a database named `farmchainx_db`. Configure your credentials in `backend/farmchainX/src/main/resources/application.properties`.

### 2. Backend Startup
```bash
cd backend/farmchainX
./mvnw spring-boot:run
```
*Server runs on port `8080`.*

### 3. Frontend Startup
```bash
cd frontend
npm install
ng serve
```
*Client runs on port `4200`.*

---

## 🗺️ Roadmap & Status

- [x] **Core Architecture**: Spring Boot + Angular Setup.
- [x] **Authentication**: Secure Login/Register for all roles.
- [x] **Farmer Module**: Product upload & My Products view.
- [x] **Distributor/Retailer**: Marketplace & Inventory logic.
- [x] **Consumer Experience**: Public product verification page.
- [ ] **Blockchain Integration**: Migrating internal ledgers to Hyperledger/Ethereum.
- [ ] **Mobile App**: Native mobile wrapper for on-field usage.

---
**FarmChainX** — *Empowering Agriculture with Technology.*
