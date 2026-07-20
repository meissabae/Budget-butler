import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './forgot-password.component.html',
  styleUrl: '../login/login.component.css'
})
export class ForgotPasswordComponent {

  email = '';
  loading = false;
  message = '';

  constructor(private authService: AuthService) {
  }

  submit(): void {
    this.loading = true;
    this.authService.forgotPassword(this.email).subscribe({
      next: (response) => {
        this.loading = false;
        this.message = response;
      },
      error: () => {
        this.loading = false;
        this.message = 'Something went wrong. Please try again.';
      }
    });
  }
}
