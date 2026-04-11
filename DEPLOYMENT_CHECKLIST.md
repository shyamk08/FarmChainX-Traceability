# 🚀 FarmChainX Deployment Checklist

## ✅ Pre-Deployment (Completed by AI)

- [x] Created `environment.prod.ts` for frontend
- [x] Created `Procfile` for Railway
- [x] Created `vercel.json` for Vercel routing
- [x] Created `application.properties.template`
- [x] Created `CorsConfig.java` for CORS handling
- [x] Created production build configuration

## 📝 Step 1: Prepare Your Code

### Backend Preparation

1. **Update application.properties**
   ```bash
   cd e:\projects\FarmChainX-master\backend\farmchainX\src\main\resources
   ```
   - Copy `application.properties.template` content
   - Update your existing `application.properties` with the template values
   - Keep your local database settings for now

2. **Commit Backend Changes**
   ```bash
   cd e:\projects\FarmChainX-master
   git add backend/
   git commit -m "Add production configurations for deployment"
   ```

### Frontend Preparation

1. **Update API URL Placeholder**
   - File already created: `frontend/src/environments/environment.prod.ts`
   - You'll update the Railway URL after backend deployment

2. **Commit Frontend Changes**
   ```bash
   git add frontend/
   git commit -m "Add production environment and Vercel config"
   git push origin main
   ```

## 🗄️ Step 2: Set Up Database (Railway)

1. **Sign up at Railway**
   - Go to https://railway.app
   - Click "Login with GitHub"
   - Authorize Railway

2. **Create MySQL Database**
   - Click "New Project"
   - Select "Provision MySQL"
   - Wait 2-3 minutes for deployment

3. **Get Database Credentials**
   - Click on MySQL service
   - Go to "Variables" tab
   - Copy these values:
     ```
     MYSQLHOST
     MYSQLPORT
     MYSQLDATABASE
     MYSQLUSER
     MYSQLPASSWORD
     ```
   - Keep them in a notepad for next steps

## 🔧 Step 3: Deploy Backend (Railway)

1. **Create New Project**
   - Railway Dashboard → "New Project"
   - Select "Deploy from GitHub repo"
   - Choose "FarmChainX" repository
   - Select "main" branch

2. **Configure Service**
   - Click "Add Service" → "GitHub Repo"
   - Root Directory: `backend/farmchainX`
   - Build Command: `./mvnw clean package -DskipTests`
   - Start Command: `java -Dserver.port=$PORT -jar target/*.jar`

3. **Add Environment Variables**
   
   Click "Variables" and add:
   ```
   MYSQLHOST=<from step 2>
   MYSQLPORT=3306
   MYSQLDATABASE=railway
   MYSQLUSER=root
   MYSQLPASSWORD=<from step 2>
   PORT=8080
   FRONTEND_URL=http://localhost:4200
   JWT_SECRET=ChangeThisToASecureRandomString123!@#
   ```

4. **Deploy**
   - Click "Deploy"
   - Wait 5-10 minutes
   - Copy your backend URL: `https://farmchainx-production.up.railway.app`

5. **Verify Backend**
   - Open: `https://your-backend-url.railway.app`
   - Should see Whitelabel Error Page (This is OK! It means backend is running)

## 🌐 Step 4: Deploy Frontend (Vercel)

1. **Update Production Environment**
   ```bash
   cd e:\projects\FarmChainX-master\frontend\src\environments
   ```
   
   Edit `environment.prod.ts`:
   ```typescript
   export const environment = {
     production: true,
     apiUrl: 'https://your-actual-railway-url.railway.app', // UPDATE THIS
     appName: 'FarmChainX',
     version: '1.0.0'
   };
   ```

   Commit:
   ```bash
   git add frontend/src/environments/environment.prod.ts
   git commit -m "Update production API URL"
   git push
   ```

2. **Sign Up at Vercel**
   - Go to https://vercel.com
   - Click "Sign Up" → "Continue with GitHub"
   - Authorize Vercel

3. **Import Project**
   - Click "Add New" → "Project"
   - Select "FarmChainX" repository
   - Click "Import"

4. **Configure Build Settings**
   
   **Framework Preset**: Angular
   
   **Root Directory**: `frontend`
   
   **Build Command**: 
   ```
   npm install && npm run build -- --configuration=production
   ```
   
   **Output Directory**: 
   ```
   dist/frontend/browser
   ```
   
   **Install Command**:
   ```
   npm install
   ```

5. **Environment Variables**
   - Add: `NODE_VERSION = 18`

6. **Deploy**
   - Click "Deploy"
   - Wait 3-5 minutes
   - Copy your frontend URL: `https://farmchainx.vercel.app`

## 🔐 Step 5: Update CORS Configuration

1. **Update Backend Environment Variable**
   - Go to Railway Dashboard
   - Click on your backend service
   - Go to "Variables"
   - Update `FRONTEND_URL` to: `https://farmchainx.vercel.app` (your actual Vercel URL)
   - Service will auto-redeploy

## 🧪 Step 6: Test Your Deployment

### Test Backend API
```bash
curl https://your-backend.railway.app/api/health
```

### Test Frontend
1. Open `https://your-frontend.vercel.app`
2. Test features:
   - ✅ Can access homepage
   - ✅ Can navigate to login
   - ✅ Can register new user
   - ✅ Can login
   - ✅ Dashboard loads
   - ✅ API calls work (check browser console F12)

### Check for Errors
- Open Browser DevTools (F12)
- Check Console for errors
- Check Network tab for failed API calls

## 🎉 Deployment Complete!

Your application is now live at:
- **Frontend**: https://your-app.vercel.app
- **Backend**: https://your-app.railway.app
- **Database**: Railway MySQL

## 🚨 Troubleshooting

### Issue: CORS Error
- Verify `FRONTEND_URL` in Railway matches your Vercel URL
- Check CorsConfig.java is deployed
- Redeploy backend after changing environment variables

### Issue: API Calls Fail
- Check `environment.prod.ts` has correct Railway URL
- Verify backend is running in Railway dashboard
- Check Railway logs for errors

### Issue: Database Connection Error
- Verify all MySQL environment variables in Railway
- Check database service is running
- Review Railway backend logs

### Issue: Angular Routes Show 404
- Verify `vercel.json` exists in frontend root
- Redeploy on Vercel

## 📊 Monitor Your Deployment

- **Railway**: Check logs and metrics in dashboard
- **Vercel**: Check deployment logs and analytics
- **Database**: Monitor connections in Railway MySQL service

## 💾 Environment Variables Reference

### Backend (Railway)
```bash
MYSQLHOST=containers-us-west-xxx.railway.app
MYSQLPORT=3306
MYSQLDATABASE=railway
MYSQLUSER=root
MYSQLPASSWORD=your_password
PORT=8080
FRONTEND_URL=https://your-frontend.vercel.app
JWT_SECRET=your-secure-secret-key
```

### Frontend (environment.prod.ts)
```typescript
apiUrl: 'https://your-backend.railway.app'
```

---

**Need Help?** Check the deployment_guide.md for detailed troubleshooting!
