import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './services/auth.service';

/**
 * The ROOT component. It's always on screen and contains the navigation bar.
 * <router-outlet> is where Angular swaps in whichever page component matches the current URL.
 *
 * "standalone: true" means this component does NOT need to be declared inside an NgModule -
 * it lists its own dependencies (RouterLink, RouterOutlet...) directly in "imports".
 */
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="shell">
      <nav class="navbar">
        <div class="brand">🎩 Budget Butler</div>

        <!-- Only show the main navigation once the user is logged in -->
        <div class="links" *ngIf="authService.isLoggedIn$ | async">
          <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{exact: true}">Dashboard</a>
          <a routerLink="/transactions" routerLinkActive="active">Transactions</a>
          <a routerLink="/recurring" routerLinkActive="active">Recurring</a>
          <a routerLink="/categories" routerLinkActive="active">Categories</a>
          <a routerLink="/wallets" routerLinkActive="active">Wallets</a>
          <a routerLink="/dreams" routerLinkActive="active">Dream</a>
          <a routerLink="/billing" routerLinkActive="active">Billing</a>
          <a routerLink="/settings" routerLinkActive="active">Settings</a>
          <button class="logout-btn" (click)="logout()">Log out</button>
        </div>
      </nav>

      <!-- Install banner - only appears when the browser signals the app is installable
           (Chrome/Edge on Android and desktop). iOS Safari doesn't support this event;
           see the "Add to Home Screen" instructions we show instead on iOS. -->
      <div class="install-banner" *ngIf="showInstallBanner">
        <span>📲 Install Budget Butler for quick access, like a real app.</span>
        <div class="install-actions">
          <button class="install-btn" (click)="installApp()">Install</button>
          <button class="dismiss-btn" (click)="dismissInstallBanner()">Not now</button>
        </div>
      </div>

      <main class="content">
        <router-outlet></router-outlet>
      </main>
    </div>
  `,
  styles: [`
    .shell { min-height: 100vh; }

    .navbar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px 32px;
      background: var(--color-ink);
      color: white;
    }

    .brand {
      font-family: var(--font-display);
      font-size: 20px;
      font-weight: 600;
    }

    .links { display: flex; gap: 24px; }

    .links a {
      color: #C9CCE0;
      text-decoration: none;
      font-weight: 500;
      font-size: 14px;
      padding: 6px 4px;
      border-bottom: 2px solid transparent;
    }

    .links a:hover { color: white; }

    .links a.active {
      color: var(--color-yellow);
      border-bottom: 2px solid var(--color-yellow);
    }

    .logout-btn {
      background: transparent;
      border: 1px solid #4A4E76;
      color: #C9CCE0;
      padding: 6px 14px;
      font-size: 13px;
      border-radius: 8px;
    }

    .logout-btn:hover {
      border-color: var(--color-coral);
      color: var(--color-coral);
    }

    .content {
      max-width: 900px;
      margin: 0 auto;
      padding: 32px 20px 60px;
    }

    .install-banner {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
      background: #FFF6E0;
      border-bottom: 1px solid var(--color-yellow);
      padding: 10px 24px;
      font-size: 13px;
      animation: fadeIn 0.3s ease both;
    }

    .install-actions {
      display: flex;
      gap: 8px;
    }

    .install-btn {
      background: var(--color-yellow);
      color: var(--color-ink);
      padding: 6px 14px;
      font-size: 13px;
    }

    .dismiss-btn {
      background: transparent;
      color: var(--color-muted);
      padding: 6px 10px;
      font-size: 13px;
    }
  `]
})
export class AppComponent {

  showInstallBanner = false;
  private deferredInstallPrompt: any = null;

  // "public" here (implicit by default) so the template above can read authService.isLoggedIn$ directly.
  constructor(public authService: AuthService, private router: Router) {
    // Chrome/Edge fire this event when the app meets install criteria (manifest + service
    // worker + HTTPS). We intercept it so we can show our OWN styled button instead of
    // waiting for the browser's default (and easy-to-miss) install icon.
    window.addEventListener('beforeinstallprompt', (event: any) => {
      event.preventDefault();
      this.deferredInstallPrompt = event;
      if (!sessionStorage.getItem('installBannerDismissed')) {
        this.showInstallBanner = true;
      }
    });

    window.addEventListener('appinstalled', () => {
      this.showInstallBanner = false;
      this.deferredInstallPrompt = null;
    });
  }

  installApp(): void {
    if (!this.deferredInstallPrompt) return;
    this.deferredInstallPrompt.prompt();
    this.deferredInstallPrompt = null;
    this.showInstallBanner = false;
  }

  dismissInstallBanner(): void {
    this.showInstallBanner = false;
    sessionStorage.setItem('installBannerDismissed', 'true');
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
