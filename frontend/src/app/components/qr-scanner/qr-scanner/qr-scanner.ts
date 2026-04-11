import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ZXingScannerModule } from '@zxing/ngx-scanner';
import { BarcodeFormat } from '@zxing/library';

// 🔥 Lucide icons
import {
  LucideAngularModule,
  ScanLine,
  Upload,
  ShieldCheck,
  Lightbulb,
  LightbulbOff,
  XCircle,
  Loader2,
  X,
  CheckCircle2,
  Scan,
  ArrowRight,
  Smartphone,
  Image as ImageIcon,
  Zap,
} from 'lucide-angular';

// Enum for camera status
export type CameraStatus = 'initializing' | 'scanning' | 'denied' | 'error';

@Component({
  selector: 'app-qr-scanner',
  standalone: true,
  imports: [CommonModule, ZXingScannerModule, LucideAngularModule],
  templateUrl: './qr-scanner.html',
  styleUrl: './qr-scanner.scss',
})
export class QrScanner {
  private router = inject(Router);

  // ⭐ Register icons for template use
  readonly ScanLine = ScanLine;
  readonly Upload = Upload;
  readonly ShieldCheck = ShieldCheck;
  readonly Lightbulb = Lightbulb;
  readonly LightbulbOff = LightbulbOff;
  readonly XCircle = XCircle;
  readonly Loader2 = Loader2;
  readonly X = X;
  readonly CheckCircle2 = CheckCircle2;
  readonly Scan = Scan;
  readonly ArrowRight = ArrowRight;
  readonly Smartphone = Smartphone;
  readonly ImageIcon = ImageIcon;
  readonly Zap = Zap;

  // ⭐ Component State
  formats = [BarcodeFormat.QR_CODE];
  isScanning = false; // user picks method first
  torchEnabled = false;
  hasTorch = false;
  usingFileUpload = false;
  uploadedImageUrl: string | null = null;
  scanResult: string | null = null;
  cameraStatus: CameraStatus = 'initializing'; // New state property

  constructor() {}

  // Start camera scanning
  startCameraScan() {
    this.isScanning = true;
    this.usingFileUpload = false;
    this.uploadedImageUrl = null;
    this.cameraStatus = 'initializing'; // Set status when starting
  }

  // Stop scanning
  stopScanning() {
    this.isScanning = false;
    this.usingFileUpload = false;
    this.torchEnabled = false;
    this.uploadedImageUrl = null;
    this.cameraStatus = 'initializing'; // Reset status
  }

  // Navigate to product details
  viewDetails() {
    if (this.scanResult) {
      // Improved matching: Checks for the UUID structure and handles both full URLs and just the UUID
      const match = this.scanResult.match(
        /verify\/([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})/i
      );
      const uuidMatch = this.scanResult.match(
        /([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})/i
      );

      const idToNavigate = match ? match[1] : uuidMatch ? uuidMatch[1] : null;

      if (idToNavigate) {
        // Use a relative path to ensure it works correctly
        this.router.navigate(['/verify', idToNavigate]);
      } else {
        // Fallback for non-standard QR codes (e.g., just a URL/text without the expected format)
        alert(
          'Could not extract a valid verification ID from the scanned QR code. Result was: ' +
            this.scanResult
        );
      }
    }
  }

  // Successful scan
  onScanSuccess(result: string) {
    this.scanResult = result;
    this.isScanning = false;
    this.usingFileUpload = false;
  }

  // Error scanning
  onScanError(err: any) {
    console.error('Scan error:', err);
    this.cameraStatus = 'error'; // Set status on error
  }

  // If camera permission denied or granted
  onPermission(granted: boolean) {
    if (!granted) {
      alert('Camera access denied. Please use the "Upload Photo" option instead.');
      this.stopScanning();
      this.cameraStatus = 'denied'; // Set status on denial
    } else {
      // Permission granted, now we are waiting for the camera feed
      // We can transition to 'scanning' once the feed is detected, but for now, 'initializing' is fine.
      this.cameraStatus = 'scanning';
    }
  }

  // Detect if device has torch
  onCamerasFound(devices: MediaDeviceInfo[]) {
    this.hasTorch = devices.some(d => !!(d as any).getCapabilities?.()?.torch);
  }

  // Upload image file & scan
  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;

    const file = input.files[0];
    const reader = new FileReader();

    reader.onload = (e: any) => {
      this.uploadedImageUrl = e.target.result;
      this.isScanning = false;
      this.usingFileUpload = true;

      // Dynamic import remains a good practice for larger libraries
      import('@zxing/library').then(zxing => {
        const codeReader = new zxing.BrowserQRCodeReader();
        const img = new Image();
        img.src = e.target.result;

        img.onload = () => {
          // This promise handles the actual decoding from the image
          codeReader
            .decodeFromImageElement(img)
            .then(r => this.onScanSuccess(r.getText()))
            .catch(() => {
              alert('No QR code found in the image. Try again with a clearer photo.');
              this.restart();
            });
        };
      });
    };

    reader.readAsDataURL(file);
  }

  // Reset to main menu
  restart() {
    this.isScanning = false;
    this.usingFileUpload = false;
    this.uploadedImageUrl = null;
    this.torchEnabled = false;
    this.scanResult = null;
    this.cameraStatus = 'initializing'; // Reset status
  }
}

export default QrScanner;