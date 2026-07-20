import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

/**
 * An "interceptor" sits in the middle of every single HTTP request the app makes,
 * and gets a chance to modify it before it's sent. This one's job: if we have a token
 * stored, attach it as an "Authorization: Bearer <token>" header automatically.
 *
 * Thanks to this, none of our components (dashboard, categories, etc.) need to know
 * anything about tokens - they just call the ApiService like normal, and the token
 * gets attached behind the scenes.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();

  // Don't attach a token to the login/register calls themselves - they happen before we have one.
  if (token && !req.url.includes('/auth/')) {
    const clonedRequest = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
    return next(clonedRequest);
  }

  return next(req);
};
