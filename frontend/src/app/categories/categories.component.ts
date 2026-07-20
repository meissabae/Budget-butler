import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../services/api.service';
import { SettingsService } from '../services/settings.service';
import { Category, WalletResponse } from '../models/models';

@Component({
  selector: 'app-categories',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './categories.component.html',
  styleUrl: './categories.component.css'
})
export class CategoriesComponent implements OnInit {

  categories: Category[] = [];
  wallets: WalletResponse[] = [];
  currency = 'USD';

  // Bound to the form. When editingId is set, submitting the form UPDATES that
  // category instead of creating a new one - the same form doubles as both.
  form: Category = { name: '', monthlyLimit: 0, walletId: null };
  editingId: number | null = null;

  constructor(private api: ApiService, private settingsService: SettingsService) {
  }

  ngOnInit(): void {
    this.loadCategories();
    this.api.getWallets().subscribe(data => this.wallets = data);
    this.settingsService.getSettings().subscribe(s => this.currency = s.currency || 'USD');
  }

  loadCategories(): void {
    this.api.getCategories().subscribe(data => this.categories = data);
  }

  startEdit(category: Category): void {
    this.editingId = category.id ?? null;
    this.form = { ...category };
  }

  cancelEdit(): void {
    this.editingId = null;
    this.form = { name: '', monthlyLimit: 0, walletId: null };
  }

  submit(): void {
    if (!this.form.name || this.form.monthlyLimit <= 0 || !this.form.walletId) {
      return;
    }

    if (this.editingId != null) {
      this.api.updateCategory(this.editingId, this.form).subscribe(() => {
        this.cancelEdit();
        this.loadCategories();
      });
    } else {
      this.api.createCategory(this.form).subscribe(() => {
        this.form = { name: '', monthlyLimit: 0, walletId: null };
        this.loadCategories();
      });
    }
  }

  removeCategory(id: number | undefined): void {
    if (id == null) return;
    if (id === this.editingId) this.cancelEdit();
    this.api.deleteCategory(id).subscribe(() => this.loadCategories());
  }
}
