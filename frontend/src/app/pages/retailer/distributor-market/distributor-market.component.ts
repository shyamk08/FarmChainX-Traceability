import { Component, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

@Component({
    selector: 'app-distributor-market',
    standalone: true,
    imports: [CommonModule, DecimalPipe, RouterModule],
    templateUrl: './distributor-market.component.html'
})
export class DistributorMarketComponent implements OnInit {
    marketItems: any[] = [];
    isLoading = true;
    purchasingId: number | null = null;

    constructor(private http: HttpClient) { }

    ngOnInit(): void {
        this.fetchMarket();
    }

    fetchMarket() {
        this.isLoading = true;
        this.http.get<any[]>(`${environment.apiUrl}/track/market/distributors`).subscribe({
            next: (data) => {
                this.marketItems = data;
                this.isLoading = false;
            },
            error: (err) => {
                console.error('Error fetching market:', err);
                this.isLoading = false;
            }
        });
    }

    placeOrder(item: any) {
        if (confirm(`Place order for ${item.quantity} ${item.unit} of ${item.cropName} from ${item.distributor}?`)) {
            this.purchasingId = item.id;

            const payload = {
                supplierId: item.distributorId,
                quantity: item.quantity,
                total: item.quantity * item.pricePerUnit
            };

            this.http.post(`${environment.apiUrl}/retailer/orders/create`, payload).subscribe({
                next: (res: any) => {
                    alert(`✅ Order Placed Successfully!\n\nOrder ID: PO-${res.orderId}\nSupplier: ${item.distributor}`);
                    this.purchasingId = null;
                    // Refresh market to reflect changes if necessary
                    this.fetchMarket();
                },
                error: (err) => {
                    console.error('Order failed', err);
                    alert('Failed to place order.');
                    this.purchasingId = null;
                }
            });
        }
    }
}
