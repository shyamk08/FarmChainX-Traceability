import { Component } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common'; // Added DatePipe just in case
import { RouterOutlet } from '@angular/router';
import { AdminSidebar } from '../components/admin-sidebar/admin-sidebar';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, AdminSidebar],
  templateUrl: './admin-layout.html',
  styleUrls: ['./admin-layout.scss'],
})
export class AdminLayout {
  // Sidebar toggle state
  sidebarOpen = false;

  constructor(private authService: AuthService) {}

  /**
   * Toggles the state of the mobile sidebar.
   */
  toggleSidebar(): void {
    this.sidebarOpen = !this.sidebarOpen;
  }

  /**
   * Retrieves and formats the current admin's first name.
   */
  currentAdminName(): string {
    const name = this.authService.getName();
    return name ? name.split(' ')[0] : 'Admin';
  }
}
