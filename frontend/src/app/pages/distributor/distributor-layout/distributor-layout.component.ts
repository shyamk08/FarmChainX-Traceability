import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, RouterOutlet } from '@angular/router';

@Component({
  standalone: true,
  selector: 'app-distributor-layout',
  imports: [CommonModule, RouterModule, RouterOutlet],
  templateUrl: './distributor-layout.component.html',
})
export class DistributorLayoutComponent {
  sidebarOpen = false;

  toggleSidebar() {
    this.sidebarOpen = !this.sidebarOpen;
  }
}
