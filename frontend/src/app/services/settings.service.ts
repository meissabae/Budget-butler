import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { UserSettingsRequest, UserSettingsResponse } from '../models/models';

@Injectable({ providedIn: 'root' })
export class SettingsService {

  private baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {
  }

  getSettings(): Observable<UserSettingsResponse> {
    return this.http.get<UserSettingsResponse>(`${this.baseUrl}/settings`);
  }

  saveSettings(request: UserSettingsRequest): Observable<UserSettingsResponse> {
    return this.http.put<UserSettingsResponse>(`${this.baseUrl}/settings`, request);
  }
}
