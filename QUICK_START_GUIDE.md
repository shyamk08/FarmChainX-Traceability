# 🚀 FarmChainX - Quick Start Guide

Welcome! This guide will help you quickly get started with FarmChainX, whether you want to run it locally or deploy it to the cloud.

---

## Choose Your Path

### 🏠 [I want to run locally](#local-development)
Perfect for development, testing, and making changes to the codebase.

### ☁️ [I want to deploy to cloud](#cloud-deployment)
Perfect for making your application accessible online for real users.

---

## 🏠 Local Development

### Quick Setup (5-10 minutes)

**Prerequisites:**
- ✅ Java 21
- ✅ Node.js 18+
- ✅ MySQL 8.0+

**Steps:**

1. **Setup Database**
   ```sql
   mysql -u root -p
   CREATE DATABASE farmchainX;
   EXIT;
   ```

2. **Configure Backend**
   ```bash
   cd backend/farmchainX/src/main/resources
   # Create application.properties from example
   # Update MySQL password in the file
   ```

3. **Start Backend**
   ```bash
   cd backend/farmchainX
   ./mvnw spring-boot:run
   ```

4. **Start Frontend** (in a new terminal)
   ```bash
   cd frontend
   npm install
   npm start
   ```

5. **Open Application**
   - Visit: http://localhost:4200
   - Backend API: http://localhost:8080

📖 **Need detailed instructions?** See [LOCAL_SETUP_GUIDE.md](./LOCAL_SETUP_GUIDE.md)

---

## ☁️ Cloud Deployment

### Quick Deploy (20-30 minutes)

**Prerequisites:**
- ✅ GitHub account with your code pushed
- ✅ Render account (for backend + database)
- ✅ Vercel account (for frontend)

**Steps:**

1. **Deploy Database on Render**
   - New → PostgreSQL
   - Name: `farmchainx-db`
   - Copy the database URL

2. **Deploy Backend on Render**
   - New → Web Service
   - Connect GitHub repo
   - Root directory: `backend/farmchainX`
   - Build: `./mvnw clean install -DskipTests`
   - Start: `java -jar target/farmchainx-0.0.1-SNAPSHOT.jar`
   - Add environment variables (database URL, JWT secret, etc.)

3. **Deploy Frontend on Vercel**
   - Import GitHub repo
   - Framework: Angular
   - Root directory: `frontend`
   - Add environment variable: Backend URL
   - Deploy

4. **Configure CORS**
   - Update backend CORS config to include Vercel URL
   - Push changes to GitHub
   - Render auto-deploys

5. **Test Your App**
   - Visit your Vercel URL
   - Test registration, login, and features

📖 **Need detailed instructions?** See [DEPLOYMENT_GUIDE.md](./DEPLOYMENT_GUIDE.md)

---

## 📁 Project Structure

```
FarmChainX-master/
├── backend/
│   └── farmchainX/           # Spring Boot backend
│       ├── src/
│       ├── pom.xml
│       └── mvnw
├── frontend/                  # Angular frontend
│   ├── src/
│   ├── package.json
│   └── angular.json
├── DEPLOYMENT_GUIDE.md       # Detailed cloud deployment guide
├── LOCAL_SETUP_GUIDE.md      # Detailed local setup guide
└── README.md                 # Project overview
```

---

## 🎯 Common Tasks

### Run Tests

**Backend:**
```bash
cd backend/farmchainX
./mvnw test
```

**Frontend:**
```bash
cd frontend
npm test
```

### Build for Production

**Backend:**
```bash
cd backend/farmchainX
./mvnw clean package -DskipTests
```

**Frontend:**
```bash
cd frontend
npm run build
```

### Check API Documentation

When backend is running:
- Swagger UI: http://localhost:8080/swagger-ui/index.html

### Database Access

**Local MySQL:**
```bash
mysql -u root -p
USE farmchainX;
SHOW TABLES;
```

**Production PostgreSQL (Render):**
- Use the PSQL command from Render dashboard

---

## 🐛 Troubleshooting

### Backend won't start
- ✅ MySQL running? `mysql -u root -p`
- ✅ Correct password in `application.properties`?
- ✅ Java 21 installed? `java -version`

### Frontend shows errors
- ✅ Backend running on port 8080?
- ✅ Dependencies installed? `npm install`
- ✅ Node 18+? `node -v`

### API calls failing
- ✅ CORS configured correctly?
- ✅ Backend URL correct in environment files?
- ✅ Check browser console for errors

### Database connection issues
- ✅ Database exists? `SHOW DATABASES;`
- ✅ Credentials correct?
- ✅ Port 3306 (MySQL) or 5432 (PostgreSQL)?

---

## 📚 Resources

### Documentation
- [Full Local Setup Guide](./LOCAL_SETUP_GUIDE.md)
- [Full Deployment Guide](./DEPLOYMENT_GUIDE.md)
- [Spring Boot Docs](https://spring.io/projects/spring-boot)
- [Angular Docs](https://angular.io/docs)

### Tools
- [MySQL Workbench](https://dev.mysql.com/downloads/workbench/)
- [Postman](https://www.postman.com/)
- [VS Code](https://code.visualstudio.com/)

---

## 🎉 Next Steps

After successful setup:

1. **Create your first user** - Try registering as a Farmer
2. **Upload a product** - Test the core workflow
3. **Explore different roles** - Distributor, Retailer, Consumer
4. **Check the database** - See how data is stored
5. **Make some changes** - Start developing!

---

## 💡 Tips

- **Keep both guides handy** - They have detailed troubleshooting sections
- **Check logs** - Most issues are clearly logged
- **Use Swagger UI** - Great for testing backend APIs
- **Browser DevTools** - Essential for frontend debugging

---

**Need Help?**

Open an issue on GitHub or contact the development team.

**Happy Building! 🌾**
