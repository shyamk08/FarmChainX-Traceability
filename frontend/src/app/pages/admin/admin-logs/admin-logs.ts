import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { AdminService, SupplyChainLog } from '../../../services/admin.service';

@Component({
    standalone: true,
    imports: [CommonModule, DatePipe],
    templateUrl: './admin-logs.html',
    selector: 'app-admin-logs'
})
export class AdminLogs implements OnInit {
    logs: SupplyChainLog[] = [];
    isLoading = false;

    constructor(private adminService: AdminService) { }

    ngOnInit(): void {
        this.refresh();
    }

    refresh(): void {
        this.isLoading = true;
        this.adminService.getSystemLogs().subscribe({
            next: (data) => {
                this.logs = data.sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
                this.isLoading = false;
            },
            error: (err) => {
                console.error(err);
                this.isLoading = false;
            }
        });
    }
}
