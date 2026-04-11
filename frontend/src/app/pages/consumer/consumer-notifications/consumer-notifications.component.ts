import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProductService } from '../../../services/product.service';

@Component({
  selector: 'app-consumer-notifications',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './consumer-notifications.component.html',
})
export class ConsumerNotificationsComponent implements OnInit {
  notifications = signal<any[]>([]);

  constructor(private productService: ProductService) { }

  ngOnInit() {
    this.loadNotifications();
  }

  loadNotifications() {
    this.productService.getNotifications().subscribe({
      next: (data) => {
        this.notifications.set(data);
      },
      error: (err) => console.error('Failed to load notifications', err)
    });
  }

  markRead(n: any) {
    n.read = true;
    // content of this method can include a backend call to mark as read if API supports it
  }

  clearAll() {
    this.notifications.set([]);
  }
}
