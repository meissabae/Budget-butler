import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Category, CsvImportResponse, DashboardResponse, Dream, NewTransactionRequest, PagedResponse, RecurringTransactionRequest, RecurringTransactionResponse, TransactionResponse, WalletRequest, WalletResponse } from '../models/models';

/**
 * @Injectable({ providedIn: 'root' }) means Angular creates ONE shared instance
 * of this service for the whole app - every component that asks for it gets the same one.
 *
 * This service's only job is to call the backend's REST endpoints and return Observables
 * (think of them as "a value that will arrive later, over HTTP").
 */
@Injectable({ providedIn: 'root' })
export class ApiService {

  private baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {
  }

  // ---------- Dashboard ----------
  getDashboard(): Observable<DashboardResponse> {
    return this.http.get<DashboardResponse>(`${this.baseUrl}/dashboard`);
  }

  // ---------- Categories ----------
  getCategories(): Observable<Category[]> {
    return this.http.get<Category[]>(`${this.baseUrl}/categories`);
  }

  createCategory(category: Category): Observable<Category> {
    return this.http.post<Category>(`${this.baseUrl}/categories`, category);
  }

  updateCategory(id: number, category: Category): Observable<Category> {
    return this.http.put<Category>(`${this.baseUrl}/categories/${id}`, category);
  }

  deleteCategory(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/categories/${id}`);
  }

  // ---------- Transactions ----------
  // Paginated - defaults to page 0, 20 per page. The frontend requests more as needed.
  getTransactions(page: number = 0, size: number = 20): Observable<PagedResponse<TransactionResponse>> {
    return this.http.get<PagedResponse<TransactionResponse>>(`${this.baseUrl}/transactions?page=${page}&size=${size}`);
  }

  exportTransactionsCsv(): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/transactions/export`, { responseType: 'blob' });
  }

  importTransactionsCsv(file: File, categoryId: number): Observable<CsvImportResponse> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('categoryId', categoryId.toString());
    return this.http.post<CsvImportResponse>(`${this.baseUrl}/transactions/import-csv`, formData);
  }

  createTransaction(transaction: NewTransactionRequest): Observable<TransactionResponse> {
    return this.http.post<TransactionResponse>(`${this.baseUrl}/transactions`, transaction);
  }

  updateTransaction(id: number, transaction: NewTransactionRequest): Observable<TransactionResponse> {
    return this.http.put<TransactionResponse>(`${this.baseUrl}/transactions/${id}`, transaction);
  }

  deleteTransaction(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/transactions/${id}`);
  }

  // ---------- Dreams ----------
  getDreams(): Observable<Dream[]> {
    return this.http.get<Dream[]>(`${this.baseUrl}/dreams`);
  }

  createDream(dream: Dream): Observable<Dream> {
    return this.http.post<Dream>(`${this.baseUrl}/dreams`, dream);
  }

  updateDream(id: number, dream: Dream): Observable<Dream> {
    return this.http.put<Dream>(`${this.baseUrl}/dreams/${id}`, dream);
  }

  deleteDream(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/dreams/${id}`);
  }

  // ---------- Wallets ----------
  getWallets(): Observable<WalletResponse[]> {
    return this.http.get<WalletResponse[]>(`${this.baseUrl}/wallets`);
  }

  createWallet(request: WalletRequest): Observable<WalletResponse> {
    return this.http.post<WalletResponse>(`${this.baseUrl}/wallets`, request);
  }

  updateWallet(id: number, request: WalletRequest): Observable<WalletResponse> {
    return this.http.put<WalletResponse>(`${this.baseUrl}/wallets/${id}`, request);
  }

  depositToWallet(id: number, amount: number, note: string): Observable<WalletResponse> {
    return this.http.post<WalletResponse>(`${this.baseUrl}/wallets/${id}/deposit`, { amount, note });
  }

  deleteWallet(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/wallets/${id}`);
  }

  creditSalaryNow(): Observable<WalletResponse> {
    return this.http.post<WalletResponse>(`${this.baseUrl}/wallets/credit-salary-now`, {});
  }

  // ---------- Recurring Transactions ----------
  getRecurringTransactions(): Observable<RecurringTransactionResponse[]> {
    return this.http.get<RecurringTransactionResponse[]>(`${this.baseUrl}/recurring-transactions`);
  }

  createRecurringTransaction(request: RecurringTransactionRequest): Observable<RecurringTransactionResponse> {
    return this.http.post<RecurringTransactionResponse>(`${this.baseUrl}/recurring-transactions`, request);
  }

  updateRecurringTransaction(id: number, request: RecurringTransactionRequest): Observable<RecurringTransactionResponse> {
    return this.http.put<RecurringTransactionResponse>(`${this.baseUrl}/recurring-transactions/${id}`, request);
  }

  deleteRecurringTransaction(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/recurring-transactions/${id}`);
  }
}
