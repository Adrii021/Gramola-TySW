import { Component } from '@angular/core';
import { CommonModule } from '@angular/common'; 
import { FormsModule } from '@angular/forms'; 
import { User } from '../user';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './register.html',
  styleUrl: './register.css'
})
export class Register {

  email? : string
  pwd1? : string
  pwd2? : string
  bar? : string
  clientId? : string
  clientSecret? : string

  constructor(private service : User) { }

  registrar() {
  // 1. Comprobar contraseñas
  if (this.pwd1 != this.pwd2) {
    alert('Las contraseñas no coinciden'); // <--- CAMBIO AQUÍ
    return;
  }

  // 2. Comprobar campos vacíos
  if (!this.email || !this.pwd1 || !this.pwd2 || !this.bar || !this.clientId || !this.clientSecret) {
    alert('Por favor, rellena todos los campos'); // <--- CAMBIO AQUÍ
    return;
  }

  // 3. Enviar al servidor
  this.service.register(
    this.email, 
    this.pwd1, 
    this.pwd2, 
    this.bar, 
    this.clientId, 
    this.clientSecret
  ).subscribe({
    next: (ok) => {
      console.log('Registro exitoso', ok);
      alert('¡Registro recibido! Revisa la consola de Java para ver el link de confirmación.');
    },
    error: (error) => {
      console.error('Error en el registro', error);
      alert('Error al registrar: ' + (error.error?.message || 'Error desconocido'));
    }
  });
}
}