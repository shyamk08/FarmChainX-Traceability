import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { AdminService, AdminOverview as AdminOverviewData } from '../../../services/admin.service';

@Component({
  standalone: true,
  imports: [CommonModule, DatePipe],
  templateUrl: './admin-overview.html',
  styleUrl: './admin-overview.scss',
  selector: 'app-admin-overview'
})
export class AdminOverview implements OnInit {
  data?: AdminOverviewData;
  isLoading: boolean = false;
  lastUpdated: Date = new Date();

  constructor(private adminService: AdminService) { }

  ngOnInit(): void {
    this.refreshData();
  }

  refreshData(): void {
    this.isLoading = true;
    this.adminService.getOverview().subscribe({
      next: (res) => {
        this.data = res;
        this.lastUpdated = new Date();
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load overview data', err);
        this.isLoading = false;
      }
    });
  }

  formatCurrency(amount: number | undefined): string {
    if (amount === null || amount === undefined) return '$0';
    return amount.toLocaleString('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 });
  }

  formatRating(rating: number | undefined): string {
    if (rating === null || rating === undefined) return '0.00';
    return rating.toFixed(2);
  }
}