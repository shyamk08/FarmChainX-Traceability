import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { ConsumerSidebarComponent } from '../consumer-sidebar/consumer-sidebar.component';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-consumer-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, ConsumerSidebarComponent],
  templateUrl: './consumer-layout.component.html',
  // styleUrls: ['./consumer-layout.component.scss'],
})
export class ConsumerLayoutComponent {
  sidebarOpen = false;

  constructor(private auth: AuthService) { }

  toggleSidebar(): void {
    this.sidebarOpen = !this.sidebarOpen;
  }

  currentConsumerName(): string {
    const n = (this.auth?.getName && this.auth.getName()) || 'User';
    return n ? String(n).split(' ')[0] : 'User';
  }
}
