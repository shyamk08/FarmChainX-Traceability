import { TestBed } from '@angular/core/testing';
import { CartService } from './cart.service';

describe('CartService', () => {
    let service: CartService;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(CartService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should start with empty cart', () => {
        expect(service.cartItems().length).toBe(0);
    });

    it('should add item to cart', () => {
        const product = { id: 1, cropName: 'Corn', expectedPrice: 100, imageUrl: 'img.jpg', qualityGrade: 'A' };
        service.addToCart(product);
        expect(service.cartItems().length).toBe(1);
        expect(service.cartItems()[0].name).toBe('Corn');
        expect(service.cartItems()[0].quantity).toBe(1);
    });

    it('should increment quantity if item exists', () => {
        const product = { id: 1, cropName: 'Corn', expectedPrice: 100 };
        service.addToCart(product);
        service.addToCart(product, 2);
        expect(service.cartItems().length).toBe(1);
        expect(service.cartItems()[0].quantity).toBe(3);
    });

    it('should remove item from cart', () => {
        const product = { id: 1, cropName: 'Corn', expectedPrice: 100 };
        service.addToCart(product);
        service.removeFromCart(1);
        expect(service.cartItems().length).toBe(0);
    });

    it('should calculate total correctly', () => {
        const p1 = { id: 1, cropName: 'Corn', expectedPrice: 100 };
        const p2 = { id: 2, cropName: 'Wheat', expectedPrice: 50 };

        service.addToCart(p1, 2); // 200
        service.addToCart(p2, 1); // 50

        expect(service.cartTotal()).toBe(250);
    });
});
