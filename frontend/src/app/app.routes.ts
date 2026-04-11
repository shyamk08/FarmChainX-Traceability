// src/app/app.routes.ts
import { Routes } from '@angular/router';
import { AuthGuard } from './guards/auth.guard';
import { AdminGuard } from './guards/admin-guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/home/home').then((m) => m.Home),
  },
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login').then((m) => m.Login),
  },
  {
    path: 'register',
    loadComponent: () => import('./pages/register/register').then((m) => m.RegisterComponent),
  },

  {
    path: 'verify/:uuid',
    loadComponent: () =>
      import('./components/verify-product/verify-product').then((m) => m.VerifyProduct),
  },

  // User routes
  {
    path: 'dashboard',
    canActivate: [AuthGuard],
    loadComponent: () => import('./pages/dashboard/dashboard').then((m) => m.Dashboard),
  },
  {
    path: 'upload',
    canActivate: [AuthGuard],
    loadComponent: () =>
      import('./pages/upload-product/upload-product').then((m) => m.UploadProduct),
  },
  {
    path: 'scanner',
    canActivate: [AuthGuard],
    loadComponent: () =>
      import('./components/qr-scanner/qr-scanner/qr-scanner').then((m) => m.QrScanner),
  },
  {
    path: 'products/my',
    canActivate: [AuthGuard],
    loadComponent: () => import('./pages/my-products/my-products').then((m) => m.MyProducts),
  },
  {
    path: 'farmer/dashboard',
    canActivate: [AuthGuard],
    loadComponent: () =>
      import('./pages/farmer/farmer-dashboard/farmer-dashboard.component').then(
        (m) => m.FarmerDashboardComponent
      ),
  },

  // FARMER ROUTES
  {
    path: 'farmer',
    canActivate: [AuthGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./pages/farmer/farmer-dashboard/farmer-dashboard').then(m => m.FarmerDashboard)
      },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },

  // CONSUMER ROUTES
  {
    path: 'consumer',
    canActivate: [AuthGuard],
    loadComponent: () =>
      import('./pages/consumer/consumer-layout/consumer-layout.component').then(
        (m) => m.ConsumerLayoutComponent
      ),
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./pages/consumer/consumer-dashboard/consumer-dashboard.component').then(
            (m) => m.ConsumerDashboardComponent
          ),
      },
      {
        path: 'retailers-inventory',
        loadComponent: () =>
          import('./pages/consumer/retailers-inventory/retailers-inventory').then(
            (m) => m.RetailersInventory
          ),
      },
      {
        path: 'history',
        loadComponent: () =>
          import('./pages/consumer/consumer-history/consumer-history.component').then(
            (m) => m.ConsumerHistoryComponent
          ),
      },
      {
        path: 'notifications',
        loadComponent: () =>
          import('./pages/consumer/consumer-notifications/consumer-notifications.component').then(
            (m) => m.ConsumerNotificationsComponent
          ),
      },
      {
        path: 'marketplace',
        loadComponent: () =>
          import('./pages/consumer/consumer-marketplace/consumer-marketplace.component').then(
            (m) => m.ConsumerMarketplaceComponent
          ),
      },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    ],
  },

  // ADMIN ROUTES
  {
    path: 'admin',
    canActivate: [AuthGuard, AdminGuard],
    loadComponent: () =>
      import('./pages/admin/admin-layout/admin-layout').then((m) => m.AdminLayout),
    children: [
      {
        path: 'overview',
        loadComponent: () =>
          import('./pages/admin/admin-overview/admin-overview').then((m) => m.AdminOverview),
      },
      {
        path: 'users',
        loadComponent: () =>
          import('./pages/admin/admin-users/admin-users').then((m) => m.AdminUsers),
      },
      {
        path: 'logs',
        loadComponent: () =>
          import('./pages/admin/admin-logs/admin-logs').then((m) => m.AdminLogs),
      },

      { path: '', redirectTo: 'overview', pathMatch: 'full' },
    ],
  },
  // =======================
  // DISTRIBUTOR ROUTES
  // =======================
  {
    path: 'distributor',
    canActivate: [AuthGuard],
    loadComponent: () =>
      import('./pages/distributor/distributor-layout/distributor-layout.component').then(
        (m) => m.DistributorLayoutComponent
      ),
    children: [
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full',
      },

      // Dashboard
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./pages/distributor/distributor-dashboard/distributor-dashboard.component').then(
            (m) => m.DistributorDashboardComponent
          ),
      },

      // Profile
      {
        path: 'profile',
        loadComponent: () =>
          import('./pages/distributor/distributor-profile/distributor-profile.component').then(
            (m) => m.DistributorProfileComponent
          ),
      },

      // Farmer Procurement
      {
        path: 'farmer-procurement',
        loadComponent: () =>
          import('./pages/distributor/farmer-procurement/farmer-procurement.component').then(
            (m) => m.FarmerProcurementComponent
          ),
      },

      // Procurement Detail
      {
        path: 'procurement-detail',
        loadComponent: () =>
          import('./pages/distributor/procurement-detail/procurement-detail.component').then(
            (m) => m.ProcurementDetailComponent
          ),
      },

      // Inventory
      {
        path: 'inventory',
        loadComponent: () =>
          import('./pages/distributor/inventory/inventory.component').then(
            (m) => m.InventoryComponent
          ),
      },

      // Dispatch
      {
        path: 'dispatch',
        loadComponent: () =>
          import('./pages/distributor/dispatch/dispatch.component').then(
            (m) => m.DispatchComponent
          ),
      },

      // Retailer Selection
      {
        path: 'retailer-selection',
        loadComponent: () =>
          import('./pages/distributor/retailer-selection/retailer-selection.component').then(
            (m) => m.RetailerSelectionComponent
          ),
      },
    ],
  },

  // RETAILER ROUTES (placed under src/app/pages/retailer/*)
  // Parent route uses AuthGuard so only authenticated users can access retailer panel.
  {
    path: 'retailer',
    canActivate: [AuthGuard],
    loadComponent: () =>
      import('./pages/retailer/retailer-layout/retailer-layout.component').then(
        (m) => m.RetailerLayoutComponent
      ),
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard',
      },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./pages/retailer/retailer-dashboard/retailer-dashboard.component').then(
            (m) => m.RetailerDashboardComponent
          ),
      },
      {
        path: 'inventory',
        loadComponent: () =>
          import('./pages/retailer/retailer-inventory/retailer-inventory.component').then(
            (m) => m.RetailerInventoryComponent
          ),
      },
      {
        path: 'dispatched-products',
        loadComponent: () =>
          import('./pages/retailer/retailer-dispatched-products/retailer-dispatched-products.component').then(
            (m) => m.RetailerDispatchedProductsComponent
          ),
      },
      {
        path: 'orders',
        loadComponent: () =>
          import('./pages/retailer/retailer-orders/retailer-orders.component').then(
            (m) => m.RetailerOrdersComponent
          ),
      },
      {
        path: 'shipments',
        loadComponent: () =>
          import('./pages/retailer/retailer-shipments/retailer-shipments.component').then(
            (m) => m.RetailerShipmentsComponent
          ),
      },
      {
        path: 'market',
        loadComponent: () =>
          import('./pages/retailer/distributor-market/distributor-market.component').then(
            (m) => m.DistributorMarketComponent
          ),
      },
    ],
  },

  // fallback
  { path: '**', redirectTo: '' },
];
