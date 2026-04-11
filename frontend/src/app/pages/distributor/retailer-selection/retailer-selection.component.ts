import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ProductService } from '../../../services/product.service';

@Component({
    selector: 'app-retailer-selection',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './retailer-selection.component.html',
})
export class RetailerSelectionComponent implements OnInit {
    productToDispatch: any = null;
    retailers: any[] = [];
    selectedRetailerId: number | null = null;
    dispatchLocation = 'Distributor Warehouse → Retailer';
    loading = false;
    dispatching = false;

    constructor(
        private productService: ProductService,
        private router: Router
    ) {
        // Get product from navigation state
        const navigation = this.router.getCurrentNavigation();
        if (navigation?.extras?.state) {
            this.productToDispatch = navigation.extras.state['productToDispatch'];
        }
    }

    ngOnInit() {
        if (!this.productToDispatch) {
            // If no product selected, redirect back to dispatch
            alert('⚠️ No product selected for dispatch');
            this.router.navigate(['/distributor/dispatch']);
            return;
        }
        this.loadRetailers();
    }

    loadRetailers() {
        this.loading = true;
        this.productService.getRetailers().subscribe({
            next: (retailers) => {
                this.retailers = retailers || [];
                this.loading = false;
            },
            error: (err) => {
                console.error('Error loading retailers:', err);
                alert('Failed to load retailers');
                this.loading = false;
            }
        });
    }

    selectRetailer(retailerId: number) {
        this.selectedRetailerId = retailerId;
    }

    dispatchToRetailer() {
        if (!this.selectedRetailerId) {
            alert('⚠️ Please select a retailer first');
            return;
        }

        if (!this.productToDispatch) {
            alert('⚠️ No product selected for dispatch');
            return;
        }

        this.dispatching = true;
        console.log('📤 Dispatching product to retailer...');
        console.log('Product:', this.productToDispatch);
        console.log('Retailer ID:', this.selectedRetailerId);

        this.productService.handoverToRetailer(
            this.productToDispatch.productId,
            this.selectedRetailerId,
            this.dispatchLocation
        ).subscribe({
            next: (response) => {
                console.log('✅ Dispatch successful!', response);
                alert(`✅ Product successfully dispatched to retailer!`);
                this.dispatching = false;
                // Navigate back to inventory or dispatch
                this.router.navigate(['/distributor/inventory']);
            },
            error: (err) => {
                console.error('❌ Dispatch failed:', err);
                const errorMsg = err.error?.error || err.error?.message || err.message || 'Unknown error';
                alert('❌ Dispatch failed: ' + errorMsg);
                this.dispatching = false;
            }
        });
    }

    cancel() {
        this.router.navigate(['/distributor/dispatch']);
    }
}
