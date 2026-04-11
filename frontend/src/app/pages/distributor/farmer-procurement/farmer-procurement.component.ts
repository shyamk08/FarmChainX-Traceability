import { Component, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ProductService } from '../../../services/product.service';
import { FindPipe } from '../../../pipes/find.pipe';

@Component({
  selector: 'app-farmer-procurement',
  standalone: true,
  imports: [CommonModule, DecimalPipe, DatePipe, FindPipe],
  templateUrl: './farmer-procurement.component.html'
})
export class FarmerProcurementComponent implements OnInit {
  availableCrops: any[] = [];
  loading = true;
  buyingId: number | null = null;
  confirmingId: number | null = null;
  toastMessage: string = '';
  toastType: 'success' | 'error' | '' = '';

  constructor(private http: HttpClient, private productService: ProductService) { }

  ngOnInit(): void {
    this.loadMarketplace();
  }

  loadMarketplace() {
    this.loading = true;
    this.productService.getMarketProducts().subscribe({
      next: (res) => {
        // Filter out sold-out products - only show available products
        this.availableCrops = (res || []).filter(crop => !crop.isSold);
        this.loading = false;
      },
      error: (err) => {
        console.error('Marketplace error', err);
        this.loading = false;
      }
    });
  }

  // Show confirmation for product
  showConfirmation(crop: any) {
    this.confirmingId = crop.id;
  }

  // Cancel confirmation
  cancelPurchase() {
    this.confirmingId = null;
  }

  // Confirm and proceed with purchase
  confirmPurchase(crop: any) {
    this.confirmingId = null;
    this.buyingId = crop.id;

    const location = "Distributor Warehouse (Initial)";
    this.productService.pickupProduct(crop.id, location).subscribe({
      next: (res) => {
        this.showToast(`✅ Successfully Acquired! Ownership of Batch #${crop.id} transferred to you.`, 'success');
        // Refresh list to remove the item
        this.loadMarketplace();
        this.buyingId = null;
      },
      error: (err) => {
        console.error(err);
        this.showToast("❌ Failed to acquire product: " + (err.error?.error || err.message), 'error');
        this.buyingId = null;
      }
    });
  }

  // Legacy method for backward compatibility (direct purchase)
  buyCrop(crop: any) {
    this.showConfirmation(crop);
  }

  showToast(message: string, type: 'success' | 'error') {
    this.toastMessage = message;
    this.toastType = type;
    setTimeout(() => {
      this.toastMessage = '';
      this.toastType = '';
    }, 5000);
  }

  estimatePrice(crop: any): number {
    if (crop.price && crop.price > 0) return crop.price;
    // Mock price calc based on quality (fallback)
    let base = 2000;
    if (crop.qualityGrade?.includes('A')) base *= 1.5;
    if (crop.qualityGrade?.includes('B')) base *= 1.2;
    return base;
  }

  getQualityColor(grade: string): string {
    if (!grade) return 'bg-gray-100 text-gray-800';
    const g = grade.toUpperCase();
    if (g.includes('A') || g.includes('A+')) return 'bg-emerald-100 text-emerald-800';
    if (g.includes('B')) return 'bg-yellow-100 text-yellow-800';
    return 'bg-orange-100 text-orange-800';
  }

  // Helper method to get the confirming crop
  getConfirmingCrop(): any {
    if (this.confirmingId === null) return null;
    return this.availableCrops.find(crop => crop.id === this.confirmingId);
  }
}
