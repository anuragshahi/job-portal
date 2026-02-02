import { Component, inject, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, RouterOutlet } from '@angular/router';
import { MatSidenavModule, MatSidenav } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    RouterOutlet,
    MatSidenavModule,
    MatToolbarModule,
    MatListModule,
    MatIconModule,
    MatButtonModule
  ],
  template: `
    <mat-sidenav-container class="sidenav-container">
      <mat-sidenav #sidenav [mode]="isMobile ? 'over' : 'side'" [opened]="!isMobile" class="sidenav">
        <mat-toolbar color="primary">
          <span>SEC Microservice</span>
        </mat-toolbar>
        <mat-nav-list>
          <a mat-list-item routerLink="/dashboard" routerLinkActive="active" (click)="closeIfMobile()">
            <mat-icon matListItemIcon>dashboard</mat-icon>
            <span matListItemTitle>Dashboard</span>
          </a>
          <a mat-list-item routerLink="/profile" routerLinkActive="active" (click)="closeIfMobile()">
            <mat-icon matListItemIcon>person</mat-icon>
            <span matListItemTitle>Profile</span>
          </a>
          <a mat-list-item routerLink="/orders" routerLinkActive="active" (click)="closeIfMobile()">
            <mat-icon matListItemIcon>shopping_cart</mat-icon>
            <span matListItemTitle>Orders</span>
          </a>
        </mat-nav-list>
      </mat-sidenav>

      <mat-sidenav-content>
        <mat-toolbar color="primary">
          @if (isMobile) {
            <button mat-icon-button (click)="sidenav.toggle()">
              <mat-icon>menu</mat-icon>
            </button>
          }
          <span>SEC Microservice</span>
          <span class="spacer"></span>
          @if (authService.currentUser(); as user) {
            <span class="user-name">{{ user.preferred_username }}</span>
          }
          <button mat-icon-button (click)="logout()" matTooltip="Logout">
            <mat-icon>logout</mat-icon>
          </button>
        </mat-toolbar>

        <div class="content-container">
          <router-outlet></router-outlet>
        </div>
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styles: [`
    .sidenav-container {
      height: 100vh;
    }

    .sidenav {
      width: 250px;
    }

    .sidenav mat-toolbar {
      position: sticky;
      top: 0;
      z-index: 1;
    }

    .active {
      background-color: rgba(0, 0, 0, 0.1);
    }

    .user-name {
      margin-right: 16px;
      font-size: 14px;
    }

    mat-sidenav-content mat-toolbar {
      position: sticky;
      top: 0;
      z-index: 1;
    }
  `]
})
export class MainLayoutComponent {
  @ViewChild('sidenav') sidenav!: MatSidenav;

  authService = inject(AuthService);
  private breakpointObserver = inject(BreakpointObserver);

  isMobile = false;

  constructor() {
    this.breakpointObserver.observe([Breakpoints.Handset]).subscribe(result => {
      this.isMobile = result.matches;
    });
  }

  closeIfMobile(): void {
    if (this.isMobile) {
      this.sidenav.close();
    }
  }

  logout(): void {
    this.authService.logout();
  }
}
