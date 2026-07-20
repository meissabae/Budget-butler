import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../services/api.service';
import { SettingsService } from '../services/settings.service';
import { Category, RecurringTransactionRequest, RecurringTransactionResponse } from '../models/models';

@Component({
  selector: 'app-recurring',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './recurring.component.html',
  styleUrl: './recurring.component.css'
})
export class RecurringComponent implements OnInit {

  rules: RecurringTransactionResponse[] = [];
  categories: Category[] = [];
  currency = 'USD';

  form: RecurringTransactionRequest = { description: '', amount: 0, dayOfMonth: 1, categoryId: 0, active: true };
  editingId: number | null = null;

  constructor(private api: ApiService, private settingsService: SettingsService) {
  }

  ngOnInit(): void {
    this.loadRules();
    this.api.getCategories().subscribe(data => this.categories = data);
    this.settingsService.getSettings().subscribe(s => this.currency = s.currency || 'USD');
  }

  loadRules(): void {
    this.api.getRecurringTransactions().subscribe(data => this.rules = data);
  }

  startEdit(rule: RecurringTransactionResponse): void {
    this.editingId = rule.id;
    this.form = {
      description: rule.description,
      amount: rule.amount,
      dayOfMonth: rule.dayOfMonth,
      categoryId: rule.categoryId ?? 0,
      active: rule.active
    };
  }

  cancelEdit(): void {
    this.editingId = null;
    this.form = { description: '', amount: 0, dayOfMonth: 1, categoryId: 0, active: true };
  }

  submit(): void {
    if (!this.form.description || this.form.amount <= 0 || !this.form.categoryId) return;

    if (this.editingId != null) {
      this.api.updateRecurringTransaction(this.editingId, this.form).subscribe(() => {
        this.cancelEdit();
        this.loadRules();
      });
    } else {
      this.api.createRecurringTransaction(this.form).subscribe(() => {
        this.cancelEdit();
        this.loadRules();
      });
    }
  }

  toggleActive(rule: RecurringTransactionResponse): void {
    const request: RecurringTransactionRequest = {
      description: rule.description,
      amount: rule.amount,
      dayOfMonth: rule.dayOfMonth,
      categoryId: rule.categoryId ?? 0,
      active: !rule.active
    };
    this.api.updateRecurringTransaction(rule.id, request).subscribe(() => this.loadRules());
  }

  removeRule(id: number): void {
    if (id === this.editingId) this.cancelEdit();
    this.api.deleteRecurringTransaction(id).subscribe(() => this.loadRules());
  }
}
