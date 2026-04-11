import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';

interface Order {
  id: string;
  supplier: string;
  items: number;
  total: number;
  createdAt: string;
  status: string;
}

import { FormsModule } from '@angular/forms';

@Component({
  standalone: true,
  imports: [CommonModule, FormsModule],
  selector: 'app-retailer-orders',
  templateUrl: './retailer-orders.component.html',
})
export class RetailerOrdersComponent {
  orders: Order[] = [];
  showModal = false;
  newOrder: any = {
    supplierId: '',
    items: '',
    total: 0
  };

  constructor(private http: HttpClient) {
    this.fetchOrders();
  }

  fetchOrders() {
    this.http.get<any[]>('/api/retailer/orders/all').subscribe({
      next: (data) => {
        this.orders = data.map(o => ({
          id: 'PO-' + o.id,
          supplier: 'Supplier ' + o.supplierId,
          items: o.items,
          total: o.totalAmount,
          createdAt: o.createdAt,
          status: o.status
        }));
      },
      error: (err) => console.error('Failed to load orders', err)
    });
  }

  statusClass(s: string) {
    switch (s) {
      case 'Delivered':
        return 'bg-emerald-100 text-emerald-800';
      case 'Shipped':
        return 'bg-sky-100 text-sky-800';
      case 'Processing':
        return 'bg-yellow-100 text-yellow-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  }

  view(o: Order) {
    // open details modal or navigate to PO detail view (future)
    alert(`Open PO: ${o.id}`);
  }

  openCreateModal() {
    this.showModal = true;
  }

  closeCreateModal() {
    this.showModal = false;
  }

  createOrder() {
    // Mock creation
    const mockOrder: Order = {
      id: 'PO-' + Math.floor(Math.random() * 1000),
      supplier: 'Supplier ' + (this.newOrder.supplierId || 'Unknown'),
      items: parseInt(this.newOrder.items) || 0,
      total: this.newOrder.total,
      createdAt: new Date().toISOString(),
      status: 'Processing'
    };

    this.orders.unshift(mockOrder);
    this.closeCreateModal();
    alert('Purchase Order Created!');
  }
}
