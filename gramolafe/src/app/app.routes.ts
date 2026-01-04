import { Routes } from '@angular/router';
import { WelcomeComponent } from './welcome/welcome';
import { LoginComponent } from './login/login.component';
import { Register} from './register/register';
import { HomeComponent } from './home/home';
import { PaymentComponent } from './payment/payment.component';
// Importamos los nuevos componentes
import { RequestResetComponent } from './password/request';
import { ResetPwdComponent } from './password/reset';

export const routes: Routes = [
  { path: '', component: WelcomeComponent },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: Register },
  { path: 'home', component: HomeComponent },
  { path: 'payment', component: PaymentComponent },
  
  // 👇 NUEVAS RUTAS
  { path: 'forgot-password', component: RequestResetComponent },
  { path: 'reset-password', component: ResetPwdComponent }
];