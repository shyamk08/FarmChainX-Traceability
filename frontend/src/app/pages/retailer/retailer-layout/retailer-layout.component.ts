// src/app/pages/retailer/retailer-layout/retailer-layout.component.ts
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterModule } from '@angular/router';

@Component({
  standalone: true,
  imports: [CommonModule, RouterModule, RouterOutlet],
  selector: 'app-retailer-layout',
  templateUrl: './retailer-layout.component.html',
})
export class RetailerLayoutComponent {
  mobileOpen = false;

  // Example retailer info and sidebar counts (dummy). Replace with API values later.
  retailerName = 'GreenStore';
  retailerInitials = this.computeInitials(this.retailerName);

  counts = {
    dashboard: 1,
    inventory: 3,
    orders: 6,
    shipments: 2,
  };

  toggleMobile() {
    this.mobileOpen = !this.mobileOpen;
  }

  createPO() {
    // Hook this up to a create PO modal or router navigation later
    alert('Create PO — UI placeholder');
  }

  computeInitials(name: string) {
    if (!name) return 'R';
    return name
      .split(' ')
      .map((s) => s.charAt(0))
      .slice(0, 2)
      .join('')
      .toUpperCase();
  }
}
