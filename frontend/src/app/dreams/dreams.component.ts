import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../services/api.service';
import { SettingsService } from '../services/settings.service';
import { Dream } from '../models/models';

@Component({
  selector: 'app-dreams',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dreams.component.html',
  styleUrl: './dreams.component.css'
})
export class DreamsComponent implements OnInit {

  dreams: Dream[] = [];
  currency = 'USD';

  form: Dream = { name: '', targetAmount: 0 };
  editingId: number | null = null;

  constructor(private api: ApiService, private settingsService: SettingsService) {
  }

  ngOnInit(): void {
    this.loadDreams();
    this.settingsService.getSettings().subscribe(s => this.currency = s.currency || 'USD');
  }

  loadDreams(): void {
    this.api.getDreams().subscribe(data => this.dreams = data);
  }

  startEdit(dream: Dream): void {
    this.editingId = dream.id ?? null;
    this.form = { name: dream.name, targetAmount: dream.targetAmount };
  }

  cancelEdit(): void {
    this.editingId = null;
    this.form = { name: '', targetAmount: 0 };
  }

  submit(): void {
    if (!this.form.name || this.form.targetAmount <= 0) return;

    if (this.editingId != null) {
      this.api.updateDream(this.editingId, this.form).subscribe(() => {
        this.cancelEdit();
        this.loadDreams();
      });
    } else {
      this.api.createDream(this.form).subscribe(() => {
        this.form = { name: '', targetAmount: 0 };
        this.loadDreams();
      });
    }
  }

  removeDream(id: number | undefined): void {
    if (id == null) return;
    if (id === this.editingId) this.cancelEdit();
    this.api.deleteDream(id).subscribe(() => this.loadDreams());
  }
}
