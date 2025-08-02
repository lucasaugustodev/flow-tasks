import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  constructor(private router: Router) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Get token from localStorage
    const token = localStorage.getItem('token');

    // Clone request and add authorization header if token exists
    let authReq = req;
    if (token && req.url.includes('/api/')) {
      authReq = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      console.log('Adding auth header to API request:', authReq.url);
    } else if (!req.url.includes('/api/')) {
      console.log('Skipping auth header for non-API request:', req.url);
    } else {
      console.log('No token found for API request:', req.url);
    }

    // Handle the request and catch errors
    return next.handle(authReq).pipe(
      catchError((error: HttpErrorResponse) => {
        console.error('HTTP Error:', error);

        // Only handle 401 for API requests, not for login requests
        if (error.status === 401 && req.url.includes('/api/') && !req.url.includes('/auth/signin')) {
          console.log('Unauthorized API request - clearing token and redirecting to login');
          localStorage.removeItem('token');
          // Only redirect if not already on login page
          if (!this.router.url.includes('/login')) {
            this.router.navigate(['/login']);
          }
        }

        return throwError(() => error);
      })
    );
  }
}
