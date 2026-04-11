import { Component, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ProductService } from '../../../services/product.service';

@Component({
  selector: 'app-consumer-history',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,     // ✅ Required for ngModel
    DecimalPipe,     // ✅ Will be used in HTML
    DatePipe         // ✅ Will be used in HTML
  ],
  templateUrl: './consumer-history.component.html',
})
export class ConsumerHistoryComponent implements OnInit {

  filterStatus = 'All';
  searchQuery = '';

  allOrders: any[] = [];
  filteredOrders: any[] = [];
  loading = true;

  constructor(private productService: ProductService) { }

  ngOnInit(): void {
    this.productService.getConsumerHistory().subscribe({
      next: (data) => {
        this.allOrders = data;
        this.filteredOrders = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load history', err);
        this.loading = false;
      }
    });
  }

  filter(): void {
    this.filteredOrders = this.allOrders.filter(order => {
      const matchStatus =
        this.filterStatus === 'All' || order.status === this.filterStatus;

      const matchSearch =
        (order.items && order.items.some((item: any) =>
          item.name.toLowerCase().includes(this.searchQuery.toLowerCase())
        )) ||
        order.vendor.toLowerCase().includes(this.searchQuery.toLowerCase());

      return matchStatus && matchSearch;
    });
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'Delivered':
        return 'bg-emerald-100 text-emerald-700';
      case 'Processing':
        return 'bg-blue-100 text-blue-700';
      case 'Cancelled':
        return 'bg-red-100 text-red-700';
      default:
        return 'bg-gray-100 text-gray-700';
    }
  }
}
