// src/app/app.config.ts

import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
// Import 'withInterceptors' and your interceptor
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';
import { JwtInterceptor } from './interceptors/jwt.interceptor'; // <-- 1. Import your Interceptor

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    // 2. Update provideHttpClient to include the interceptor
    provideHttpClient(
      withInterceptors([JwtInterceptor]) // <-- Registration goes here
    ),
  ],
};
