import { Component, EventEmitter, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { CartService } from '../../../services/cart.service';
import { ProductService } from '../../../services/product.service';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-cart-sidebar',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="fixed inset-0 z-50 overflow-hidden" aria-labelledby="slide-over-title" role="dialog" aria-modal="true">
      <div class="absolute inset-0 overflow-hidden">
        <!-- Background overlay with blur -->
        <div class="absolute inset-0 bg-gray-900/60 backdrop-blur-sm transition-opacity" (click)="close.emit()"></div>

        <div class="pointer-events-none fixed inset-y-0 right-0 flex max-w-full pl-10">
          <div class="pointer-events-auto w-screen max-w-md">
            <div class="flex h-full flex-col overflow-y-scroll bg-white shadow-2xl">
              
              <!-- Header -->
              <div class="flex-1 overflow-y-auto px-4 py-6 sm:px-6">
                <div class="flex items-start justify-between">
                  <div>
                    <h2 class="text-xl font-bold text-gray-900" id="slide-over-title">Shopping Cart</h2>
                    <p class="mt-1 text-sm text-gray-500">Review your fresh picks</p>
                  </div>
                  <div class="ml-3 flex h-7 items-center">
                    <button type="button" (click)="close.emit()" class="relative -m-2 p-2 text-gray-400 hover:text-gray-500 transition-colors">
                      <span class="absolute -inset-0.5"></span>
                      <span class="sr-only">Close panel</span>
                      <svg class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" aria-hidden="true">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
                      </svg>
                    </button>
                  </div>
                </div>

                <!-- Cart Items -->
                <div class="mt-8">
                  <div class="flow-root">
                    <ul role="list" class="-my-6 divide-y divide-gray-100">
                      <li *ngFor="let item of cartService.cartItems()" class="flex py-6 transition hover:bg-gray-50 rounded-lg px-2 -mx-2">
                        <div class="h-24 w-24 flex-shrink-0 overflow-hidden rounded-xl border border-gray-200">
                          <img [src]="item.image || 'assets/placeholder.jpg'" [alt]="item.name" class="h-full w-full object-cover object-center">
                        </div>

                        <div class="ml-4 flex flex-1 flex-col">
                          <div>
                            <div class="flex justify-between text-base font-bold text-gray-900">
                              <h3>
                                <a href="javascript:void(0)">{{ item.name }}</a>
                              </h3>
                              <p class="text-emerald-700">₹{{ item.price }}</p>
                            </div>
                            <p class="mt-1 text-sm text-gray-500">Quality: <span class="font-medium text-emerald-600">{{ item.quality || 'Standard' }}</span></p>
                          </div>
                          <div class="flex flex-1 items-end justify-between text-sm">
                            <div class="flex items-center space-x-3 bg-gray-50 border border-gray-200 rounded-lg p-1">
                                <button (click)="cartService.updateQuantity(item.id, item.quantity - 1)" 
                                  class="w-6 h-6 flex items-center justify-center rounded bg-white shadow-sm text-gray-600 hover:text-emerald-600 font-bold transition-colors disabled:opacity-50"
                                  [disabled]="item.quantity <= 1">
                                  -
                                </button>
                                <span class="font-semibold w-4 text-center">{{ item.quantity }}</span>
                                <button (click)="cartService.updateQuantity(item.id, item.quantity + 1)" 
                                  class="w-6 h-6 flex items-center justify-center rounded bg-white shadow-sm text-gray-600 hover:text-emerald-600 font-bold transition-colors">
                                  +
                                </button>
                            </div>

                            <div class="flex">
                              <button type="button" (click)="cartService.removeFromCart(item.id)" class="font-medium text-red-500 hover:text-red-700 transition-colors">Remove</button>
                            </div>
                          </div>
                        </div>
                      </li>

                      <!-- Empty State -->
                      <li *ngIf="cartService.cartItems().length === 0" class="py-12 flex flex-col items-center justify-center text-center">
                        <div class="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-4 text-3xl">🛒</div>
                        <p class="text-gray-900 font-medium">Your cart is empty</p>
                        <p class="text-gray-500 text-sm mt-1">Add some fresh produce to get started!</p>
                      </li>
                    </ul>
                  </div>
                </div>
              </div>

              <!-- Footer -->
              <div class="border-t border-gray-200 px-4 py-6 sm:px-6 bg-gray-50/50">
                <div class="flex justify-between text-base font-medium text-gray-900 mb-4">
                  <p>Subtotal</p>
                  <p class="text-xl font-bold text-emerald-700">₹{{ cartService.cartTotal() }}</p>
                </div>
                <div class="mt-6">
                  <button (click)="checkout()" 
                    [disabled]="cartService.cartItems().length === 0 || isProcessing"
                    class="w-full flex items-center justify-center rounded-xl border border-transparent bg-emerald-600 px-6 py-4 text-base font-bold text-white shadow-sm hover:bg-emerald-700 transition-all disabled:opacity-50 disabled:cursor-not-allowed hover:shadow-lg active:scale-[0.98]">
                    {{ isProcessing ? 'Processing...' : 'Proceed to Checkout' }}
                  </button>
                </div>
                <div class="mt-6 flex justify-center text-center text-sm text-gray-500">
                  <p>
                    or
                    <button type="button" (click)="close.emit()" class="font-medium text-emerald-600 hover:text-emerald-500 hover:underline ml-1">
                      Continue Shopping
                      <span aria-hidden="true"> &rarr;</span>
                    </button>
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class CartSidebarComponent {
  @Output() close = new EventEmitter<void>();

  private router = inject(Router);
  private productService = inject(ProductService);

  isProcessing = false;

  constructor(public cartService: CartService) { }

  checkout() {
    if (this.cartService.cartItems().length === 0) return;

    this.isProcessing = true;
    const items = this.cartService.cartItems();

    // Create an observable for each item purchase
    const purchaseRequests = items.map(item =>
      this.productService.purchaseProduct(item.id, 'Online Consumer')
    );

    forkJoin(purchaseRequests).subscribe({
      next: (results) => {
        // All successful
        alert('Order placed successfully! You can view it in your History.');
        this.cartService.clearCart();
        this.close.emit();
        // Updated requirement: go to marketplace (or History? User said "comes under the Provenance history". 
        // But user previously asked "products are comes in the marketplace page".
        // Let's stick to Marketplace for flow, or stick to user's "wants the products are comes in the marketplace page" 
        // but now "add into marketplace after rhe addded it will comes under the Provenance history"
        // Let's go to History to show the change. Or stay in Marketplace?
        // Let's go to History as per "it will comes under the Provenance history" implying they want to see it there.
        // Actually, redirecting to History makes more sense now.
        this.router.navigate(['/consumer/history']);
        this.isProcessing = false;
      },
      error: (err) => {
        console.error('Checkout failed', err);
        console.error('Error details:', err.error);
        const errorMsg = err.error?.error || err.message || 'Some items could not be purchased. They might be out of stock.';
        alert(`Purchase failed: ${errorMsg}`);
        this.isProcessing = false;
      }
    });
  }
}
