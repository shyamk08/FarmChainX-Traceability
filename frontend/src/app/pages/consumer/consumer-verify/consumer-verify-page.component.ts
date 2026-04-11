import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductService } from '../../../services/product.service';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
    selector: 'app-consumer-verify-page',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './consumer-verify-page.component.html',
})
export class ConsumerVerifyPageComponent implements OnInit {
    codeInput: string = '';
    loading = false;
    verificationResult: any = null;
    error: string | null = null;
    timeline: any[] = [];
    productData: any = null;

    // AI Feature
    aiTrustScore: number = 0;
    aiAnalysis: string = '';

    // Feedback
    feedbackText: string = '';
    rating: number = 0;
    feedbackSubmitted = false;

    constructor(
        private productService: ProductService,
        private route: ActivatedRoute,
        private router: Router
    ) { }

    ngOnInit() {
        this.route.queryParams.subscribe(params => {
            if (params['uuid']) {
                this.codeInput = params['uuid'];
                this.verifyManual();
            }
        });
    }

    verifyManual(): void {
        if (!this.codeInput) return;
        this.loading = true;
        this.error = null;
        this.verificationResult = null;

        this.productService.verifyProduct(this.codeInput).subscribe({
            next: (data) => {
                this.productData = data;
                this.processTimeline(data);
                this.loading = false;
            },
            error: (err) => {
                this.error = "Product not found or invalid ID.";
                this.loading = false;
            }
        });
    }

    processTimeline(data: any) {
        this.timeline = [];
        const logs = data.logs || [];

        this.timeline.push({
            title: 'Harvested & Registered',
            role: 'Farmer',
            actor: data.farmerName || 'Farmer',
            date: data.harvestDate,
            location: data.originLocation,
            icon: '👨‍🌾',
            status: 'completed',
            details: `Quality Grade: ${data.qualityGrade || 'Pending'}`
        });

        logs.forEach((log: any) => {
            let title = 'Movement Update';
            let icon = '🚚';
            let details = log.notes;

            if (log.notes.includes('Distributor collected')) {
                title = 'Procured by Distributor';
                icon = '🏭';
            } else if (log.notes.includes('Handover to Retailer')) {
                title = 'Dispatched to Retailer';
                icon = '📦';
            } else if (log.notes.includes('Retailer Confirmed')) {
                title = 'Received at Retail Store';
                icon = '🏪';
            } else if (log.notes.includes('Sold to Consumer')) {
                title = 'Sold to Consumer';
                icon = '🛍️';
            }

            this.timeline.push({
                title: title,
                role: 'Logistics',
                actor: log.createdBy,
                date: log.timestamp,
                location: log.location,
                icon: icon,
                status: 'completed',
                details: details
            });
        });

        this.timeline.push({
            title: 'Verified Consumer Purchase',
            role: 'Consumer',
            actor: 'You',
            date: new Date().toISOString(),
            location: 'Online Check',
            icon: '✅',
            status: 'active',
            details: 'Authenticity confirmed on Blockchain.'
        });

        this.calculateAiScore();
    }

    calculateAiScore() {
        let score = 50;
        let analysis = "Basic verification passed.";

        if (this.productData.farmerName) {
            score += 20;
            analysis = "Authentic source verified directly from Farmer.";
        }

        const distributorLog = this.timeline.find(t => t.role === 'Logistics' && t.title.includes('Distributor'));
        if (distributorLog) {
            score += 15;
        }

        const retailerLog = this.timeline.find(t => t.title.includes('Retailer'));
        if (retailerLog) {
            score += 15;
        }

        if (score > 100) score = 100;

        this.aiTrustScore = score;
        this.aiAnalysis = `${analysis} Supply chain completness is ${(score / 100) * 100}%.`;
    }

    submitFeedback() {
        if ((!this.feedbackText && this.rating === 0) || !this.productData) return;

        const feedback = {
            rating: this.rating,
            comment: this.feedbackText
        };

        if (this.productData.productId) {
            this.productService.submitFeedback(this.productData.productId, feedback).subscribe({
                next: () => {
                    this.feedbackSubmitted = true;
                },
                error: (err) => {
                    console.error(err);
                    alert('Failed to submit feedback. You might have already submitted one.');
                }
            });
        } else {
            alert('Product ID missing, cannot submit feedback.');
        }
    }

    setRating(r: number) {
        this.rating = r;
    }

    scanQR(): void {
        this.router.navigate(['/scanner']);
    }

    clear(): void {
        this.codeInput = '';
        this.verificationResult = null;
        this.timeline = [];
        this.productData = null;
    }
}
