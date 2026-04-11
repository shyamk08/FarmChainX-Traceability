import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  standalone: true,
  selector: 'app-procurement-detail',
  imports: [CommonModule],
  templateUrl: './procurement-detail.component.html',
})
export class ProcurementDetailComponent {
  procurement = {
    farmer: 'Ramesh Kumar',
    crop: 'Wheat',
    quantity: '25 tons',
    pricePerTon: '₹32,000',
    totalAmount: '₹8,00,000',
    warehouse: 'Warehouse A',
    status: 'Completed',
    date: '12 Dec 2025',
  };
}
