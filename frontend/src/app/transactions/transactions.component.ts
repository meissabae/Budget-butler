import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../services/api.service';
import { SettingsService } from '../services/settings.service';
import { Category, CsvImportResponse, NewTransactionRequest, TransactionResponse } from '../models/models';

@Component({
  selector: 'app-transactions',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './transactions.component.html',
  styleUrl: './transactions.component.css'
})
export class TransactionsComponent implements OnInit {

  transactions: TransactionResponse[] = [];
  categories: Category[] = [];
  currency = 'USD';

  // Pagination state
  page = 0;
  pageSize = 20;
  totalPages = 0;
  totalElements = 0;

  // Form fields double as both "add" and "edit" - editingId tracks which mode we're in.
  description = '';
  amount = 0;
  date = new Date().toISOString().substring(0, 10);
  categoryId: number | null = null;
  editingId: number | null = null;

  exporting = false;

  // Bank CSV import state
  importCategoryId: number | null = null;
  selectedFile: File | null = null;
  importing = false;
  importResult: CsvImportResponse | null = null;

  constructor(private api: ApiService, private settingsService: SettingsService) {
  }

  ngOnInit(): void {
    this.loadTransactions();
    this.api.getCategories().subscribe(data => this.categories = data);
    this.settingsService.getSettings().subscribe(s => this.currency = s.currency || 'USD');
  }

  loadTransactions(resetToFirstPage: boolean = true): void {
    if (resetToFirstPage) this.page = 0;
    this.api.getTransactions(this.page, this.pageSize).subscribe(result => {
      this.transactions = resetToFirstPage ? result.content : [...this.transactions, ...result.content];
      this.totalPages = result.totalPages;
      this.totalElements = result.totalElements;
    });
  }

  loadMore(): void {
    this.page++;
    this.loadTransactions(false);
  }

  get hasMore(): boolean {
    return this.page + 1 < this.totalPages;
  }

  startEdit(t: TransactionResponse): void {
    this.editingId = t.id ?? null;
    this.description = t.description;
    this.amount = t.amount;
    this.date = t.date;
    this.categoryId = t.categoryId ?? null;
  }

  cancelEdit(): void {
    this.editingId = null;
    this.description = '';
    this.amount = 0;
    this.categoryId = null;
  }

  submit(): void {
    if (!this.description || this.amount <= 0 || this.categoryId == null) {
      return;
    }
    const request: NewTransactionRequest = {
      description: this.description,
      amount: this.amount,
      date: this.date,
      categoryId: this.categoryId
    };

    if (this.editingId != null) {
      this.api.updateTransaction(this.editingId, request).subscribe(() => {
        this.cancelEdit();
        this.loadTransactions();
      });
    } else {
      this.api.createTransaction(request).subscribe(() => {
        this.description = '';
        this.amount = 0;
        this.loadTransactions();
      });
    }
  }

  removeTransaction(id: number | undefined): void {
    if (id == null) return;
    if (id === this.editingId) this.cancelEdit();
    this.api.deleteTransaction(id).subscribe(() => this.loadTransactions());
  }

  exportCsv(): void {
    this.exporting = true;
    this.api.exportTransactionsCsv().subscribe({
      next: (blob) => {
        this.exporting = false;
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'budget-butler-transactions.csv';
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.exporting = false;
      }
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files && input.files.length > 0 ? input.files[0] : null;
    this.importResult = null;
  }

  submitImport(): void {
    if (!this.selectedFile || !this.importCategoryId) return;

    this.importing = true;
    this.importResult = null;
    this.api.importTransactionsCsv(this.selectedFile, this.importCategoryId).subscribe({
      next: (result) => {
        this.importing = false;
        this.importResult = result;
        this.selectedFile = null;
        this.loadTransactions();
      },
      error: () => {
        this.importing = false;
        this.importResult = { imported: 0, skipped: 0, errors: ['Import failed. Please check the file and try again.'] };
      }
    });
  }
}
