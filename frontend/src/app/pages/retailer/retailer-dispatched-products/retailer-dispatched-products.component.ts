import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProductService } from '../../../services/product.service';

@Component({
    selector: 'app-retailer-dispatched-products',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './retailer-dispatched-products.component.html',
})
export class RetailerDispatchedProductsComponent implements OnInit {
    dispatchOffers = signal<any[]>([]);
    loading = signal(false);
    error = signal<string | null>(null);

    constructor(private productService: ProductService) { }

    ngOnInit() {
        this.loadDispatchedProducts();
    }

    loadDispatchedProducts() {
        this.loading.set(true);
        this.error.set(null);

        this.productService.getRetailerOffers().subscribe({
            next: (offers) => {
                this.dispatchOffers.set(offers);
                this.loading.set(false);
            },
            error: (err) => {
                console.error('Error loading dispatch offers:', err);
                this.error.set('Failed to load dispatch offers');
                this.loading.set(false);
            },
        });
    }

    acceptOffer(offer: any) {
        if (!confirm(`Accept dispatch offer for ${offer.cropName || 'this product'}?`)) {
            return;
        }

        this.productService.acceptOffer(offer.offerId, 'Retailer Warehouse').subscribe({
            next: (response) => {
                console.log('Offer accepted:', response);
                alert(`Successfully accepted ${offer.cropName}! Product added to your inventory.`);
                // Reload the list to remove accepted offer
                this.loadDispatchedProducts();
            },
            error: (err) => {
                console.error('Error accepting offer:', err);
                const errorMsg = err.error?.error || 'Failed to accept offer';
                alert(errorMsg);
            },
        });
    }

    rejectOffer(offer: any) {
        if (!confirm(`Reject dispatch offer for ${offer.cropName || 'this product'}?`)) {
            return;
        }

        this.productService.rejectOffer(offer.offerId).subscribe({
            next: (response) => {
                console.log('Offer rejected:', response);
                alert(`Dispatch offer for ${offer.cropName} has been rejected.`);
                // Reload the list to remove rejected offer
                this.loadDispatchedProducts();
            },
            error: (err) => {
                console.error('Error rejecting offer:', err);
                const errorMsg = err.error?.error || 'Failed to reject offer';
                alert(errorMsg);
            },
        });
    }

    getOfferAge(createdAt: string): string {
        const now = new Date();
        const created = new Date(createdAt);
        const diffMs = now.getTime() - created.getTime();
        const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
        const diffDays = Math.floor(diffHours / 24);

        if (diffDays > 0) {
            return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
        } else if (diffHours > 0) {
            return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
        } else {
            return 'Just now';
        }
    }
}
