import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { PricingService } from '../services/pricing.service';

declare var Stripe: any;

@Component({
  selector: 'app-payment',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './payment.component.html',
  styleUrls: ['./payment.component.css']
})
export class PaymentComponent implements OnInit {
  token: string = "";
  email: string = ""; // Variable para guardar el email
  stripe: any;
  card: any; 
  clientSecret: string = "";
  processing: boolean = false;
  price: number = 0; 

  constructor(
    private route: ActivatedRoute, 
    private http: HttpClient, 
    private router: Router,
    private pricingService: PricingService
  ) {
    this.stripe = Stripe("pk_test_51SixjzR3ux91c7imTZjFULUGxl7oh0SaevRtgx3WqDi34rWh2DGLlC5b9i7BURyReACNsz1sCzB7DR15k2DPTjEO00CEUH7qcB");
  }

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.token = params['token'];
      this.email = params['email']; // 👇 Recuperamos el email de la URL
    });

    this.pricingService.getPrices().subscribe({
      next: (prices) => {
        const plan = prices.find(p => p.type === 'MONTHLY');
        if (plan) this.price = plan.price;
      }
    });
  }

  prepay() {
    // 1. Pedir intento de pago al backend
    let info = { 
      token: this.token, 
      amount: this.price 
    };

    // Usamos POST y enviamos el precio
    this.http.post<any>("http://localhost:8080/payments/prepay", info).subscribe({
      next: (res) => {
        this.clientSecret = res.clientSecret;
        
        // 2. Mostrar formulario solo si tenemos éxito
        document.getElementById("prepay-btn")!.style.display = "none";
        document.getElementById("payment-form")!.style.display = "block";

        this.mountStripeElements();
      },
      error: (err) => {
        alert("Error al iniciar el pago: " + err.message);
      }
    });
  }

  mountStripeElements() {
    setTimeout(() => {
        let elements = this.stripe.elements();
        let style = { base: { color: "#32325d", fontFamily: 'Arial, sans-serif', fontSize: "16px" } };
        
        this.card = elements.create("card", { style: style });
        this.card.mount("#card-element");

        this.card.on("change", (event: any) => {
          let displayError = document.getElementById("card-error");
          if (event.error) displayError!.textContent = event.error.message;
          else displayError!.textContent = "";
        });

        let form = document.getElementById("payment-form");
        form!.addEventListener("submit", (event) => {
          event.preventDefault();
          this.payWithCard();
        });
    }, 100);
  }

  payWithCard() {
    if (this.processing) return;
    this.processing = true; 
    
    const btn = document.getElementById("submit") as HTMLButtonElement;
    btn.disabled = true;
    btn.innerText = "Procesando...";

    this.stripe.confirmCardPayment(this.clientSecret, {
      payment_method: { card: this.card }
    }).then((result: any) => {
      if (result.error) {
        this.processing = false;
        btn.disabled = false;
        btn.innerText = `Pagar ${this.price} €`;
        alert(result.error.message);
      } else {
        if (result.paymentIntent.status === 'succeeded') {
          // 👇 AQUÍ ESTÁ LA SOLUCIÓN AL 404
          // Usamos this.email. Si está vacío (caso manual), usamos 'dummy' (fallará si no existe).
          const userEmail = this.email || 'dummy';
          
          this.http.get(`http://localhost:8080/users/confirm/Token/${userEmail}?token=${this.token}`)
            .subscribe({
              next: () => {
                alert("¡Pago completado! Cuenta activada.");
                this.router.navigate(['/login']);
              },
              error: (err) => {
                console.error(err);
                // Si el pago pasó pero falló la confirmación, avisamos pero redirigimos
                alert("Pago recibido. Redirigiendo..."); 
                this.router.navigate(['/login']);
              }
            });
        }
      }
    });
  }
}