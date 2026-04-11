import { Component, inject, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';


@Component({
  standalone: true,
  imports: [CommonModule, RouterLink],
  selector: 'app-dashboard',
  templateUrl: './dashboard.html',
})
export class Dashboard implements AfterViewInit {
  private authService = inject(AuthService);

  private router = inject(Router);

  name = this.authService.getName() || 'User';
  // make sure role values like "ROLE_CONSUMER" become "CONSUMER"
  role = this.authService.getRole()?.replace('ROLE_', '') || 'USER';
  isAdmin = this.authService.isAdmin();

  get panelConfig() {
    const roleKey = this.role.toUpperCase();

    const roleMap: {
      [key: string]: { label: string; route: string; subtitle: string };
    } = {
      ADMIN: {
        label: 'Admin Panel',
        route: '/admin',
        subtitle: 'You have elevated access',
      },
      DISTRIBUTOR: {
        label: 'Distributor Panel',
        route: '/distributor',
        subtitle: 'Manage distribution',
      },
      CONSUMER: {
        label: 'Consumer Panel',
        route: '/consumer',
        subtitle: 'Track your purchases',
      },
      RETAILER: {
        label: 'Retailer Panel',
        route: '/retailer',
        subtitle: 'Manage retail operations',
      },
      FARMER: {
        label: 'Farmer Dashboard',
        route: '/farmer/dashboard',
        subtitle: 'View AI predictions and insights',
      },
    };

    return roleMap[roleKey] || null;
  }

  ngAfterViewInit(): void {
    const ringColor =
      getComputedStyle(document.documentElement).getPropertyValue('--tw-ring-color') || '(not set)';
    console.log('tw-ring-color:', ringColor);
  }




}
