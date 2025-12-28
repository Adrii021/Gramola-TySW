import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { User } from '../user';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule], 
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {
  email = '';
  pwd = '';

  constructor(private userService: User, private router: Router) {}



  login() {
    this.userService.login(this.email, this.pwd).subscribe({
      next: (user) => {
        // 1. Guardamos el usuario en la memoria del navegador para no perderlo
        localStorage.setItem('currentUser', JSON.stringify(user));
        
        // 2. Redirigimos a la pantalla principal
        this.router.navigate(['/home']);
      },
      error: (err) => {
        alert("Error: " + (err.error?.message || "Login fallido"));
      }
    });
  }
}