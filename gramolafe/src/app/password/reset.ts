import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router, ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-reset-pwd',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="auth">
      <div class="auth-card">
        <h2> Nueva Contraseña</h2>
        <p class="subtitle">Establece tu nueva clave de acceso.</p>
        
        <div class="field">
          <label>Nueva Contraseña</label>
          <input 
            type="password" 
            [(ngModel)]="pwd1" 
            placeholder="Mínimo 4 caracteres"
            class="input-modern"
          >
        </div>

        <div class="field">
          <label>Repetir Contraseña</label>
          <input 
            type="password" 
            [(ngModel)]="pwd2" 
            placeholder="Confirma la contraseña"
            class="input-modern"
          >
        </div>
        
        <button (click)="resetPwd()" class="btn-primary">
          Cambiar Contraseña
        </button>
      </div>
    </div>
  `,
  styles: [`
    /* Mismos estilos Dark Mode */
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
export class ResetPwdComponent implements OnInit {
  email: string = "";
  token: string = "";
  pwd1: string = "";
  pwd2: string = "";

  constructor(private route: ActivatedRoute, private http: HttpClient, private router: Router) {}

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.email = params['email'];
      this.token = params['token'];
    });
  }

  resetPwd() {
    if (this.pwd1 !== this.pwd2) return alert("Las contraseñas no coinciden");
    
    let info = {
      email: this.email,
      token: this.token,
      pwd1: this.pwd1,
      pwd2: this.pwd2
    };

    this.http.post("http://localhost:8080/users/reset-pwd", info)
      .subscribe({
        next: () => {
          alert("¡Contraseña cambiada con éxito! Ya puedes entrar.");
          this.router.navigate(['/login']);
        },
        error: (e) => alert("Error: " + e.message)
      });
  }
}