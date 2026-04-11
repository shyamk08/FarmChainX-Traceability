import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { environment } from '../../../environments/environment';

type Mode = 'update' | 'handover' | null;

@Component({
  selector: 'app-verify-product',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './verify-product.html',
  styleUrls: ['./verify-product.scss']
})
export class VerifyProduct implements OnInit {
  product: any = null;
  loading = true;
  error = '';
  uuid = '';
  showAddForm = false;
  newNote = '';
  newLocation = '';
  retailers: any[] = [];
  selectedRetailerId: number | null = null;
  currentUserId: number | null = null;
  displayLocation = '';
  mode: Mode = null;

  userRole: 'Guest' | 'Farmer' | 'Distributor' | 'Retailer' | 'Admin' | 'Consumer' = 'Guest';

  feedbacks: any[] = [];
  consumerCanGiveFeedback = false;
  myRating = 5;
  myComment = '';

  constructor(private route: ActivatedRoute, private http: HttpClient) { }

  ngOnInit(): void {
    this.uuid = this.route.snapshot.paramMap.get('uuid')!;
    this.readRoleFromToken();
    this.loadProduct();
  }

  private readRoleFromToken() {
    const token = localStorage.getItem('fcx_token') || localStorage.getItem('token') || localStorage.getItem('jwt');
    const storedRole = localStorage.getItem('fcx_role') || localStorage.getItem('role');

    if (storedRole) {
      const rn = storedRole.toUpperCase().replace(/^ROLE_/, '');
      if (rn.includes('DISTRIBUTOR')) this.userRole = 'Distributor';
      else if (rn.includes('RETAILER')) this.userRole = 'Retailer';
      else if (rn.includes('FARMER')) this.userRole = 'Farmer';
      else if (rn.includes('ADMIN')) this.userRole = 'Admin';
      else if (rn.includes('CONSUMER')) this.userRole = 'Consumer';
      else this.userRole = 'Guest';
    }

    if (!token) return;

    try {
      const parts = token.split('.');
      if (parts.length < 2) return;
      const payload = JSON.parse(atob(parts[1].replace(/-/g, '+').replace(/_/g, '/')));
      this.currentUserId = payload.userId || payload.id || payload.sub || payload.user?.id || null;
      if (this.userRole === 'Distributor') this.loadRetailers();
    } catch { }
  }

  private loadProduct() {
    this.loading = true;
    this.http.get(`${environment.apiUrl}/verify/${this.uuid}`).subscribe({
      next: (data: any) => {
        if (Array.isArray(data?.trackingHistory)) {
          data.trackingHistory = [...data.trackingHistory].sort(
            (a: any, b: any) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
          );
        }
        this.product = data;
        this.displayLocation = data.displayLocation || data.gpsLocation || 'Protected Location';
        this.consumerCanGiveFeedback = (this.userRole === 'Consumer') && (data.canGiveFeedback === true);
        this.loading = false;
        this.loadFeedbacks();
      },
      error: () => {
        this.error = 'Invalid or expired QR code • Product not found';
        this.loading = false;
      }
    });
  }

  private loadRetailers() {
    this.http.get<any[]>(`${environment.apiUrl}/track/users/retailers`).subscribe({
      next: (data) => {
        this.retailers = data;
      }
    });
  }

  private loadFeedbacks() {
    if (!this.product?.productId) return;
    this.http.get<any[]>(`${environment.apiUrl}/products/${this.product.productId}/feedbacks`).subscribe({
      next: (list) => {
        this.feedbacks = list || [];
      }
    });
  }

  private latestLog() {
    return this.product?.trackingHistory?.length
      ? this.product.trackingHistory[this.product.trackingHistory.length - 1]
      : null;
  }

  private myIdNum(): number | null {
    return this.currentUserId != null ? Number(this.currentUserId) : null;
  }

  private isCurrentOwner(): boolean {
    const myId = this.myIdNum();
    const last = this.latestLog();
    if (!myId || !last) return false;
    const to = last.toUserId != null ? Number(last.toUserId) : null;
    return to === myId && last.confirmed === true;
  }

  private isUnclaimed(): boolean {
    const last = this.latestLog();
    if (!last) return true;
    const from = last.fromUserId != null ? Number(last.fromUserId) : null;
    const to = last.toUserId != null ? Number(last.toUserId) : null;
    const confirmed = last.confirmed === true;
    return from === null && to === null && confirmed === false;
  }

  hasDistributorTakenPossession(): boolean {
    if (this.userRole !== 'Distributor') return false;
    return this.isCurrentOwner();
  }

  showUpdatePossible(): boolean {
    const myId = this.myIdNum();
    const last = this.latestLog();

    if (this.userRole === 'Distributor') {
      if (!last) return true;
      const to = last.toUserId != null ? Number(last.toUserId) : null;
      const from = last.fromUserId != null ? Number(last.fromUserId) : null;
      const confirmed = last.confirmed === true;

      const unclaimed = from === null && to === null && confirmed === false;
      if (unclaimed) return true;

      const isOwnerNow = to === myId && confirmed;
      const handedToRetailer = to !== null && to !== myId;
      return isOwnerNow && !handedToRetailer;
    }

    if (this.userRole === 'Retailer') {
      if (!last || !myId) return false;
      const to = last.toUserId != null ? Number(last.toUserId) : null;
      const from = last.fromUserId != null ? Number(last.fromUserId) : null;
      return to === myId && from !== null && last.confirmed !== true;
    }

    return false;
  }

  openForm(which: Mode) {
    if (!this.showUpdatePossible()) return;
    this.mode = which;
    this.showAddForm = true;
    if (which === 'handover' && this.userRole === 'Distributor' && this.retailers.length === 0) {
      this.loadRetailers();
    }
  }

  cancelForm() {
    this.showAddForm = false;
    this.mode = null;
    this.newLocation = '';
    this.newNote = '';
    this.selectedRetailerId = null;
  }

  submitChainUpdate() {
    if (!this.showUpdatePossible()) {
      alert('Action not allowed');
      return;
    }
    if (!this.newLocation.trim()) {
      alert('Location is required');
      return;
    }
    if (this.mode === 'handover' && !this.selectedRetailerId) {
      alert('Please select a retailer');
      return;
    }

    const payload: any = {
      productId: this.product.productId,
      location: this.newLocation.trim(),
      notes: this.newNote.trim() || undefined
    };

    if (this.mode === 'handover') {
      payload.toUserId = this.selectedRetailerId;
    }

    this.http.post(`${environment.apiUrl}/track/update-chain`, payload).subscribe({
      next: (res: any) => {
        alert(res?.message || 'Success!');
        this.cancelForm();
        this.loadProduct();
      },
      error: (err) => {
        alert(err.error?.error || 'Failed');
      }
    });
  }

  firstClaimNow() {
    if (this.userRole !== 'Distributor') { alert('Only distributor can claim first.'); return; }
    if (!this.isUnclaimed()) { alert('Not in unclaimed state.'); return; }
    const payload = {
      productId: this.product.productId,
      location: this.newLocation.trim() || 'Distributor received from farmer',
      notes: this.newNote.trim() || 'Distributor received from farmer'
    };
    this.http.post(`${environment.apiUrl}/track/update-chain`, payload).subscribe({
      next: (res: any) => {
        alert(res?.message || 'You have taken possession from farmer');
        this.cancelForm();
        this.loadProduct();
      },
      error: (err) => {
        alert(err?.error?.error || 'Failed');
      }
    });
  }

  submitFeedback() {
    if (this.userRole !== 'Consumer') { alert('Only consumers can submit feedback.'); return; }
    if (!this.product?.productId) { alert('Product not loaded'); return; }
    if (!this.consumerCanGiveFeedback) { alert('You have already submitted feedback for this product.'); return; }
    const rating = Number(this.myRating || 0);
    if (!rating || rating < 1 || rating > 5) { alert('Rating must be 1–5'); return; }
    const payload = { rating, comment: (this.myComment || '').trim() };
    this.http.post(`${environment.apiUrl}/products/${this.product.productId}/feedback`, payload).subscribe({
      next: () => {
        alert('Thanks for your feedback!');
        this.consumerCanGiveFeedback = false;
        this.myRating = 5;
        this.myComment = '';
        this.loadFeedbacks();
      },
      error: (err) => {
        alert(err?.error?.error || 'Failed');
      }
    });
  }

  getCropEmoji(): string {
    const name = (this.product?.cropName || '').toLowerCase();
    const map: Record<string, string> = {
      onion: '🧅', tomato: '🍅', mango: '🥭', potato: '🥔', rice: '🍚',
      banana: '🍌', apple: '🍎', orange: '🍊', grape: '🍇', wheat: '🌾',
      corn: '🌽', carrot: '🥕', cucumber: '🥒', lettuce: '🥬',
      strawberry: '🍓', watermelon: '🍉', coffee: '☕', cotton: '👕', sugarcane: '🍬'
    };
    const key = Object.keys(map).find(k => name.includes(k));
    return key ? map[key] : '🌱';
  }

  getImageUrl(path: string): string {
    return path?.startsWith('http') ? path : `${environment.apiUrl}${path}`;
  }

  actorFromLog(log: any): string {
    const createdBy = (log?.createdBy || '').trim();
    if (createdBy) return createdBy;

    const from = log?.fromUserId ?? null;
    const to = log?.toUserId ?? null;
    const confirmed = !!log?.confirmed;

    if (from === null && to !== null && confirmed) return 'Distributor';
    if (from !== null && to !== null && from === to && confirmed) return 'Distributor';
    if (from !== null && to !== null && from !== to && confirmed) return 'Retailer';
    if (from === null && to === null && !confirmed) return 'Farmer';

    return 'Farmer';
  }
}
