import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

interface InventoryItem {
  productId: string;
  name: string;
  batchId: string;
  qtyOnHand: number;
  unit: string;
  costPrice: number;
  sellPrice: number;
  expiryDate?: string;
  supplier: string;
}

@Component({
  standalone: true,
  imports: [CommonModule],
  selector: 'app-retailer-inventory',
  templateUrl: './retailer-inventory.component.html',
})
export class RetailerInventoryComponent {
  lowThreshold = 20;
  items: InventoryItem[] = [];
  activeQrItem: InventoryItem | null = null;
  qrUrl: string = '';

  constructor(private http: HttpClient) {
    this.fetchInventory();
  }

  fetchInventory() {
    this.http.get<any[]>(`${environment.apiUrl}/track/retailer/inventory`).subscribe({
      next: (data) => {
        this.items = data;
      },
      error: (err) => console.error('Failed to load inventory', err)
    });
  }

  openQr(item: InventoryItem) {
    this.activeQrItem = item;
    // Link to public verification page
    const verifyUrl = `${window.location.origin}/verify/${item.productId}`;
    this.qrUrl = `https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(verifyUrl)}`;
  }

  closeQr() {
    this.activeQrItem = null;
    this.qrUrl = '';
  }

  sell(item: InventoryItem) {
    if (confirm(`Mark 1 unit of ${item.name} as sold?`)) {
      this.http.post(`${environment.apiUrl}/retailer/sell`, { productId: item.productId }).subscribe({
        next: () => {
          alert(`✅ Sold 1 unit of ${item.name}. Inventory updated.`);
          this.fetchInventory(); // Refresh list to remove item
        },
        error: (err) => {
          alert('Error processing sale: ' + (err.error?.message || "Unknown error"));
          console.error(err);
        }
      });
    }
  }

  exportCsv() {
    // lightweight CSV export of current items (dummy)
    const header = [
      'productId',
      'name',
      'batchId',
      'qtyOnHand',
      'unit',
      'costPrice',
      'sellPrice',
      'supplier',
      'expiryDate',
    ];
    const csv = [header.join(',')]
      .concat(
        this.items.map((i) =>
          [
            i.productId,
            i.name,
            i.batchId,
            i.qtyOnHand,
            i.unit,
            i.costPrice,
            i.sellPrice,
            i.supplier,
            i.expiryDate || '',
          ]
            .map((v) => `"${v}"`)
            .join(',')
        )
      )
      .join('\n');

    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'inventory.csv';
    a.click();
    URL.revokeObjectURL(url);
  }
}
