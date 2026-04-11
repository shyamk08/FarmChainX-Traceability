import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export type AuthState = {
  token: string | null;
  role: string | null;
  name: string | null;
  email: string | null;
};

const initial: AuthState = {
  token: localStorage.getItem('fcx_token'),
  role: localStorage.getItem('fcx_role'),
  name: localStorage.getItem('fcx_name'),
  email: localStorage.getItem('fcx_email')
};

@Injectable({ providedIn: 'root' })
export class AuthService {

  private _auth$ = new BehaviorSubject<AuthState>(initial);
  readonly auth$ = this._auth$.asObservable();

  // Current values
  get current() {
    return this._auth$.value;
  }

  isLoggedIn(): boolean {
    return !!this.current.token;
  }

  getRole(): string | null {
    return this.current.role;
  }

  getName(): string | null {
    return this.current.name || this.current.email;
  }

  getEmail(): string | null {
    return this.current.email;
  }

  // ADD THESE METHODS — THIS FIXES THE ERROR
  isAdmin(): boolean {
    return this.current.role === 'ROLE_ADMIN';
  }

  hasRole(role: string): boolean {
    return this.current.role === role;
  }

  // Login success — call this after login API
  setAuthFromResponse(res: { token?: string; role?: string; name?: string; email?: string }) {
    const token = res?.token || null;
    const role = res?.role || null;
    const name = res?.name || null;
    const email = res?.email || null;

    if (token) localStorage.setItem('fcx_token', token);
    if (role) localStorage.setItem('fcx_role', role);
    if (name) localStorage.setItem('fcx_name', name);
    if (email) localStorage.setItem('fcx_email', email);

    this._auth$.next({ token, role, name, email });
  }

  // Logout
  logout() {
    localStorage.removeItem('fcx_token');
    localStorage.removeItem('fcx_role');
    localStorage.removeItem('fcx_name');
    localStorage.removeItem('fcx_email');

    this._auth$.next({ token: null, role: null, name: null, email: null });
  }
}
