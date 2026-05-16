import { Component, inject, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { TranslateModule } from '@ngx-translate/core';


@Component({
  standalone: true,
  imports: [CommonModule, RouterLink, TranslateModule],
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
        label: 'ADMIN_PANEL',
        route: '/admin',
        subtitle: 'ADMIN_SUBTITLE',
      },
      DISTRIBUTOR: {
        label: 'DISTRIBUTOR_PANEL',
        route: '/distributor',
        subtitle: 'DISTRIBUTOR_SUBTITLE',
      },
      CONSUMER: {
        label: 'CONSUMER_PANEL',
        route: '/consumer',
        subtitle: 'CONSUMER_SUBTITLE',
      },
      RETAILER: {
        label: 'RETAILER_PANEL',
        route: '/retailer',
        subtitle: 'RETAILER_SUBTITLE',
      },
      FARMER: {
        label: 'FARMER_DASHBOARD',
        route: '/farmer/dashboard',
        subtitle: 'FARMER_SUBTITLE',
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
