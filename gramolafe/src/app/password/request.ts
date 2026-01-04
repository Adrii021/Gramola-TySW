import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';

@Component({
  selector: 'app-request-reset',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="auth">
      <div class="auth-card">
        <h2> Recuperar Contraseña</h2>
        <p class="subtitle">Introduce tu correo y te enviaremos un enlace de recuperación.</p>
        
        <div class="field">
          <label>Correo electrónico</label>
          <input 
            type="email" 
            [(ngModel)]="email" 
            placeholder="tu@email.com" 
            class="input-modern"
          >
        </div>
        
        <button (click)="sendRequest()" class="btn-primary">
          Enviar Enlace
        </button>

        <div class="auth-footer">
            <a routerLink="/login">Volver al Login</a>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .auth {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 20px;
      background: radial-gradient(circle at top, rgba(29,185,84,0.18), transparent 45%), #020617;
      font-family: 'Inter', sans-serif;
    }

    .auth-card {
      width: 100%;
      max-width: 420px;
      background: rgba(2, 6, 23, 0.88);
      padding: 50px 45px;
      border-radius: 22px;
      box-shadow: 0 30px 60px rgba(0,0,0,.55);
      backdrop-filter: blur(12px);
      border: 1px solid rgba(255,255,255,0.05);
    }

    h2 {
      text-align: center;
      color: #e5e7eb;
      font-size: 1.8rem;
      margin-bottom: 10px;
      margin-top: 0;
    }

    .subtitle {
      text-align: center;
      color: #9ca3af;
      font-size: 0.95rem;
      margin-bottom: 35px;
    }

    .field { margin-bottom: 22px; }
    
    .field label {
      display: block;
      margin-bottom: 6px;
      color: #9ca3af;
      font-size: 0.9rem;
    }

    .input-modern {
      width: 100%;
      padding: 12px 14px;
      border-radius: 10px;
      border: 1px solid #1f2937;
      background: #020617;
      color: #e5e7eb;
      font-size: 0.95rem;
      box-sizing: border-box;
      transition: border-color .2s, box-shadow .2s;
    }

    .input-modern:focus {
      outline: none;
      border-color: #1db954;
      box-shadow: 0 0 0 3px rgba(29,185,84,0.25);
    }

    .btn-primary {
      width: 100%;
      background: #1db954;
      color: #000;
      border: none;
      padding: 14px;
      border-radius: 999px;
      font-size: 1rem;
      font-weight: 600;
      cursor: pointer;
      margin-top: 10px;
      transition: transform .2s, box-shadow .2s;
    }

    .btn-primary:hover {
      transform: translateY(-2px);
      box-shadow: 0 10px 20px rgba(29,185,84,0.3);
    }
  `]
})
export class RequestResetComponent {
  email: string = "";

  constructor(private http: HttpClient, private router: Router) {}

  sendRequest() {
    if (!this.email) return alert("Escribe un correo");
    
    this.http.post("http://localhost:8080/users/request-reset", { email: this.email })
      .subscribe({
        next: () => {
          alert("✅ ¡Correo enviado! Mira la consola de Java para ver el link simulado.");
          this.router.navigate(['/login']);
        },
        error: (e) => alert("Error: " + e.message)
      });
  }
}