import { ApplicationConfig, isDevMode } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideServiceWorker } from '@angular/service-worker';
import { routes } from './app.routes';
import { authInterceptor } from './interceptors/auth.interceptor';

/**
 * In older Angular apps you'd register things like routing and HttpClient inside
 * an NgModule. With "standalone" Angular (what we're using here), we register them
 * once in this config object instead - simpler, less boilerplate.
 *
 * withInterceptors([authInterceptor]) plugs our JWT interceptor into every HTTP call
 * made through Angular's HttpClient, app-wide.
 *
 * provideServiceWorker registers the PWA service worker - but only in production
 * (isDevMode() is false when built with `ng build`). Running it during `ng serve`
 * would cache your local dev files and cause confusing "why isn't my change showing up"
 * bugs, so it's disabled there on purpose.
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideServiceWorker('ngsw-worker.js', {
      enabled: !isDevMode(),
      registrationStrategy: 'registerWhenStable:30000'
    })
  ]
};
