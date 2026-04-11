import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
// Import map for the retry logic fix
import { catchError, delay, retryWhen, scan, throwError, map } from 'rxjs';
import { TitleCasePipe, DecimalPipe } from '@angular/common';

// Define the type for the scan accumulator to make the logic clear
interface RetryState {
  count: number;
  error: any;
  stop: boolean;
}

@Component({
  standalone: true,
  imports: [CommonModule, TitleCasePipe],
  templateUrl: './my-products.html',
})
export class MyProducts {
  products: any[] = [];
  loading = true;
  retryMessage = '';
  page = 0;
  size = 9;
  totalPages = 0;

  farmerId: string | null = null;
  totalItems = 0;

  constructor(private http: HttpClient, private router: Router) {
    this.load();
  }

  load(page: number = 0) {
    this.loading = true;
    this.retryMessage = '';

    //

    this.http
      .get<any>(`/api/products/my?page=${page}&size=${this.size}&sort=id,desc`)
      .pipe(
        retryWhen((errors) =>
          errors.pipe(
            // 1. Use scan to increment the counter and determine if we should stop.
            // The seed is now an object (RetryState) to hold all necessary state.
            scan(
              (state: RetryState, err) => {
                state.count++;
                state.error = err; // Store the original error

                if (state.count > 3) {
                  state.stop = true;
                } else {
                  this.retryMessage = `🔁 Reconnecting... (Attempt ${state.count} of 3)`;
                }
                // scan MUST return the next accumulator value (the state object)
                return state;
              },
              { count: 0, error: null, stop: false } as RetryState
            ), // Initialize state object

            // 2. Use map to check the state and conditionally throw the error stream.
            map((state) => {
              if (state.stop) {
                // Throw the error *value* here to correctly terminate the stream
                // and pass the error down the chain for catchError to handle.
                throw new Error('Max retries reached');
              }
              return state;
            }),

            delay(1000)
          )
        ),
        catchError((err) => {
          this.retryMessage = '';
          this.loading = false;

          // Check for the specific error message thrown after max retries
          if (err.message === 'Max retries reached') {
            alert('❌ Failed to load products after multiple attempts.');
          }
          // The JwtInterceptor handles 401/403, but we must re-throw other errors
          return throwError(() => err);
        })
      )
      .subscribe({
        next: (res) => {
          this.products = res?.content || [];
          this.page = res?.number || 0;
          this.totalPages = res?.totalPages || 1;
          this.totalItems = res?.totalElements || this.products.length;
          this.loading = false;
          this.retryMessage = '';
        },
      });
  }

  // ... rest of the component methods (nextPage, prevPage, generateQr, etc.) ...

  nextPage() {
    if (this.page < this.totalPages - 1) this.load(this.page + 1);
  }

  prevPage() {
    if (this.page > 0) this.load(this.page - 1);
  }

  generateQr(id: number) {
    this.http.post<any>(`/api/products/${id}/qrcode`, {}).subscribe({
      next: (res) => {
        const product = this.products.find((p) => p.id === id)!;
        const url = res.qrPath.startsWith('http')
          ? res.qrPath
          : `http://localhost:8080${res.qrPath}`;
        const filename = this.generateFilename(product);

        // Download the QR code automatically
        this.http.get(url, { responseType: 'blob' }).subscribe((blob) => {
          const a = document.createElement('a');
          a.href = window.URL.createObjectURL(blob);
          a.download = filename;
          a.click();
          window.URL.revokeObjectURL(a.href);
        });

        // Show success message with highlight
        this.showSuccessMessage(id, filename);

        // Reload to update the product with QR code path
        this.load(this.page);
      },
      error: (err) => alert(err.error?.message || 'Failed to generate QR'),
    });
  }

  private showSuccessMessage(productId: number, filename: string) {
    // Create success notification
    const notification = document.createElement('div');
    notification.className =
      'fixed top-4 right-4 bg-emerald-500 text-white px-6 py-4 rounded-lg shadow-2xl z-50 animate-bounce';
    notification.innerHTML = `
      <div class="flex items-center gap-3">
        <span class="text-2xl">✅</span>
        <div>
          <p class="font-bold">QR Code Generated!</p>
          <p class="text-sm">${filename}</p>
        </div>
      </div>
    `;
    document.body.appendChild(notification);

    // Highlight the product card using the data-product-id attribute
    setTimeout(() => {
      // Find the card using the data-product-id attribute added in the HTML
      const productCard = document.querySelector(`[data-product-id="${productId}"]`);
      if (productCard) {
        productCard.classList.add('ring-4', 'ring-emerald-500', 'scale-105');
        setTimeout(() => {
          productCard.classList.remove('ring-4', 'ring-emerald-500', 'scale-105');
        }, 2000);
      }
    }, 100);

    // Remove notification after 3 seconds
    setTimeout(() => {
      notification.remove();
    }, 3000);
  }

  downloadQr(id: number) {
    const product = this.products.find((p) => p.id === id);
    if (!product?.qrCodePath) return;

    const url = this.getQrUrl(product.qrCodePath);
    const filename = this.generateFilename(product);

    this.http.get(url, { responseType: 'blob' }).subscribe((blob) => {
      const a = document.createElement('a');
      a.href = window.URL.createObjectURL(blob);
      a.download = filename;
      a.click();
      window.URL.revokeObjectURL(a.href);

      // Show download confirmation
      const notification = document.createElement('div');
      notification.className =
        'fixed bottom-4 right-4 bg-blue-500 text-white px-6 py-3 rounded-lg shadow-xl z-50';
      notification.innerHTML = `
        <div class="flex items-center gap-2">
          <span class="text-xl">⬇️</span>
          <span class="font-semibold">Downloaded: ${filename}</span>
        </div>
      `;
      document.body.appendChild(notification);
      setTimeout(() => notification.remove(), 2500);
    });
  }

  viewQr(qrPath: string) {
    const url = this.getQrUrl(qrPath);
    window.open(url, '_blank');
  }

  private generateFilename(product: any): string {
    const cleanName = (product.cropName || 'Product')
      .replace(/[^a-zA-Z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '');
    return `QR_${cleanName}_${product.id}.png`;
  }

  getImageUrl(path: string): string {
    return path?.startsWith('http') ? path : `http://localhost:8080${path}`;
  }

  getQrUrl(path: string): string {
    return path?.startsWith('http') ? path : `http://localhost:8080${path}`;
  }

  formatDate(date: string | null): string {
    if (!date) return 'Unknown Date';
    return new Date(date).toLocaleDateString('en-US', {
      month: 'long',
      day: 'numeric',
      year: 'numeric',
    });
  }

  getCropEmoji(name: string): string {
    const n = (name || '').toLowerCase();
    const map: Record<string, string> = {
      onion: '🧅',
      tomato: '🍅',
      mango: '🥭',
      potato: '🥔',
      rice: '🌾',
      banana: '🍌',
      apple: '🍎',
      orange: '🍊',
      grape: '🍇',
      wheat: '🌿',
      corn: '🌽',
      carrot: '🥕',
      cucumber: '🥒',
      strawberry: '🍓',
      watermelon: '🍉',
    };
    return Object.keys(map).find((k) => n.includes(k))
      ? map[Object.keys(map).find((k) => n.includes(k))!]
      : '🌱';
  }
}
