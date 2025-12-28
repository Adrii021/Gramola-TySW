import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './home.html',
  styleUrls: ['./home.css']
})
export class HomeComponent implements OnInit {
  barName: string = "Usuario";

  constructor(private router: Router) {}

  ngOnInit() {
    // Recuperar los datos del usuario guardado
    const userJson = localStorage.getItem('currentUser');
    if (userJson) {
      const user = JSON.parse(userJson);
      this.barName = user.bar;
    } else {
      // Si no hay usuario logueado, echarlo al login
      this.router.navigate(['/login']);
    }
  }

  logout() {
    localStorage.removeItem('currentUser'); // Borrar sesión
    this.router.navigate(['/login']);
  }
}