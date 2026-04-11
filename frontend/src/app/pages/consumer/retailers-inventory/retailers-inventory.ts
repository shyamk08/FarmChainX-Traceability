import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProductService } from '../../../services/product.service';
import { CartService } from '../../../services/cart.service';
import { CartSidebarComponent } from '../cart-sidebar/cart-sidebar.component';

@Component({
  selector: 'app-retailers-inventory',
  imports: [CommonModule, CartSidebarComponent],
  templateUrl: './retailers-inventory.html',
  styleUrl: './retailers-inventory.scss',
  standalone: true
})
export class RetailersInventory implements OnInit {
  products = signal<any[]>([]);
  isCartOpen = signal(false);
  isLoading = signal(true);

  constructor(
    private productService: ProductService,
    public cartService: CartService
  ) { }

  ngOnInit() {
    this.loadRetailersInventory();
  }

  loadRetailersInventory() {
    this.isLoading.set(true);
    this.productService.getConsumerMarketProducts().subscribe({
      next: (data) => {
        // Filter to show only IN_STOCK products
        const available = (data || []).filter(p => p.status === 'IN_STOCK' && p.quantity > 0);
        this.products.set(available);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load retailers inventory', err);
        this.isLoading.set(false);
      }
    });
  }

  addToCart(product: any) {
    this.cartService.addToCart(product);
    this.isCartOpen.set(true);
  }

  getQualityClass(grade: string): string {
    if (!grade) return 'text-slate-600';
    if (grade.includes('A')) return 'text-emerald-600';
    if (grade.includes('B')) return 'text-yellow-600';
    return 'text-orange-600';
  }

  getStockStatusClass(quantity: number): string {
    if (quantity <= 0) return 'bg-red-100 text-red-700';
    if (quantity < 10) return 'bg-yellow-100 text-yellow-700';
    return 'bg-green-100 text-green-700';
  }

  getStockStatusText(quantity: number): string {
    if (quantity <= 0) return 'Out of Stock';
    if (quantity < 10) return 'Low Stock';
    return 'In Stock';
  }
}

