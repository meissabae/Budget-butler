import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../services/api.service';
import { SettingsService } from '../services/settings.service';
import { WalletResponse } from '../models/models';

@Component({
  selector: 'app-wallets',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './wallets.component.html',
  styleUrl: './wallets.component.css'
})
export class WalletsComponent implements OnInit {

  wallets: WalletResponse[] = [];
  newWalletName = '';
  currency = 'USD';

  editingId: number | null = null;
  editName = '';

  // A small "add funds" form per wallet, tracked by which wallet id is currently open.
  depositOpenFor: number | null = null;
  depositAmount: number | null = null;
  depositNote = '';

  creditingSalary = false;
  salaryMessage = '';

  constructor(private api: ApiService, private settingsService: SettingsService) {
  }

  ngOnInit(): void {
    this.loadWallets();
    this.settingsService.getSettings().subscribe(s => this.currency = s.currency || 'USD');
  }

  loadWallets(): void {
    this.api.getWallets().subscribe(data => this.wallets = data);
  }

  addWallet(): void {
    if (!this.newWalletName.trim()) return;
    this.api.createWallet({ name: this.newWalletName }).subscribe(() => {
      this.newWalletName = '';
      this.loadWallets();
    });
  }

  startEdit(wallet: WalletResponse): void {
    this.editingId = wallet.id;
    this.editName = wallet.name;
  }

  cancelEdit(): void {
    this.editingId = null;
    this.editName = '';
  }

  saveEdit(): void {
    if (this.editingId == null || !this.editName.trim()) return;
    this.api.updateWallet(this.editingId, { name: this.editName }).subscribe(() => {
      this.cancelEdit();
      this.loadWallets();
    });
  }

  removeWallet(id: number): void {
    if (id === this.editingId) this.cancelEdit();
    this.api.deleteWallet(id).subscribe(() => this.loadWallets());
  }

  toggleDeposit(id: number): void {
    this.depositOpenFor = this.depositOpenFor === id ? null : id;
    this.depositAmount = null;
    this.depositNote = '';
  }

  submitDeposit(id: number): void {
    if (!this.depositAmount || this.depositAmount <= 0) return;
    this.api.depositToWallet(id, this.depositAmount, this.depositNote).subscribe(() => {
      this.depositOpenFor = null;
      this.loadWallets();
    });
  }

  creditSalaryNow(): void {
    this.creditingSalary = true;
    this.salaryMessage = '';
    this.api.creditSalaryNow().subscribe({
      next: (wallet) => {
        this.creditingSalary = false;
        this.salaryMessage = `Salary credited! ${wallet.name} balance updated.`;
        this.loadWallets();
      },
      error: (err) => {
        this.creditingSalary = false;
        this.salaryMessage = err.error || 'Could not credit salary. Check your Settings.';
      }
    });
  }
}
