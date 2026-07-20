import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { BillingInterval, CheckoutRequest, CheckoutSessionResponse, SubscriptionStatusResponse } from '../models/models';

@Injectable({ providedIn: 'root' })
export class SubscriptionService {

  private baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {
  }

  getStatus(): Observable<SubscriptionStatusResponse> {
    return this.http.get<SubscriptionStatusResponse>(`${this.baseUrl}/subscription/status`);
  }

  /**
   * Asks the backend to create a Stripe Checkout Session for a specific tier + billing interval,
   * then hands back its URL. The component calling this redirects the browser there
   * (window.location.href = url) - Stripe's own hosted page takes over from that point.
   */
  createCheckoutSession(tier: 'PLUS' | 'PREMIUM', interval: BillingInterval): Observable<CheckoutSessionResponse> {
    const request: CheckoutRequest = { tier, interval };
    return this.http.post<CheckoutSessionResponse>(`${this.baseUrl}/subscription/create-checkout-session`, request);
  }
}
