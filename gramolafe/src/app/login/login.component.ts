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
  
  // 👇 Variable nueva para el cliente
  barIdInput = '';

  constructor(private userService: User, private router: Router) {}

  login() {
    this.userService.login(this.email, this.pwd).subscribe({
      next: (user) => {
        localStorage.setItem('currentUser', JSON.stringify(user));
        this.router.navigate(['/home']);
      },
      error: (err) => alert("Error: " + (err.error?.message || "Login fallido"))
    });
  }

  // 👇 Método nuevo para el cliente
  enterAsClient() {
    if(!this.barIdInput.trim()) return alert("Por favor, escribe el email del bar.");
    // Redirige al portal del cliente con el ID en la URL
    this.router.navigate(['/jukebox', this.barIdInput]);
  }
}