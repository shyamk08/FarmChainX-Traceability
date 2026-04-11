import { Component, OnInit, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Chart, registerables } from 'chart.js';
import { ProductService } from '../../../services/product.service';

Chart.register(...registerables);

@Component({
  selector: 'app-distributor-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, DecimalPipe],
  templateUrl: './distributor-dashboard.component.html'
})
export class DistributorDashboardComponent implements OnInit, AfterViewInit {
  @ViewChild('inventoryChart') inventoryChartRef!: ElementRef;
  @ViewChild('salesChart') salesChartRef!: ElementRef;

  stats = {
    connectedFarmers: 0,
    activeBatches: 0,
    pendingOrders: 0,
    revenue: 0 // Using this for Total Value
  };

  recentActivities: any[] = [];
  inventoryChart: any;
  salesChart: any;

  constructor(private productService: ProductService) { }

  ngOnInit(): void {
    this.loadStats();
  }

  loadStats() {
    this.productService.getDistributorStats().subscribe({
      next: (data) => {
        this.stats.activeBatches = data.activeBatches;
        this.stats.revenue = data.totalValue;
        this.stats.connectedFarmers = data.connectedFarmers;
        this.stats.pendingOrders = data.pendingOrders;
        this.recentActivities = data.recentActivities || [];

        if (data.chartData) {
          this.updateCharts(data.chartData);
        }
      },
      error: (err) => console.error('Failed to load dashboard stats', err)
    });
  }

  ngAfterViewInit() {
    // Charts will be initialized when data arrives or with empty data first
    this.initInventoryChart();
    this.initSalesChart();
  }

  updateCharts(data: any) {
    if (this.inventoryChart && data.inventory) {
      this.inventoryChart.data.labels = Object.keys(data.inventory);
      this.inventoryChart.data.datasets[0].data = Object.values(data.inventory);
      this.inventoryChart.update();
    }

    if (this.salesChart && data.sales) {
      this.salesChart.data.labels = Object.keys(data.sales);
      this.salesChart.data.datasets[0].data = Object.values(data.sales);
      this.salesChart.update();
    }
  }

  initInventoryChart() {
    if (!this.inventoryChartRef) return;
    const ctx = this.inventoryChartRef.nativeElement.getContext('2d');
    this.inventoryChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: [],
        datasets: [{
          data: [],
          backgroundColor: [
            '#10B981', // Emerald 500
            '#3B82F6', // Blue 500
            '#F59E0B', // Amber 500
            '#8B5CF6', '#EC4899', '#6366F1'
          ],
          borderWidth: 0
        }]
      },
      options: {
        responsive: true,
        plugins: {
          legend: {
            position: 'bottom'
          }
        },
        cutout: '70%'
      }
    });
  }

  initSalesChart() {
    if (!this.salesChartRef) return;
    const ctx = this.salesChartRef.nativeElement.getContext('2d');
    this.salesChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: [], // Will be filled
        datasets: [{
          label: 'Revenue (₹)',
          data: [],
          backgroundColor: '#059669', // Emerald 600
          borderRadius: 4
        }]
      },
      options: {
        responsive: true,
        plugins: {
          legend: { display: false }
        },
        scales: {
          y: { beginAtZero: true }
        }
      }
    });
  }
}
