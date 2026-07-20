import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../services/api.service';
import { AuthService } from '../services/auth.service';
import { DashboardResponse } from '../models/models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink], // gives us *ngIf, *ngFor, currency pipe, and routerLink
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {

  dashboard: DashboardResponse | null = null;
  loading = true;
  errorMessage = '';

  resendingVerification = false;
  resendSent = false;

  constructor(private api: ApiService, private authService: AuthService) {
  }

  // ngOnInit runs once, right after Angular creates this component.
  // This is the standard place to load data from the backend.
  ngOnInit(): void {
    this.api.getDashboard().subscribe({
      next: (data) => {
        this.dashboard = data;
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.errorMessage = 'Could not reach the Budget Butler backend. Is it running on port 8080?';
        this.loading = false;
      }
    });
  }

  resendVerification(): void {
    const email = this.authService.getUserEmail();
    if (!email) return;

    this.resendingVerification = true;
    this.authService.resendVerification(email).subscribe({
      next: () => {
        this.resendingVerification = false;
        this.resendSent = true;
      },
      error: () => {
        this.resendingVerification = false;
      }
    });
  }
}
