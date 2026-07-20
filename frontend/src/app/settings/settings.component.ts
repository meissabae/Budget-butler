import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../services/api.service';
import { SettingsService } from '../services/settings.service';
import { WalletResponse } from '../models/models';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.css'
})
export class SettingsComponent implements OnInit {

  monthlySalary: number | null = null;
  salaryPaymentDay: number | null = null;
  monthlyWorkingHours: number | null = 160; // a sensible full-time default, editable
  currency = 'USD';
  salaryWalletId: number | null = null;

  wallets: WalletResponse[] = [];

  // A small curated list is easier for a beginner to work with than a giant ISO 4217 list.
  readonly currencies = ['USD', 'EUR', 'GBP', 'SAR', 'AED', 'DZD', 'MAD', 'TND', 'EGP'];

  loading = true;
  saving = false;
  successMessage = '';
  errorMessage = '';

  constructor(private settingsService: SettingsService, private api: ApiService) {
  }

  ngOnInit(): void {
    this.api.getWallets().subscribe(data => this.wallets = data);

    this.settingsService.getSettings().subscribe({
      next: (data) => {
        if (data.configured) {
          this.monthlySalary = data.monthlySalary;
          this.salaryPaymentDay = data.salaryPaymentDay;
          this.monthlyWorkingHours = data.monthlyWorkingHours;
          this.currency = data.currency || 'USD';
          this.salaryWalletId = data.salaryWalletId;
        }
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  save(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.monthlySalary || !this.salaryPaymentDay || !this.monthlyWorkingHours) {
      this.errorMessage = 'Please fill in all fields.';
      return;
    }
    if (this.salaryPaymentDay < 1 || this.salaryPaymentDay > 31) {
      this.errorMessage = 'Payment day must be between 1 and 31.';
      return;
    }

    this.saving = true;
    this.settingsService.saveSettings({
      monthlySalary: this.monthlySalary,
      salaryPaymentDay: this.salaryPaymentDay,
      monthlyWorkingHours: this.monthlyWorkingHours,
      currency: this.currency,
      salaryWalletId: this.salaryWalletId
    }).subscribe({
      next: () => {
        this.saving = false;
        this.successMessage = 'Saved! Your dashboard will now reflect these settings.';
      },
      error: () => {
        this.saving = false;
        this.errorMessage = 'Could not save your settings. Please try again.';
      }
    });
  }
}
