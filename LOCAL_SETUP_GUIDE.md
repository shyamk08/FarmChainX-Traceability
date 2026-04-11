# 💻 FarmChainX - Local Development Setup Guide

This guide will help you set up and run FarmChainX on your local machine for development.

---

## 📋 Prerequisites

### Required Software

Before starting, install the following software:

| Software | Version | Download Link | Purpose |
|----------|---------|---------------|---------|
| **Java JDK** | 21 | [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://adoptium.net/) | Backend runtime |
| **Node.js** | 18+ | [nodejs.org](https://nodejs.org/) | Frontend build tool |
| **MySQL** | 8.0+ | [MySQL Downloads](https://dev.mysql.com/downloads/installer/) | Local database |
| **Git** | Latest | [git-scm.com](https://git-scm.com/) | Version control |
| **Maven** | 3.6+ | Included with project (`mvnw`) | Backend build tool |
| **Angular CLI** | 20+ | `npm install -g @angular/cli` | Frontend framework CLI |

### Verify Installations

Open a terminal and verify each installation:

```bash
# Check Java
java -version
# Expected output: openjdk version "21.0.x" or java version "21.0.x"

# Check Node.js
node -v
# Expected output: v18.x.x or higher

# Check npm
npm -v
# Expected output: 9.x.x or higher

# Check MySQL
mysql --version
# Expected output: mysql  Ver 8.0.x for Win64

# Check Git
git --version
# Expected output: git version 2.x.x

# Check Angular CLI (install if needed)
ng version
# If not installed: npm install -g @angular/cli
```

---

## 🗄️ Part 1: Database Setup

### Step 1.1: Start MySQL Server

**On Windows:**
1. Open **Services** (Win + R → `services.msc`)
2. Find **MySQL80** (or your MySQL version)
3. Right-click → **Start**

**On macOS:**
```bash
brew services start mysql
```

**On Linux:**
```bash
sudo systemctl start mysql
```

### Step 1.2: Access MySQL

Open MySQL command line:

```bash
mysql -u root -p
```

Enter your MySQL root password (the one you set during installation).

> [!TIP]
> If you forgot your password, you may need to reset it. Search for "reset MySQL root password" for your OS.

### Step 1.3: Create Database

Run the following SQL commands:

```sql
-- Create the database
CREATE DATABASE IF NOT EXISTS farmchainX;

-- Verify database was created
SHOW DATABASES;

-- Select the database
USE farmchainX;

-- Exit MySQL
EXIT;
```

> [!NOTE]
> The database name is **case-sensitive** on some systems. Use `farmchainX` exactly as shown.

### Step 1.4: Create a MySQL User (Optional but Recommended)

For better security, create a dedicated user instead of using root:

```sql
-- Create user
CREATE USER 'farmchainx_user'@'localhost' IDENTIFIED BY 'your_secure_password';

-- Grant privileges
GRANT ALL PRIVILEGES ON farmchainX.* TO 'farmchainx_user'@'localhost';

-- Apply changes
FLUSH PRIVILEGES;

-- Exit
EXIT;
```

---

## ⚙️ Part 2: Backend Setup

### Step 2.1: Clone the Repository (if not already done)

```bash
cd E:\projects
git clone <your-repo-url> FarmChainX-master
cd FarmChainX-master
```

### Step 2.2: Configure Database Connection

1. **Navigate to backend resources:**
   ```bash
   cd backend/farmchainX/src/main/resources
   ```

2. **Create `application.properties`** from the example file:
   
   **On Windows (PowerShell):**
   ```powershell
   Copy-Item application.properties.example application.properties
   ```

   **On macOS/Linux:**
   ```bash
   cp application.properties.example application.properties
   ```

3. **Edit `application.properties`:**
   
   Open the file in your favorite editor and update these values:

   ```properties
   # Database Configuration
   spring.datasource.url=jdbc:mysql://localhost:3306/farmchainX?createDatabaseIfNotExist=true
   spring.datasource.username=root
   spring.datasource.password=YOUR_MYSQL_PASSWORD
   spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
   
   spring.jpa.show-sql=true
   spring.jpa.hibernate.ddl-auto=update
   
   # File Upload Configuration
   spring.servlet.multipart.max-file-size=10MB
   spring.servlet.multipart.max-request-size=10MB
   
   # JWT Configuration
   jwt.secret=mySecretKeyForDevelopmentPleaseChangeInProduction123456789
   jwt.expiration=3600000
   jwt.refreshExpiration=86400000
   jwt.header=Authorization
   jwt.prefix=Bearer 
   
   # Rate Limiting
   jwt.login.attempts.max=5
   jwt.login.attempts.window=900000
   jwt.login.lockout.duration=1800000
   
   # Groq AI Configuration (Optional)
   groq.api.key=${GROQ_API_KEY:}
   groq.api.url=https://api.groq.com/openai/v1/chat/completions
   groq.api.model=llama-3.3-70b-versatile
   ```

   > [!IMPORTANT]
   > **Update these values:**
   > - `spring.datasource.password`: Your MySQL root password (or the password of the user you created)
   > - `jwt.secret`: Use a long, random string for security

### Step 2.3: Build and Run Backend

1. **Navigate to backend directory:**
   ```bash
   cd E:\projects\FarmChainX-master\backend\farmchainX
   ```

2. **Clean and build the project:**
   ```bash
   ./mvnw clean install -DskipTests
   ```
   
   > [!NOTE]
   > This will download all dependencies. First run may take 5-10 minutes.

3. **Run the application:**
   ```bash
   ./mvnw spring-boot:run
   ```

4. **Verify backend is running:**
   
   Look for these log messages:
   ```
   Started FarmchainApplication in X.XXX seconds
   Tomcat started on port(s): 8080 (http)
   ```

5. **Test the backend:**
   
   Open your browser and visit:
   - **Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
   - **Health Check**: Create a simple endpoint or check logs

> [!TIP]
> **Keep this terminal open** - the backend needs to keep running while you develop.

---

## 🌐 Part 3: Frontend Setup

### Step 3.1: Navigate to Frontend Directory

Open a **new terminal** (keep backend running in the first one):

```bash
cd E:\projects\FarmChainX-master\frontend
```

### Step 3.2: Install Dependencies

```bash
npm install
```

> [!NOTE]
> This will download all Node.js dependencies. May take 3-5 minutes on first run.

### Step 3.3: Configure Environment (Optional)

1. **Create environment files** if they don't exist:
   
   **Create `src/environments/environment.ts`:**
   ```typescript
   export const environment = {
     production: false,
     apiUrl: 'http://localhost:8080'
   };
   ```

   **Create `src/environments/environment.prod.ts`:**
   ```typescript
   export const environment = {
     production: true,
     apiUrl: 'http://localhost:8080'  // Change this for production
   };
   ```

2. **Update services to use environment variables** (if not already done):
   
   In your service files, use:
   ```typescript
   import { environment } from '../../environments/environment';
   
   private apiUrl = environment.apiUrl;
   ```

### Step 3.4: Run Frontend Development Server

```bash
npm start
```

Or manually:
```bash
ng serve
```

Or with proxy (if configured):
```bash
ng serve --proxy-config proxy.conf.json
```

> [!NOTE]
> The development server will compile your Angular app. This takes 30-60 seconds on first run.

### Step 3.5: Verify Frontend is Running

Look for this message:
```
** Angular Live Development Server is listening on localhost:4200, open your browser on http://localhost:4200/ **
```

**Open your browser** and visit: [http://localhost:4200](http://localhost:4200)

You should see the FarmChainX login page! 🎉

---

## ✅ Part 4: Verification & Testing

### Step 4.1: Test User Registration

1. **Navigate to** [http://localhost:4200](http://localhost:4200)

2. **Click "Register"** or navigate to registration page

3. **Fill in the form:**
   - Username: `testfarmer`
   - Email: `farmer@test.com`
   - Password: `Test@123`
   - Role: `Farmer`

4. **Submit the registration**

5. **Check for success:**
   - You should see a success message
   - Check backend logs for processing
   - Verify in database:
     ```bash
     mysql -u root -p
     USE farmchainX;
     SELECT * FROM users;
     ```

### Step 4.2: Test Login

1. **Go to login page**

2. **Use credentials created above:**
   - Email: `farmer@test.com`
   - Password: `Test@123`

3. **Click "Login"**

4. **Verify you're redirected** to the farmer dashboard

### Step 4.3: Test Role-Based Features

**As Farmer:**
- Create a new product
- Upload product image
- View "My Products"
- Generate QR code

**As Distributor:**
- Register a distributor account
- View marketplace
- Procure products from farmers
- Manage inventory

**As Retailer:**
- Register a retailer account
- View distributor dispatches
- Accept dispatches
- Track inventory

**As Consumer:**
- Scan QR code
- View product history
- Verify product authenticity

### Step 4.4: Verify Database Tables

Check that tables were auto-created:

```bash
mysql -u root -p
USE farmchainX;

-- Show all tables
SHOW TABLES;

-- Check table structures
DESCRIBE users;
DESCRIBE products;
DESCRIBE inventory;
DESCRIBE transactions;

-- View some data
SELECT * FROM users;
SELECT * FROM products;
```

---

## 🔧 Part 5: Development Workflow

### Running Both Services

You need **two terminal windows** running simultaneously:

**Terminal 1 - Backend:**
```bash
cd E:\projects\FarmChainX-master\backend\farmchainX
./mvnw spring-boot:run
```

**Terminal 2 - Frontend:**
```bash
cd E:\projects\FarmChainX-master\frontend
npm start
```

### Making Changes

**Backend Changes:**
1. Edit Java files in `backend/farmchainX/src/main/java`
2. Press `Ctrl+C` in backend terminal
3. Run `./mvnw spring-boot:run` again
4. Or use Spring Boot DevTools for hot reload (already configured)

**Frontend Changes:**
1. Edit TypeScript/HTML/CSS files in `frontend/src`
2. Changes are automatically detected and recompiled
3. Browser auto-refreshes (Hot Module Replacement)

### Stopping Services

**Stop Backend:**
- Press `Ctrl+C` in backend terminal

**Stop Frontend:**
- Press `Ctrl+C` in frontend terminal

**Stop MySQL:**
- Windows: Services → MySQL80 → Stop
- macOS: `brew services stop mysql`
- Linux: `sudo systemctl stop mysql`

---

## 🐛 Common Issues & Troubleshooting

### Issue 1: Backend won't start - "Cannot create PoolableConnectionFactory"

**Symptoms:** Error connecting to MySQL

**Solutions:**
1. **Verify MySQL is running:**
   ```bash
   mysql -u root -p
   ```
2. **Check credentials** in `application.properties`
3. **Verify database exists:**
   ```sql
   SHOW DATABASES;
   ```
4. **Check MySQL port** (should be 3306):
   ```sql
   SHOW VARIABLES LIKE 'port';
   ```

### Issue 2: Frontend shows "Cannot GET /api/*"

**Symptoms:** API calls returning 404

**Solutions:**
1. **Ensure backend is running** on port 8080
2. **Check proxy configuration** in `proxy.conf.json`:
   ```json
   {
     "/api": {
       "target": "http://localhost:8080",
       "secure": false,
       "changeOrigin": true
     }
   }
   ```
3. **Start with proxy:**
   ```bash
   ng serve --proxy-config proxy.conf.json
   ```

### Issue 3: Port 8080 or 4200 already in use

**Symptoms:** "Port already in use" error

**Solutions:**

**Find process using port:**
```bash
# Windows
netstat -ano | findstr :8080
netstat -ano | findstr :4200

# Kill process (replace PID)
taskkill /PID <PID> /F
```

**Or change ports:**
- **Backend**: Add to `application.properties`:
  ```properties
  server.port=8081
  ```
- **Frontend**: Run with custom port:
  ```bash
  ng serve --port 4201
  ```

### Issue 4: "Module not found" errors in frontend

**Symptoms:** TypeScript compilation errors

**Solutions:**
1. **Delete node_modules and reinstall:**
   ```bash
   rm -rf node_modules package-lock.json
   npm install
   ```
2. **Clear Angular cache:**
   ```bash
   rm -rf .angular
   ```
3. **Rebuild:**
   ```bash
   ng serve
   ```

### Issue 5: Changes not reflecting

**Symptoms:** Code changes don't appear

**Solutions:**

**Backend:**
- Ensure Spring Boot DevTools is active
- Or restart manually: `Ctrl+C` then `./mvnw spring-boot:run`

**Frontend:**
- Check terminal for compilation errors
- Hard refresh browser: `Ctrl+F5`
- Clear browser cache

### Issue 6: Database tables not created

**Symptoms:** Table doesn't exist errors

**Solutions:**
1. **Check `hibernate.ddl-auto`** setting:
   ```properties
   spring.jpa.hibernate.ddl-auto=update
   ```
2. **Manually create tables** by running the backend once
3. **Check database logs** in backend console
4. **Verify entity classes** have proper JPA annotations

---

## 📚 Additional Development Tips

### Database Management Tools

Use a GUI tool for easier database inspection:
- **MySQL Workbench** (Free): [Download](https://dev.mysql.com/downloads/workbench/)
- **DBeaver** (Free): [Download](https://dbeaver.io/)
- **phpMyAdmin** (Web-based): [Setup Guide](https://www.phpmyadmin.net/)

### Backend Testing

**Run tests:**
```bash
./mvnw test
```

**Access H2 Console** (if configured):
```
http://localhost:8080/h2-console
```

### Frontend Testing

**Run unit tests:**
```bash
npm test
```

**Run end-to-end tests:**
```bash
npm run e2e
```

### API Testing Tools

- **Postman**: [Download](https://www.postman.com/)
- **Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **cURL**: Command-line testing

### Useful Commands

**Backend:**
```bash
# Clean build
./mvnw clean

# Package without tests
./mvnw package -DskipTests

# Run with specific profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

**Frontend:**
```bash
# Build for production
npm run build

# Lint code
ng lint

# Format code
npm run format
```

---

## 🎯 Next Steps

After successful local setup:

1. **Explore the codebase**
   - Backend: `backend/farmchainX/src/main/java`
   - Frontend: `frontend/src/app`

2. **Set up your IDE**
   - **IntelliJ IDEA** (recommended for Java): [Download](https://www.jetbrains.com/idea/)
   - **VS Code**: [Download](https://code.visualstudio.com/)
   - Install relevant extensions (Java, Angular, etc.)

3. **Read the documentation**
   - Check `README.md` for project overview
   - Review API documentation in Swagger

4. **Start developing!**
   - Pick a feature from GitHub issues
   - Create a new branch
   - Make your changes
   - Test thoroughly
   - Submit a pull request

---

## 📞 Getting Help

If you encounter issues not covered here:

1. **Check logs carefully** - most issues are logged
2. **Search online** - Stack Overflow, GitHub issues
3. **Contact team** - Open an issue on GitHub
4. **Review documentation** - Spring Boot, Angular docs

---

**Happy Coding! 🚀**

Your local development environment is now ready. Time to build amazing features for FarmChainX!
