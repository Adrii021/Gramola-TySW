import { Routes } from '@angular/router';
import { Register } from './register/register';
import { PaymentComponent } from './payment/payment.component';

export const routes: Routes = [
    { path: 'register', component: Register },
    { path: 'payment', component: PaymentComponent },
    { path: '', redirectTo: '/register', pathMatch: 'full' }
];