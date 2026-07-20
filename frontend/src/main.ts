import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { appConfig } from './app/app.config';

// This is the very first line of code that runs in the browser.
// It boots up our root AppComponent using the configuration (routes, HTTP client, etc.)
// defined in app.config.ts.
bootstrapApplication(AppComponent, appConfig).catch((err) => console.error(err));
