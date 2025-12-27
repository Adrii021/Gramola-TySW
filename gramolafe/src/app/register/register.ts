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
    if (this.pwd1 != this.pwd2) {
      console.error('Las contraseñas no coinciden');
      return;
    }

    if (!this.email || !this.pwd1 || !this.pwd2 || !this.bar || !this.clientId || !this.clientSecret) {
      console.error('Por favor, rellena todos los campos');
      return;
    }

    this.service.register(
      this.email, 
      this.pwd1, 
      this.pwd2, 
      this.bar, 
      this.clientId, 
      this.clientSecret
    ).subscribe(
      ok => {
        console.log('Registro exitoso', ok);
      },
      error => {
        console.error('Error en el registro', error);
      }
    );
  }
}