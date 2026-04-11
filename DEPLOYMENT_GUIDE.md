# 🚀 FarmChainX - Complete Deployment Guide

This guide will walk you through deploying FarmChainX to the cloud using **Render** for the backend and **Vercel** for the frontend.

---

## 📋 Prerequisites

Before starting, ensure you have:
- ✅ A GitHub account
- ✅ A Render account (sign up at [render.com](https://render.com))
- ✅ A Vercel account (sign up at [vercel.com](https://vercel.com))
- ✅ Git installed on your machine
- ✅ Your project pushed to a GitHub repository

---

## 🗄️ Part 1: Deploy PostgreSQL Database on Render

### Step 1.1: Create PostgreSQL Database

1. **Log in to Render** at [dashboard.render.com](https://dashboard.render.com)

2. **Click "New +"** button in the top right

3. **Select "PostgreSQL"**

4. **Configure the database:**
   - **Name**: `farmchainx-db` (or any name you prefer)
   - **Database**: `farmchainx` (this will be your database name)
   - **User**: (auto-generated, you'll use this later)
   - **Region**: Choose the closest to your users
   - **PostgreSQL Version**: 16 (or latest)
   - **Instance Type**: Free (or paid based on your needs)

5. **Click "Create Database"**

6. **Wait 2-3 minutes** for the database to be ready

### Step 1.2: Save Database Credentials

Once created, you'll see a dashboard with connection details:

- **Internal Database URL**: `postgresql://user:password@host:port/database`
- **External Database URL**: `postgresql://user:password@external-host:port/database`
- **PSQL Command**: Connection string for manual access

> [!IMPORTANT]
> **Copy the "Internal Database URL"** - you'll need this for backend configuration. It looks like:
> ```
> postgresql://farmchainx_db_user:xxxxxxxxxxxxx@dpg-xxxxx-a/farmchainx_db
> ```

---

## ⚙️ Part 2: Deploy Backend on Render

### Step 2.1: Prepare Backend for Deployment

1. **Ensure your `pom.xml` includes PostgreSQL driver** (already configured in your project):
   ```xml
   <dependency>
       <groupId>org.postgresql</groupId>
       <artifactId>postgresql</artifactId>
       <scope>runtime</scope>
   </dependency>
   ```

2. **Create a production application properties file** if needed, or use environment variables (recommended)

### Step 2.2: Create Web Service on Render

1. **Go to Render Dashboard** and click **"New +"**

2. **Select "Web Service"**

3. **Connect your GitHub repository:**
   - Click "Connect" next to your GitHub account
   - Select your `FarmChainX` repository
   - Grant necessary permissions

4. **Configure the Web Service:**
   
   **Basic Settings:**
   - **Name**: `farmchainx-backend`
   - **Region**: Same as your database
   - **Branch**: `main` (or your default branch)
   - **Root Directory**: `backend/farmchainX`
   - **Runtime**: `Java`
   - **Build Command**: 
     ```bash
     ./mvnw clean install -DskipTests
     ```
   - **Start Command**:
     ```bash
     java -jar target/farmchainx-0.0.1-SNAPSHOT.jar
     ```

   **Instance Type:**
   - Free (or paid based on your needs)

5. **Add Environment Variables:**

   Click "Advanced" → "Add Environment Variable" and add the following:

   | Key | Value | Notes |
   |-----|-------|-------|
   | `SPRING_DATASOURCE_URL` | `<Your Internal Database URL from Step 1.2>` | PostgreSQL connection string |
   | `SPRING_DATASOURCE_USERNAME` | `<Database username>` | Extract from database URL |
   | `SPRING_DATASOURCE_PASSWORD` | `<Database password>` | Extract from database URL |
   | `SPRING_DATASOURCE_DRIVER_CLASS_NAME` | `org.postgresql.Driver` | PostgreSQL driver |
   | `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` | Auto-create tables |
   | `SPRING_JPA_DATABASE_PLATFORM` | `org.hibernate.dialect.PostgreSQLDialect` | PostgreSQL dialect |
   | `JWT_SECRET` | `<Your secure random string>` | Generate a strong secret key |
   | `GROQ_API_KEY` | `<Your Groq API Key>` | If using AI features |

   > [!TIP]
   > **To generate a secure JWT secret**, run this in your terminal:
   > ```bash
   > node -e "console.log(require('crypto').randomBytes(64).toString('base64'))"
   > ```
   > Or use any random 64+ character string

   > [!WARNING]
   > **Database URL Format**: The URL should be in the format:
   > ```
   > jdbc:postgresql://dpg-xxxxx-a.oregon-postgres.render.com:5432/farmchainx_db
   > ```
   > If Render gives you `postgresql://...`, add `jdbc:` prefix and ensure the port is included.

6. **Click "Create Web Service"**

7. **Wait for deployment** (first deployment takes 5-10 minutes)

### Step 2.3: Verify Backend Deployment

1. **Check deployment logs** for any errors

2. **Once deployed**, you'll get a URL like: `https://farmchainx-backend.onrender.com`

3. **Test the API:**
   - Open: `https://farmchainx-backend.onrender.com/api/auth/health` (if you have a health endpoint)
   - Or check Swagger UI: `https://farmchainx-backend.onrender.com/swagger-ui/index.html`

> [!NOTE]
> Free tier services on Render spin down after 15 minutes of inactivity. The first request after spin-down may take 30-60 seconds.

---

## 🌐 Part 3: Deploy Frontend on Vercel

### Step 3.1: Prepare Frontend Configuration

**Create an environment configuration file:**

1. **Navigate to your frontend directory**

2. **Create `src/environments/environment.prod.ts`:**
   ```typescript
   export const environment = {
     production: true,
     apiUrl: 'https://farmchainx-backend.onrender.com'
   };
   ```

3. **Create `src/environments/environment.ts`** (for development):
   ```typescript
   export const environment = {
     production: false,
     apiUrl: 'http://localhost:8080'
   };
   ```

4. **Update your API service to use environment variables:**

   In your services (e.g., `auth.service.ts`, `product.service.ts`), replace hardcoded URLs:
   ```typescript
   import { environment } from '../../environments/environment';
   
   // Replace 'http://localhost:8080' with:
   private apiUrl = environment.apiUrl;
   ```

   Or use a base URL service/interceptor approach.

### Step 3.2: Deploy to Vercel

#### Option A: Deploy via Vercel CLI (Recommended)

1. **Install Vercel CLI:**
   ```bash
   npm install -g vercel
   ```

2. **Navigate to frontend directory:**
   ```bash
   cd frontend
   ```

3. **Login to Vercel:**
   ```bash
   vercel login
   ```

4. **Deploy:**
   ```bash
   vercel --prod
   ```

5. **Follow the prompts:**
   - Set up and deploy: `Y`
   - Which scope: Select your account
   - Link to existing project: `N`
   - Project name: `farmchainx-frontend`
   - Directory: `./` (current directory)
   - Override settings: `N`

#### Option B: Deploy via Vercel Dashboard

1. **Go to [vercel.com/new](https://vercel.com/new)**

2. **Import your Git repository:**
   - Click "Import Git Repository"
   - Select your GitHub account and repository
   - Select the repository

3. **Configure project:**
   - **Project Name**: `farmchainx-frontend`
   - **Framework Preset**: Angular
   - **Root Directory**: `frontend`
   - **Build Command**: `npm run build` (or leave auto-detected)
   - **Output Directory**: `dist/farmchainx-frontend/browser` (check your `angular.json`)
   - **Install Command**: `npm install`

4. **Add Environment Variables:**
   - Click "Environment Variables"
   - Add: `NG_APP_API_URL` = `https://farmchainx-backend.onrender.com`

5. **Click "Deploy"**

6. **Wait 2-5 minutes** for deployment to complete

### Step 3.3: Verify Frontend Deployment

1. **You'll get a URL** like: `https://farmchainx-frontend.vercel.app`

2. **Test the application:**
   - Open the URL in your browser
   - Try to register a new user
   - Check if API calls work (check browser console for errors)

---

## 🔧 Part 4: Configure CORS

Your backend needs to allow requests from your Vercel frontend.

### Step 4.1: Update CORS Configuration

1. **Find your CORS configuration** in `backend/farmchainX/src/main/java/com/farmchainx/config/CorsConfig.java`

2. **Update the `allowedOrigins`** to include your Vercel URL:
   ```java
   @Override
   public void addCorsMappings(CorsRegistry registry) {
       registry.addMapping("/**")
           .allowedOrigins(
               "http://localhost:4200",
               "https://farmchainx-frontend.vercel.app",  // Add your Vercel URL
               "https://*.vercel.app"  // Allow all Vercel preview deployments
           )
           .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
           .allowedHeaders("*")
           .allowCredentials(true);
   }
   ```

3. **Commit and push changes:**
   ```bash
   git add .
   git commit -m "Add Vercel URL to CORS configuration"
   git push origin main
   ```

4. **Render will automatically redeploy** your backend (takes 2-3 minutes)

---

## ✅ Part 5: Final Verification

### Step 5.1: Test Complete Flow

1. **Open your Vercel frontend URL**

2. **Test user registration:**
   - Go to register page
   - Create a new account
   - Check if data is saved (check Render logs or database)

3. **Test login:**
   - Login with created credentials
   - Verify JWT token is received

4. **Test role-specific features:**
   - Farmer: Create a product
   - Distributor: View marketplace
   - Retailer: View inventory
   - Consumer: Scan QR code

### Step 5.2: Monitor and Debug

**Backend Logs (Render):**
- Go to Render dashboard → Your service → "Logs" tab
- Check for errors, API calls, database connections

**Frontend Logs (Vercel):**
- Go to Vercel dashboard → Your project → "Deployments" → Click on deployment → "Functions" tab
- Check browser console for client-side errors

**Database (Render):**
- Go to your PostgreSQL database → "Connect" tab
- Use the PSQL command to connect via terminal:
  ```bash
  psql postgresql://user:password@host:port/database
  ```
- Run queries to verify data:
  ```sql
  \dt  -- List all tables
  SELECT * FROM users LIMIT 10;
  SELECT * FROM products LIMIT 10;
  ```

---

## 🐛 Common Issues & Troubleshooting

### Issue 1: Backend not connecting to database

**Symptoms:** 500 errors, "Unable to acquire JDBC Connection"

**Solution:**
- Verify `SPRING_DATASOURCE_URL` environment variable is correct
- Ensure `jdbc:postgresql://` prefix is present
- Check username and password are correct
- Verify database is in the same region as the backend

### Issue 2: Frontend showing 405 Method Not Allowed

**Symptoms:** API calls failing with 405 errors

**Solution:**
- Check CORS configuration includes your Vercel URL
- Verify API endpoints are correct (not using `/api` prefix if backend doesn't have it)
- Check if backend is actually running (visit backend URL directly)

### Issue 3: Frontend showing CORS errors

**Symptoms:** "Access to XMLHttpRequest has been blocked by CORS policy"

**Solution:**
- Add Vercel URL to CORS allowed origins
- Ensure `allowCredentials(true)` is set if using cookies/auth headers
- Redeploy backend after CORS changes

### Issue 4: Environment variables not working

**Symptoms:** App using default/wrong values

**Solution:**
- Verify environment variables are set correctly in Render/Vercel dashboard
- Restart the service manually
- Check logs for "Using property" or "Loading environment" messages

### Issue 5: Build failures

**Symptoms:** Deployment fails during build

**Solution Backend:**
- Ensure Java 21 is specified in Render settings
- Check `mvnw` has execute permissions
- Verify `pom.xml` dependencies resolve correctly

**Solution Frontend:**
- Check Node version compatibility (Angular 20 requires Node 18+)
- Clear `node_modules` and rebuild
- Check for TypeScript errors in build logs

---

## 🎯 Next Steps

After successful deployment:

1. **Set up custom domain** (optional)
   - Vercel: Project Settings → Domains
   - Render: Settings → Custom Domains

2. **Enable HTTPS** (automatic on both platforms)

3. **Set up monitoring:**
   - Render: Built-in metrics
   - Vercel: Analytics dashboard

4. **Configure auto-deployments:**
   - Both platforms auto-deploy on git push to main branch

5. **Database backups:**
   - Render: Automated backups on paid plans

---

## 📚 Additional Resources

- [Render Documentation](https://render.com/docs)
- [Vercel Documentation](https://vercel.com/docs)
- [Spring Boot Deployment Guide](https://spring.io/guides/gs/spring-boot/)
- [Angular Deployment Guide](https://angular.io/guide/deployment)

---

**Congratulations! Your FarmChainX application is now live! 🎉**

Visit your frontend URL and start using your deployed application.
