import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { Subject, takeUntil, filter } from 'rxjs';
import { ApiService } from './services/api.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  title = 'Project Management';
  isLoggedIn = false;
  showHeader = false;

  constructor(
    private apiService: ApiService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Check login status
    this.isLoggedIn = this.apiService.isLoggedIn();

    // Listen to route changes to show/hide header
    this.router.events
      .pipe(
        filter(event => event instanceof NavigationEnd),
        takeUntil(this.destroy$)
      )
      .subscribe((event) => {
        if (event instanceof NavigationEnd) {
          this.showHeader = !event.url.includes('/login');
        }
      });

    // Listen to authentication changes
    this.apiService.currentUser$
      .pipe(takeUntil(this.destroy$))
      .subscribe(user => {
        this.isLoggedIn = !!user;
      });

    // Load current user if logged in
    if (this.isLoggedIn) {
      this.loadCurrentUser();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadCurrentUser(): void {
    this.apiService.getCurrentUser()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (user) => {
          // User loaded successfully
        },
        error: (error) => {
          console.error('Error loading user:', error);
          // If token is invalid, logout
          this.apiService.logout();
          this.router.navigate(['/login']);
        }
      });
  }
}
