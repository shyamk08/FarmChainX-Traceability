import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ProductService } from '../../services/product.service';

@Component({
  selector: 'app-upload-product',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './upload-product.html'
})
export class UploadProduct {
  cropName = '';
  soilType = '';
  pesticides = '';
  harvestDate = '';             // yyyy-MM-dd
  gpsLocation = '';
  price: number | null = null;
  quantity: number = 1000;  // Default quantity
  quantityUnit: string = 'kg';  // Default unit
  imageFile: File | null = null;
  previewUrl: string | ArrayBuffer | null = null;

  loading = false;
  today = this.getTodayString(); // used as max for date input

  // AI Prediction properties
  showPrediction = false;
  aiPrediction: any = null;
  productId: number | null = null;

  constructor(private productService: ProductService, private router: Router) { }

  // helper to produce today's date in yyyy-MM-dd
  private getTodayString(): string {
    const d = new Date();
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }

  onFileSelected(event: any) {
    const file = event.target.files && event.target.files[0];
    if (!file) return;

    this.imageFile = file;
    const reader = new FileReader();
    reader.onload = () => this.previewUrl = reader.result;
    reader.readAsDataURL(file);
  }

  detectGPS() {
    if (!navigator.geolocation) {
      alert("GPS not supported");
      return;
    }

    navigator.geolocation.getCurrentPosition(position => {
      const lat = position.coords.latitude;
      const lng = position.coords.longitude;
      this.gpsLocation = `${lat},${lng}`;
      alert('GPS detected');
    }, (err) => {
      console.warn('GPS error', err);
      alert('Unable to detect GPS');
    });
  }

  uploadProduct() {
    if (!this.imageFile) {
      alert("Please select an image");
      return;
    }

    // Prevent future-date uploads
    if (this.harvestDate) {
      if (this.harvestDate > this.today) {
        alert('Harvest date cannot be in the future.');
        return;
      }
    }

    this.loading = true;

    const formData = new FormData();
    formData.append('cropName', this.cropName.trim());
    formData.append('soilType', this.soilType.trim());
    formData.append('pesticides', this.pesticides.trim());
    // ensure backend-friendly date format yyyy-MM-dd (input already gives it)
    formData.append('harvestDate', this.harvestDate);
    formData.append('gpsLocation', this.gpsLocation.trim());
    formData.append('price', this.price ? String(this.price) : '0');
    formData.append('quantity', String(this.quantity));
    formData.append('quantityUnit', this.quantityUnit);
    formData.append('image', this.imageFile);

    this.productService.uploadProduct(formData)
      .subscribe({
        next: (res) => {
          this.loading = false;
          if (res.success && res.aiPrediction) {
            // Store AI prediction and show modal
            this.aiPrediction = res.aiPrediction;
            this.productId = res.id;
            this.showPrediction = true;
          } else {
            // Fallback if no AI prediction
            alert(`Product uploaded! ID = ${res.id}`);
            this.router.navigate(['/products/my']);
          }
        },
        error: (err) => {
          this.loading = false;
          console.error('Upload error full:', err);
          console.error('Status:', err?.status);
          console.error('Error body:', err?.error);
          const serverMsg = err?.error?.message || err?.error?.error || err?.statusText || (err?.message ? err.message : 'Upload failed!');
          alert(`Upload failed: ${serverMsg}`);
        }
      });
  }

  closePredictionModal() {
    this.showPrediction = false;
    this.router.navigate(['/dashboard']);
  }

  getQualityColor(grade: string): string {
    if (!grade) return 'slate';
    const gradeUpper = grade.toUpperCase();
    if (gradeUpper.includes('A+') || gradeUpper === 'A') return 'emerald';
    if (gradeUpper.includes('B+') || gradeUpper === 'B') return 'yellow';
    if (gradeUpper === 'C') return 'orange';
    return 'slate';
  }
}
