import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-farmer-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './farmer-dashboard.html'
})
export class FarmerDashboard implements OnInit {
  stats = {
    totalProducts: 0,
    activeBatches: 0,
    totalSales: 0,
    avgQuality: 'A'
  };

  recentUploads: any[] = [];
  loading = true;

  constructor(private http: HttpClient) { }

  ngOnInit() {
    this.loadDashboardData();
  }

  loadDashboardData() {
    // Fetch a larger set to approximate stats better (or use a real stats endpoint)
    this.http.get<any>('/api/products/my?page=0&size=100&sort=id,desc').subscribe({
      next: (res) => {
        const allProducts = res.content || [];
        this.recentUploads = allProducts.slice(0, 5); // Show top 5

        // Calculate basic stats
        this.stats.totalProducts = res.totalElements || allProducts.length;
        this.stats.activeBatches = allProducts.filter((p: any) => !p.sold).length;

        // Calculate Total Sales based on Sold items
        this.stats.totalSales = allProducts
          .filter((p: any) => p.sold)
          .reduce((sum: number, p: any) => sum + (p.price || 0), 0);

        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load farmer data', err);
        this.loading = false;
      }
    });
  }

  getQualityColor(grade: string): string {
    if (!grade) return 'bg-gray-100 text-gray-800';
    const g = grade.toUpperCase();
    if (g.includes('A') || g.includes('A+')) return 'bg-emerald-100 text-emerald-800';
    if (g.includes('B')) return 'bg-yellow-100 text-yellow-800';
    return 'bg-orange-100 text-orange-800';
  }
}
