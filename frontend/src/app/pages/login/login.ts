import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { environment } from '../../../environments/environment';

@Component({
  standalone: true,
  selector: 'app-login',
  templateUrl: './login.html',
  imports: [CommonModule, FormsModule, RouterModule]
})
export class Login {
  email = '';
  password = '';

  constructor(
    private http: HttpClient,
    private router: Router,
    private auth: AuthService   // ✔ Inject AuthService
  ) { }

  login() {
    this.http.post<any>(`${environment.apiUrl}/auth/login`, {
      email: this.email,
      password: this.password
    }).subscribe({
      next: (res) => {
        // ✔ Store into service (also updates localStorage)
        this.auth.setAuthFromResponse(res);

        alert('Login successful ✅');
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        const msg = err?.error?.error || 'Invalid email or password';
        alert(msg);
      }
    });
  }
}
