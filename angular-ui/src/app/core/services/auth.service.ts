import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface User {
  sub: string;
  preferred_username: string;
  email: string;
  name: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly currentUserSignal = signal<User | null>(null);

  readonly currentUser = this.currentUserSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.currentUserSignal() !== null);

  constructor(
    private http: HttpClient,
    private router: Router
  ) {}

  async checkAuth(): Promise<boolean> {
    try {
      const user = await firstValueFrom(
        this.http.get<User>(`${environment.bffUrl}/user`)
      );
      this.currentUserSignal.set(user);
      return true;
    } catch {
      this.currentUserSignal.set(null);
      return false;
    }
  }

  login(): void {
    window.location.href = `${environment.bffUrl}/login`;
  }

  async logout(): Promise<void> {
    window.location.href = `${environment.bffUrl}/logout`;
  }

  clearAuth(): void {
    this.currentUserSignal.set(null);
  }
}
