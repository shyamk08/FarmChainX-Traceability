import { Injectable, signal, computed } from '@angular/core';

export interface CartItem {
    id: number;
    name: string;
    price: number; // Price per unit
    quantity: number;
    image: string;
    quality: string;
}

@Injectable({
    providedIn: 'root'
})
export class CartService {
    // Signal to hold cart items
    private cartItemsSignal = signal<CartItem[]>([]);

    // Computed signals for derived state
    cartItems = this.cartItemsSignal.asReadonly();

    cartTotal = computed(() => {
        return this.cartItemsSignal().reduce((acc, item) => acc + (item.price * item.quantity), 0);
    });

    cartCount = computed(() => {
        return this.cartItemsSignal().reduce((acc, item) => acc + item.quantity, 0);
    });

    constructor() { }

    addToCart(product: any, quantity: number = 1) {
        this.cartItemsSignal.update(items => {
            const existingItem = items.find(i => i.id === product.id);
            if (existingItem) {
                return items.map(i => i.id === product.id
                    ? { ...i, quantity: i.quantity + quantity }
                    : i);
            } else {
                return [...items, {
                    id: product.id,
                    name: product.cropName || product.name,
                    price: product.expectedPrice || product.price || 100, // Fallback price
                    quantity: quantity,
                    // Fix: Use imagePath as primary source, then imageUrl, then image, then fallback
                    image: product.imagePath || product.imageUrl || product.image || 'assets/placeholder.jpg',
                    quality: product.qualityGrade || 'Standard'
                }];
            }
        });
    }

    removeFromCart(productId: number) {
        this.cartItemsSignal.update(items => items.filter(i => i.id !== productId));
    }

    updateQuantity(productId: number, quantity: number) {
        if (quantity <= 0) {
            this.removeFromCart(productId);
            return;
        }
        this.cartItemsSignal.update(items => items.map(i => i.id === productId ? { ...i, quantity } : i));
    }

    clearCart() {
        this.cartItemsSignal.set([]);
    }
}
