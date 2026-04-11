import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ProductService } from '../../../services/product.service';
import { FormsModule } from '@angular/forms';

@Component({
  standalone: true,
  imports: [CommonModule, DatePipe, FormsModule],
  selector: 'app-retailer-shipments',
  templateUrl: './retailer-shipments.component.html',
})
export class RetailerShipmentsComponent implements OnInit {
  shipments: any[] = [];
  dispatchOffers: any[] = [];
  loading = true;
  offersLoading = true;
  verifyingId: any = null;
  verificationChecked = false;

  constructor(private productService: ProductService) { }

  ngOnInit() {
    this.fetchShipments();
    this.fetchDispatchOffers();
  }

  fetchShipments() {
    this.loading = true;
    this.productService.getPendingShipments().subscribe({
      next: (data) => {
        // Backend returns Page object
        this.shipments = data.content || [];
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load shipments', err);
        this.loading = false;
      }
    });
  }

  fetchDispatchOffers() {
    this.offersLoading = true;
    this.productService.getRetailerOffers().subscribe({
      next: (offers) => {
        this.dispatchOffers = offers || [];
        console.log('📦 Loaded dispatch offers:', this.dispatchOffers.length);
        this.offersLoading = false;
      },
      error: (err) => {
        console.error('Failed to load dispatch offers', err);
        this.offersLoading = false;
      }
    });
  }

  acceptOffer(offer: any) {
    const confirmed = confirm(`Accept dispatch offer for ${offer.cropName} from ${offer.distributorName}?`);
    if (!confirmed) return;

    const location = 'Retailer Warehouse';
    this.productService.acceptOffer(offer.offerId, location).subscribe({
      next: (response) => {
        alert('✅ Dispatch offer accepted! Product will appear in pending shipments.');
        this.fetchDispatchOffers();
        this.fetchShipments();
      },
      error: (err) => {
        const errorMsg = err.error?.error || err.message || 'Failed to accept offer';
        alert('❌ Error: ' + errorMsg);
        this.fetchDispatchOffers(); // Refresh in case it was already accepted
      }
    });
  }

  startVerification(id: any) {
    this.verifyingId = id;
    this.verificationChecked = false;
  }

  cancelVerification() {
    this.verifyingId = null;
    this.verificationChecked = false;
  }

  confirmReceipt(shipment: any) {
    if (!this.verificationChecked) {
      return;
    }

    const location = "Retailer Store (Received)";
    this.productService.confirmReceipt(shipment.productId, location).subscribe({
      next: () => {
        alert('✅ Receipt Confirmed! Product is now in your Inventory.');
        this.fetchShipments();
        this.fetchDispatchOffers();
        this.verifyingId = null;
      },
      error: (err) => {
        alert('Error confirming receipt: ' + err.error?.error || err.message);
      }
    });
  }
}
