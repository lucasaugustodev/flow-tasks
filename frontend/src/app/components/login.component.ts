import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ApiService } from '../services/api.service';
import { LoginRequest } from '../models/task.model';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {
  loginForm: LoginRequest = {
    username: '',
    password: ''
  };

  registerForm = {
    username: '',
    email: '',
    fullName: '',
    password: '',
    confirmPassword: ''
  };

  isLoginMode = true;
  loading = false;
  error = '';

  constructor(
    private apiService: ApiService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Se já estiver logado, redirecionar
    if (this.apiService.isLoggedIn()) {
      this.router.navigate(['/dashboard']);
    }
  }

  toggleMode(): void {
    this.isLoginMode = !this.isLoginMode;
    this.error = '';
    this.resetForms();
  }

  private resetForms(): void {
    this.loginForm = { username: '', password: '' };
    this.registerForm = {
      username: '',
      email: '',
      fullName: '',
      password: '',
      confirmPassword: ''
    };
  }

  onLogin(): void {
    if (!this.loginForm.username.trim() || !this.loginForm.password.trim()) {
      this.error = 'Por favor, preencha todos os campos';
      return;
    }

    this.loading = true;
    this.error = '';

    console.log('Attempting login with:', this.loginForm.username);

    this.apiService.login(this.loginForm).subscribe({
      next: (response) => {
        console.log('Login successful:', response);
        this.apiService.setToken(response.accessToken);
        this.loading = false;
        this.router.navigate(['/dashboard']);
      },
      error: (error) => {
        console.error('Login error:', error);
        this.error = 'Usuário ou senha inválidos';
        this.loading = false;
      }
    });
  }

  onRegister(): void {
    if (!this.isRegisterFormValid()) {
      return;
    }

    this.loading = true;
    this.error = '';

    console.log('Attempting registration with:', this.registerForm.username);

    const signupData = {
      username: this.registerForm.username.trim(),
      email: this.registerForm.email.trim(),
      password: this.registerForm.password,
      fullName: this.registerForm.fullName.trim()
    };

    this.apiService.register(signupData).subscribe({
      next: (response) => {
        console.log('Registration successful:', response);
        this.loading = false;
        this.error = '';

        // Show success message
        alert('Usuário registrado com sucesso! Você pode fazer login agora.');

        // Switch to login mode
        this.isLoginMode = true;
        this.resetForms();
      },
      error: (error) => {
        console.error('Registration error:', error);
        this.loading = false;

        if (error.status === 400 && error.error?.message) {
          if (error.error.message.includes('Username is already taken')) {
            this.error = 'Este nome de usuário já está em uso';
          } else if (error.error.message.includes('Email is already in use')) {
            this.error = 'Este email já está em uso';
          } else {
            this.error = error.error.message;
          }
        } else {
          this.error = 'Erro ao registrar usuário. Tente novamente.';
        }
      }
    });
  }

  private isRegisterFormValid(): boolean {
    const form = this.registerForm;
    
    if (!form.username.trim() || !form.email.trim() || 
        !form.fullName.trim() || !form.password.trim()) {
      this.error = 'Por favor, preencha todos os campos';
      return false;
    }

    if (form.password !== form.confirmPassword) {
      this.error = 'As senhas não coincidem';
      return false;
    }

    if (form.password.length < 6) {
      this.error = 'A senha deve ter pelo menos 6 caracteres';
      return false;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(form.email)) {
      this.error = 'Por favor, insira um email válido';
      return false;
    }

    return true;
  }

  isLoginFormValid(): boolean {
    return this.loginForm.username.trim().length > 0 && 
           this.loginForm.password.trim().length > 0;
  }

  isRegisterFormValidForSubmit(): boolean {
    const form = this.registerForm;
    return form.username.trim().length > 0 &&
           form.email.trim().length > 0 &&
           form.fullName.trim().length > 0 &&
           form.password.trim().length > 0 &&
           form.confirmPassword.trim().length > 0 &&
           form.password === form.confirmPassword;
  }
}
