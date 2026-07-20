import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/models';

const TOKEN_KEY = 'budget_butler_token';
const NAME_KEY = 'budget_butler_name';
const EMAIL_KEY = 'budget_butler_email';

/**
 * Handles everything related to "who is logged in":
 * - calling /api/auth/login and /api/auth/register
 * - storing the token in the browser's localStorage, so the user stays logged in
 *   even after refreshing the page or closing the tab
 * - exposing a simple isLoggedIn$ stream that other parts of the app (like the navbar)
 *   can watch to instantly react when the user logs in or out
 */
@Injectable({ providedIn: 'root' })
export class AuthService {

  private baseUrl = environment.apiUrl;

  // BehaviorSubject = an observable that remembers its last value and gives it
  // immediately to anyone who subscribes. Perfect for "is the user logged in right now?".
  private loggedInSubject = new BehaviorSubject<boolean>(this.hasToken());
  isLoggedIn$ = this.loggedInSubject.asObservable();

  constructor(private http: HttpClient) {
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/auth/register`, request)
      .pipe(tap(response => this.storeSession(response)));
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/auth/login`, request)
      .pipe(tap(response => this.storeSession(response)));
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(NAME_KEY);
    localStorage.removeItem(EMAIL_KEY);
    this.loggedInSubject.next(false);
  }

  forgotPassword(email: string): Observable<string> {
    return this.http.post(`${this.baseUrl}/auth/forgot-password`, { email }, { responseType: 'text' });
  }

  resetPassword(token: string, newPassword: string): Observable<string> {
    return this.http.post(`${this.baseUrl}/auth/reset-password`, { token, newPassword }, { responseType: 'text' });
  }

  verifyEmail(token: string): Observable<string> {
    return this.http.get(`${this.baseUrl}/auth/verify-email?token=${encodeURIComponent(token)}`, { responseType: 'text' });
  }

  resendVerification(email: string): Observable<string> {
    return this.http.post(`${this.baseUrl}/auth/resend-verification`, { email }, { responseType: 'text' });
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  getUserName(): string | null {
    return localStorage.getItem(NAME_KEY);
  }

  getUserEmail(): string | null {
    return localStorage.getItem(EMAIL_KEY);
  }

  isLoggedIn(): boolean {
    return this.hasToken();
  }

  private storeSession(response: AuthResponse): void {
    localStorage.setItem(TOKEN_KEY, response.token);
    localStorage.setItem(NAME_KEY, response.name || response.email);
    localStorage.setItem(EMAIL_KEY, response.email);
    this.loggedInSubject.next(true);
  }

  private hasToken(): boolean {
    return !!localStorage.getItem(TOKEN_KEY);
  }
}
