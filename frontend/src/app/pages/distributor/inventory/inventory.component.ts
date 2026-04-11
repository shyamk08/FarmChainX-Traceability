import { Component, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { ProductService } from '../../../services/product.service';

@Component({
  selector: 'app-inventory',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './inventory.component.html',
})
export class InventoryComponent implements OnInit {
  inventoryItems: any[] = [];
  totalValue = 0;

  constructor(
    private productService: ProductService,
    private router: Router
  ) { }

  ngOnInit() {
    this.loadInventory();
  }

  loadInventory() {
    this.productService.getDistributorInventory().subscribe(items => {
      this.inventoryItems = items || [];
      this.totalValue = this.inventoryItems.reduce((acc, item) => acc + (item.value || 0), 0);
    });
  }

  getGradeColor(grade: string): string {
    if (!grade) return 'bg-gray-100 text-gray-800';
    if (grade.includes('A')) return 'bg-emerald-100 text-emerald-800';
    if (grade.includes('B')) return 'bg-yellow-100 text-yellow-800';
    return 'bg-orange-100 text-orange-800';
  }

  getStatusColor(status: string): string {
    if (status === 'In Stock') return 'bg-blue-100 text-blue-800';
    if (status === 'Reserved') return 'bg-purple-100 text-purple-800';
    if (status === 'Low Stock') return 'bg-red-100 text-red-800';
    return 'bg-gray-100 text-gray-800';
  }

  goToDispatch(item: any) {
    // Navigate to dispatch page with selected product data
    this.router.navigate(['/distributor/dispatch'], {
      state: { selectedProduct: item }
    });
  }
}
