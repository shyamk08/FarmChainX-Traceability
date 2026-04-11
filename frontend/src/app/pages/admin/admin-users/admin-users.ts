import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

type PrimaryRole = 'ADMIN' | 'FARMER' | 'DISTRIBUTOR' | 'RETAILER' | 'CONSUMER' | 'USER';

interface User {
  id: number;
  name: string;
  email: string;
  roles: string[];
  isAdmin: boolean;
  primaryRole: PrimaryRole;
  isPromoting?: boolean; // Tracks promotion status for button disabling
}

@Component({
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-users.html',
  styleUrl: './admin-users.scss',
  selector: 'app-admin-users'
})
export class AdminUsers implements OnInit {
  users: User[] = [];
  filteredUsers: User[] = [];
  searchTerm: string = '';
  isLoading: boolean = false; // Manages the loading spinner/state

  constructor(private http: HttpClient) { }

  ngOnInit(): void {
    this.loadUsers();
  }

  private normalizeRole(r: any): string {
    const val = (r?.name ?? r ?? '').toString().toUpperCase();
    return val.startsWith('ROLE_') ? val : `ROLE_${val}`;
  }

  private derivePrimaryRole(roles: string[]): PrimaryRole {
    const set = new Set(roles.map(this.normalizeRole));
    if (set.has('ROLE_ADMIN')) return 'ADMIN';
    if (set.has('ROLE_FARMER')) return 'FARMER';
    if (set.has('ROLE_DISTRIBUTOR')) return 'DISTRIBUTOR';
    if (set.has('ROLE_RETAILER')) return 'RETAILER';
    if (set.has('ROLE_CONSUMER')) return 'CONSUMER';
    return 'USER';
  }

  loadUsers(): void {
    this.isLoading = true;
    this.http.get<any[]>('/api/admin/users').subscribe({
      next: (data) => {
        this.users = data.map(u => {
          const roles = (u.roles ?? []).map((r: any) => this.normalizeRole(r));
          const isAdmin = roles.includes('ROLE_ADMIN');
          return {
            id: u.id,
            name: u.name,
            email: u.email,
            roles,
            isAdmin,
            primaryRole: this.derivePrimaryRole(roles),
            isPromoting: false // Initialize state
          };
        });
        this.filterUsers(); // Initial filter
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load users:', err);
        this.isLoading = false;
        alert('Could not load user data.');
      }
    });
  }

  /**
   * Generates Tailwind CSS classes for high-contrast role badges.
   */
  roleClass(role: PrimaryRole): string {
    // Solid colors with white text for better visibility on light backgrounds
    switch (role) {
      case 'ADMIN': return 'bg-purple-600 text-white ring-2 ring-purple-300';
      case 'FARMER': return 'bg-emerald-600 text-white ring-2 ring-emerald-300';
      case 'DISTRIBUTOR': return 'bg-cyan-600 text-white ring-2 ring-cyan-300';
      case 'RETAILER': return 'bg-amber-600 text-white ring-2 ring-amber-300';
      case 'CONSUMER': return 'bg-blue-600 text-white ring-2 ring-blue-300';
      default: return 'bg-gray-600 text-white ring-2 ring-gray-300';
    }
  }

  promote(userId: number): void {
    const user = this.users.find(u => u.id === userId);
    if (!user || !confirm(`Are you sure you want to promote ${user.name} to Admin? This action cannot be undone.`)) return;

    user.isPromoting = true; // Set promoting state to disable the button

    this.http.post(`/api/admin/promote/${userId}`, {}).subscribe({
      next: () => {
        alert(`${user.name} promoted to Admin successfully!`);
        this.loadUsers(); // Reload to refresh data for all users
      },
      error: (err) => {
        user.isPromoting = false; // Reset state on error
        alert(err.error?.message || 'Promotion failed. Please try again.');
        console.error('Promotion error:', err);
      }
    });
  }

  onSearch(): void {
    this.filterUsers();
  }

  filterUsers(): void {
    if (!this.searchTerm) {
      this.filteredUsers = this.users;
      return;
    }
    const term = this.searchTerm.toLowerCase();
    this.filteredUsers = this.users.filter(u =>
      u.name.toLowerCase().includes(term) ||
      u.email.toLowerCase().includes(term) ||
      u.primaryRole.toLowerCase().includes(term)
    );
  }
}