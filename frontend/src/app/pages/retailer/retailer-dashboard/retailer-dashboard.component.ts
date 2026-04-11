import { HttpClient } from '@angular/common/http';
import { Component, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Chart, registerables } from 'chart.js';
Chart.register(...registerables);

@Component({
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './retailer-dashboard.component.html',
})
export class RetailerDashboardComponent implements AfterViewInit {
  @ViewChild('barChart', { static: false }) barChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('pieChart', { static: false }) pieChartRef!: ElementRef<HTMLCanvasElement>;

  private barChartInstance: Chart | null = null;
  private pieChartInstance: Chart | null = null;

  constructor(private http: HttpClient) { }

  stats: any = {
    inventoryValue: 0,
    openPOs: 0,
    incomingShipments: 0,
    lowStock: 0
  };

  recentOrders: any[] = [];

  ngAfterViewInit(): void {
    this.fetchDashboardData();
  }

  fetchDashboardData() {
    // 1. Fetch Stats
    this.http.get<any>('/api/retailer/dashboard-stats').subscribe({
      next: (data) => {
        this.stats = data;
      },
      error: (err) => {
        console.error('Failed to load stats', err);
        // Set fallback stats
        this.stats = {
          inventoryValue: 125000,
          openPOs: 5,
          incomingShipments: 3,
          lowStock: 2
        };
      }
    });

    // 2. Fetch Sales Chart
    this.http.get<any>('/api/retailer/sales-chart').subscribe({
      next: (data) => {
        // Check if data is valid before attempting to use it
        if (data && data.labels && data.values && data.labels.length > 0) {
          this.renderBarChart(data.labels, data.values);
        } else {
          // Data is null or invalid, use fallback
          console.log('Invalid chart data received, using fallback');
          const fallbackLabels = this.getLast7Days();
          const fallbackValues = [15000, 18000, 22000, 19000, 25000, 21000, 23000];
          this.renderBarChart(fallbackLabels, fallbackValues);
        }
      },
      error: (err) => {
        console.error('Failed to load chart data, using fallback', err);
        // Use fallback data so graph still displays
        const fallbackLabels = this.getLast7Days();
        const fallbackValues = [15000, 18000, 22000, 19000, 25000, 21000, 23000];
        this.renderBarChart(fallbackLabels, fallbackValues);
      }
    });

    // 3. Render Pie Chart (Inventory Distribution)
    this.renderPieChart();

    // 4. Fetch Recent Orders
    this.http.get<any>('/api/track/pending?size=5').subscribe({
      next: (page) => {
        const orders = page.content || [];
        this.recentOrders = orders.map((o: any) => ({
          id: o.productId,
          supplier: 'Distributor',
          items: 'Batch #' + o.productId,
          total: 0,
          status: 'Pending'
        }));
      },
      error: (err) => console.error('Failed to load orders', err)
    });
  }

  renderBarChart(labels: string[], values: number[]) {
    if (!this.barChartRef) {
      console.error('Bar chart ref not available');
      return;
    }

    const barCtx = this.barChartRef.nativeElement.getContext('2d')!;

    // Destroy existing chart if it exists
    if (this.barChartInstance) {
      this.barChartInstance.destroy();
    }

    this.barChartInstance = new Chart(barCtx, {
      type: 'bar',
      data: {
        labels: labels,
        datasets: [{
          label: 'Sales (₹)',
          data: values,
          borderRadius: 8,
          backgroundColor: 'rgba(16, 185, 129, 0.85)',
          hoverBackgroundColor: 'rgba(16, 185, 129, 1)',
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: true,
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: 'rgba(0, 0, 0, 0.8)',
            padding: 12,
            cornerRadius: 8,
            titleFont: { size: 14, weight: 'bold' },
            bodyFont: { size: 13 }
          }
        },
        scales: {
          y: {
            beginAtZero: true,
            grid: {
              color: 'rgba(0, 0, 0, 0.05)'
            }
          },
          x: {
            grid: {
              display: false
            }
          }
        }
      },
    });
  }

  renderPieChart() {
    if (!this.pieChartRef) {
      console.error('Pie chart ref not available');
      return;
    }

    const pieCtx = this.pieChartRef.nativeElement.getContext('2d')!;

    // Destroy existing chart if it exists
    if (this.pieChartInstance) {
      this.pieChartInstance.destroy();
    }

    this.pieChartInstance = new Chart(pieCtx, {
      type: 'doughnut',
      data: {
        labels: ['Vegetables', 'Fruits', 'Grains', 'Others'],
        datasets: [{
          data: [45, 25, 20, 10],
          backgroundColor: [
            'rgba(16, 185, 129, 0.8)',
            'rgba(59, 130, 246, 0.8)',
            'rgba(251, 191, 36, 0.8)',
            'rgba(139, 92, 246, 0.8)'
          ],
          borderWidth: 0
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: true,
        plugins: {
          legend: {
            position: 'bottom',
            labels: {
              padding: 15,
              font: { size: 12 },
              usePointStyle: true
            }
          }
        }
      },
    });
  }

  statusClass(s: string) {
    if (!s) return 'bg-gray-100 text-gray-800';
    switch (s.toLowerCase()) {
      case 'delivered':
        return 'bg-emerald-100 text-emerald-800';
      case 'shipped':
        return 'bg-sky-100 text-sky-800';
      case 'processing':
        return 'bg-yellow-100 text-yellow-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  }

  refresh() {
    this.fetchDashboardData();
  }

  export() {
    console.log('Export data');
  }

  getLast7Days(): string[] {
    const dates = [];
    for (let i = 6; i >= 0; i--) {
      const d = new Date();
      d.setDate(d.getDate() - i);
      dates.push(d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }));
    }
    return dates;
  }
}
