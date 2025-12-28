import { Routes } from '@angular/router';
import { Register } from './register/register';
import { PaymentComponent } from './payment/payment.component';
import { LoginComponent } from './login/login.component';
import { HomeComponent } from './home/home'; 
import { WelcomeComponent } from './welcome/welcome'; // <--- 1. Importar

export const routes: Routes = [
    { path: 'welcome', component: WelcomeComponent }, // <--- 2. Nueva ruta
    { path: 'register', component: Register },
    { path: 'payment', component: PaymentComponent },
    { path: 'login', component: LoginComponent },
    { path: 'home', component: HomeComponent },
    
    // 👇 3. CAMBIO IMPORTANTE: Redirigir a 'welcome' en vez de 'register'
    { path: '', redirectTo: '/welcome', pathMatch: 'full' } 
];