import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProductService } from '../../../services/product.service';
import { CartService } from '../../../services/cart.service';
import { CartSidebarComponent } from '../cart-sidebar/cart-sidebar.component';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-retailers-inventory',
  imports: [CommonModule, CartSidebarComponent, TranslateModule],
  templateUrl: './retailers-inventory.html',
  styleUrl: './retailers-inventory.scss',
  standalone: true
})
export class RetailersInventory implements OnInit {
  products = signal<any[]>([]);
  isCartOpen = signal(false);
  isLoading = signal(true);
  aiGrades: Record<number, any> = {};

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
        const available = (data || []).filter(p => p.status === 'IN_STOCK' && p.quantity > 0);
        this.products.set(available);
        this.isLoading.set(false);
        // Auto-grade each product
        available.forEach(p => this.fetchAIGrade(p));
      },
      error: (err) => {
        console.error('Failed to load retailers inventory', err);
        this.isLoading.set(false);
      }
    });
  }

  fetchAIGrade(product: any) {
    const payload: any = {};
    if (product.imagePath) {
      payload.imageUrl = product.imagePath;
    } else {
      payload.colorScore = 75;
      payload.freshnessScore = 80;
      payload.sizeScore = 70;
    }
    if (product.price) payload.price = product.price;

    this.productService.gradeProduct(payload).subscribe({
      next: (result) => { this.aiGrades[product.id] = result; },
      error: () => { /* silently skip - AI grade is optional */ }
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
    if (quantity <= 0) return 'OUT_OF_STOCK';
    if (quantity < 10) return 'LOW_STOCK';
    return 'IN_STOCK';
  }

  getAIGradeBadgeClass(grade: string): string {
    if (!grade) return 'text-slate-600 bg-slate-50 border-slate-200';
    if (grade === 'A') return 'text-emerald-700 bg-emerald-50 border-emerald-200';
    if (grade === 'B') return 'text-yellow-700 bg-yellow-50 border-yellow-200';
    return 'text-red-700 bg-red-50 border-red-200';
  }
}

