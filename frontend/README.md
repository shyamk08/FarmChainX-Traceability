# 🌾 FarmChainX Frontend

FarmChainX is a cutting-edge **Agricultural Supply Chain Management System** built with **Angular 20** and **Tailwind CSS**. It provides a premium, responsive user interface for tracking agricultural products from farm to fork, ensuring transparency, quality, and trust.

## 🚀 Features

### 👨‍🌾 Farmer Dashboard
- **Product Management**: Register and upload harvest details with images.
- **AI Quality Scoring**: View automated quality assessments for crops.
- **QR Code Generation**: Generate unique QR codes for product batches.
- **Sales Analytics**: Track sales and revenue with interactive charts.

### 🚚 Distributor & Retailer Portals
- **Marketplace**: Browse and procure fresh produce directly from farmers.
- **Inventory Management**: Real-time tracking of stock levels.
- **Supply Chain Flow**: Manage shipments, orders, and dispatch logistics.
- **Provenance Tracking**: Verify product origin and journey steps.

### 🛒 Consumer Interface
- **Traceability Timeline**: Scan QR codes or search IDs to see the full journey of a product.
- **Eco-Impact Specs**: View sustainability data and quality certifications.
- **Shopping Cart**: Purchase verified organic products.

### 🛡️ Admin & Auditor
- **System Logs**: Monitor system activities and security events.
- **User Management**: Manage role-based access control.
- **Verification**: Audit product batches and override quality scores if necessary.

## 🛠️ Tech Stack

- **Framework**: [Angular 20](https://angular.io/) (Bleeding Edge)
- **Styling**: [Tailwind CSS 4](https://tailwindcss.com/)
- **Icons**: [Lucide Angular](https://lucide.dev/)
- **Charts**: [ApexCharts](https://apexcharts.com/) & Chart.js
- **QR Scanning**: ZXing Browser
- **HTTP Client**: Axios based services (via Angular HTTP Client)

## 📦 Installation & Setup

1. **Prerequisites**: Ensure you have Node.js (v18+) and npm installed.
2. **Install Dependencies**:
   ```bash
   npm install
   ```
3. **Run Development Server**:
   ```bash
   ng serve
   ```
   Navigate to `http://localhost:4200/`. The app will verify connections to the backend API automatically.

## 🧪 Testing

- **Unit Tests**: `ng test`
- **E2E Tests**: `ng e2e` (configured via Cypress/Protractor if available)

## 📂 Project Structure

- `src/app/pages`: Contains modules for each role (Farmer, Distributor, Admin, etc.)
- `src/app/components`: Reusable UI components (Navbar, Cards, Modals).
- `src/app/services`: API integration services.
- `src/assets`: Static images and global styles.

---
*Generated for FarmChainX Project*
