import { Routes } from '@angular/router';
import { WelcomeComponent } from './welcome/welcome';
import { LoginComponent } from './login/login.component';
import { Register } from './register/register';
import { HomeComponent } from './home/home';
import { PaymentComponent } from './payment/payment.component';
import { RequestResetComponent } from './password/request';
import { ResetPwdComponent } from './password/reset';
import { ClientComponent } from './client/client.component';

export const routes: Routes = [
  { path: '', component: WelcomeComponent },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: Register },
  { path: 'home', component: HomeComponent }, // <- SOLO PARA DUEÑOS
  { path: 'payment', component: PaymentComponent },
  { path: 'forgot-password', component: RequestResetComponent },
  { path: 'reset-password', component: ResetPwdComponent },
  
  // 👇 NUEVA RUTA PARA EL CLIENTE (Pública)
  { path: 'jukebox/:id', component: ClientComponent },
  
  { path: '**', redirectTo: '' }
];