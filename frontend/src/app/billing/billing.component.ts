import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { SubscriptionService } from '../services/subscription.service';
import { BillingInterval, SubscriptionStatusResponse } from '../models/models';

@Component({
  selector: 'app-billing',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './billing.component.html',
  styleUrl: './billing.component.css'
})
export class BillingComponent implements OnInit {

  status: SubscriptionStatusResponse | null = null;
  loading = true;
  redirecting = false;
  errorMessage = '';

  // Toggle between monthly and annual pricing - defaults to monthly.
  selectedInterval: BillingInterval = 'MONTHLY';

  // Set when Stripe redirects the user back here after checkout.
  showSuccessBanner = false;
  showCanceledBanner = false;

  // Prices shown in the UI (kept here so the template stays simple - these must match
  // whatever Prices you actually configured in Stripe / application.properties).
  readonly prices = {
    plus: { monthly: 4.99, annual: 47.99 },
    premium: { monthly: 7.99, annual: 76.99 }
  };

  constructor(
    private subscriptionService: SubscriptionService,
    private route: ActivatedRoute
  ) {
  }

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.showSuccessBanner = params['success'] === 'true';
      this.showCanceledBanner = params['canceled'] === 'true';
    });

    this.loadStatus();
  }

  loadStatus(): void {
    this.subscriptionService.getStatus().subscribe({
      next: (data) => {
        this.status = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'Could not load your subscription status.';
      }
    });
  }

  setInterval(interval: BillingInterval): void {
    this.selectedInterval = interval;
  }

  subscribe(tier: 'PLUS' | 'PREMIUM'): void {
    this.redirecting = true;
    this.subscriptionService.createCheckoutSession(tier, this.selectedInterval).subscribe({
      next: (session) => {
        window.location.href = session.checkoutUrl;
      },
      error: () => {
        this.redirecting = false;
        this.errorMessage = 'Could not start checkout. Please try again.';
      }
    });
  }
}
