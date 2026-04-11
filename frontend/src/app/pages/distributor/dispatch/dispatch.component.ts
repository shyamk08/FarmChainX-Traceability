import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ProductService } from '../../../services/product.service';

@Component({
    selector: 'app-dispatch',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './dispatch.component.html',
})
export class DispatchComponent implements OnInit {
    dispatchableItems: any[] = [];
    dispatchHistory: any[] = [];
    selectedProduct: any = null; // Product selected from inventory

    // Dispatch statistics
    totalDispatches = 0;
    dispatchesToday = 0;

    // Filter and search
    searchQuery = '';
    statusFilter = 'all'; // all, available, dispatched

    constructor(
        private productService: ProductService,
        private router: Router
    ) {
        // Check if navigation state contains a selected product
        const navigation = this.router.getCurrentNavigation();
        if (navigation?.extras?.state) {
            this.selectedProduct = navigation.extras.state['selectedProduct'];
        }
    }

    ngOnInit() {
        this.loadDispatchableItems();
        this.loadDispatchHistory();
    }

    loadDispatchableItems() {
        console.log('🔄 Loading dispatchable items...');
        this.productService.getDistributorInventory().subscribe({
            next: (items) => {
                this.dispatchableItems = items || [];
                console.log('✅ Loaded dispatchable items:', this.dispatchableItems.length);
            },
            error: (err) => {
                console.error('❌ Error loading items:', err);
            }
        });
    }

    loadDispatchHistory() {
        console.log('🔄 Loading dispatch history...');
        this.productService.getDispatchHistory().subscribe({
            next: (history) => {
                this.dispatchHistory = history || [];
                console.log('✅ Loaded dispatch history:', this.dispatchHistory.length);

                // Calculate total dispatches
                this.totalDispatches = this.dispatchHistory.length;

                // Calculate dispatches today
                const today = new Date();
                today.setHours(0, 0, 0, 0);

                this.dispatchesToday = this.dispatchHistory.filter(dispatch => {
                    const dispatchDate = new Date(dispatch.timestamp || dispatch.createdAt);
                    dispatchDate.setHours(0, 0, 0, 0);
                    return dispatchDate.getTime() === today.getTime();
                }).length;

                console.log(`📊 Total: ${this.totalDispatches}, Today: ${this.dispatchesToday}`);
            },
            error: (err) => {
                console.error('❌ Error loading dispatch history:', err);
                // Set defaults on error
                this.dispatchHistory = [];
                this.totalDispatches = 0;
                this.dispatchesToday = 0;
            }
        });
    }

    get filteredItems() {
        return this.dispatchableItems.filter(item => {
            const matchesSearch = !this.searchQuery ||
                item.cropName?.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
                item.productId?.toString().includes(this.searchQuery);

            return matchesSearch;
        });
    }

    openDispatch(item: any) {
        console.log('📦 Navigating to retailer selection for item:', item);
        // Navigate to retailer selection page with product data
        this.router.navigate(['/distributor/retailer-selection'], {
            state: { productToDispatch: item }
        });
    }

    getStatusBadgeClass(status: string): string {
        if (status === 'Dispatched') return 'bg-green-100 text-green-800';
        if (status === 'In Transit') return 'bg-blue-100 text-blue-800';
        if (status === 'Available') return 'bg-emerald-100 text-emerald-800';
        return 'bg-gray-100 text-gray-800';
    }
}
