import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UserProfile } from './profile.model';

@Injectable({
  providedIn: 'root'
})
export class ProfileService {
  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.bffUrl}/api/profile`;

  async getProfile(): Promise<UserProfile | null> {
    try {
      return await firstValueFrom(this.http.get<UserProfile>(this.apiUrl));
    } catch (error) {
      if (error instanceof HttpErrorResponse && error.status === 404) {
        return null;
      }
      throw error;
    }
  }

  async createProfile(profile: Omit<UserProfile, 'id'>): Promise<UserProfile> {
    return firstValueFrom(this.http.post<UserProfile>(this.apiUrl, profile));
  }

  async deleteProfile(): Promise<void> {
    await firstValueFrom(this.http.delete<void>(this.apiUrl));
  }
}
