import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
    providedIn: 'root'
})
export class ProductService {
    private apiUrl = environment.apiUrl;

    constructor(private http: HttpClient) { }

    uploadProduct(formData: FormData): Observable<any> {
        return this.http.post<any>(`${this.apiUrl}/products/upload`, formData);
    }

    getMyProducts(page: number = 0, size: number = 9): Observable<any> {
        let params = new HttpParams()
            .set('page', page.toString())
            .set('size', size.toString());
        return this.http.get<any>(`${this.apiUrl}/products/my`, { params });
    }

    getMarketProducts(): Observable<any[]> {
        return this.http.get<any[]>(`${this.apiUrl}/products/market`);
    }

    getConsumerMarketProducts(): Observable<any[]> {
        return this.http.get<any[]>(`${this.apiUrl}/products/consumer`);
    }

    pickupProduct(productId: number, location: string): Observable<any> {
        return this.http.post<any>(`${this.apiUrl}/track/update-chain`, {
            productId,
            location,
            notes: 'Distributor collected from farmer',
            toUserId: null // Not needed for first pickup
        });
    }

    getDistributorInventory(): Observable<any[]> {
        return this.http.get<any[]>(`${this.apiUrl}/track/inventory`);
    }

    getRetailers(): Observable<any[]> {
        return this.http.get<any[]>(`${this.apiUrl}/track/users/retailers`);
    }

    handoverToRetailer(productId: number, retailerId: number, location: string): Observable<any> {
        return this.http.post<any>(`${this.apiUrl}/track/update-chain`, {
            productId,
            toUserId: retailerId,
            location: location,
            notes: 'Handover to Retailer'
        });
    }

    getPendingShipments(): Observable<any> {
        return this.http.get<any>(`${this.apiUrl}/track/pending`);
    }

    confirmReceipt(productId: number, location: string): Observable<any> {
        return this.http.post<any>(`${this.apiUrl}/track/update-chain`, {
            productId,
            location,
            notes: 'Retailer Confirmed Receipt',
            // Backend logic knows if I am a retailer confirming, I don't need to send toUserId
        });
    }

    verifyProduct(uuid: string): Observable<any> {
        return this.http.get<any>(`${this.apiUrl}/verify/${uuid}`);
    }

    getDistributorStats(): Observable<any> {
        return this.http.get<any>(`${this.apiUrl}/track/dashboard/distributor`);
    }

    purchaseProduct(productId: number, location: string): Observable<any> {
        return this.http.post<any>(`${this.apiUrl}/track/purchase`, {
            productId,
            location
        });
    }

    getConsumerHistory(): Observable<any[]> {
        return this.http.get<any[]>(`${this.apiUrl}/track/consumer/history`);
    }

    getNotifications(): Observable<any[]> {
        return this.http.get<any[]>(`${this.apiUrl}/notifications`);
    }

    submitFeedback(productId: number, feedback: { rating: number, comment: string }): Observable<any> {
        return this.http.post<any>(`${this.apiUrl}/products/${productId}/feedback`, feedback);
    }

    // Broadcast Dispatch Methods
    broadcastDispatch(productId: number, location: string, notes?: string): Observable<any> {
        return this.http.post<any>(`${this.apiUrl}/track/broadcast-dispatch`, {
            productId,
            location,
            notes: notes || 'Product dispatched to all retailers'
        });
    }

    getRetailerOffers(): Observable<any[]> {
        return this.http.get<any[]>(`${this.apiUrl}/retailer/dispatch-offers`);
    }

    acceptOffer(offerId: number, location: string): Observable<any> {
        return this.http.post<any>(`${this.apiUrl}/retailer/accept-offer/${offerId}`, {
            location
        });
    }

    rejectOffer(offerId: number): Observable<any> {
        return this.http.post<any>(`${this.apiUrl}/retailer/reject-offer/${offerId}`, {});
    }

    // Get dispatch history for distributor
    getDispatchHistory(): Observable<any[]> {
        return this.http.get<any[]>(`${this.apiUrl}/track/dispatch-history`);
    }
}
