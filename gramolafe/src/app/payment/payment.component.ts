import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

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
  stripe: any;
  card: any; // <--- Importante: Guardamos la tarjeta aquí para no perderla
  clientSecret: string = "";
  processing: boolean = false; // <--- Para bloquear el botón

  constructor(private route: ActivatedRoute, private http: HttpClient, private router: Router) {
    // TU CLAVE PÚBLICA (La que empieza por pk_test_)
    this.stripe = Stripe("pk_test_51SixjzR3ux91c7imTZjFULUGxl7oh0SaevRtgx3WqDi34rWh2DGLlC5b9i7BURyReACNsz1sCzB7DR15k2DPTjEO00CEUH7qcB");
  }

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.token = params['token'];
    });
  }

  prepay() {
    this.http.post<any>("http://localhost:8080/payments/prepay", { token: this.token }).subscribe({
      next: (res) => {
        this.clientSecret = res.clientSecret;
        
        // Ocultar botón prepay, mostrar formulario
        document.getElementById("prepay-btn")!.style.display = "none";
        document.getElementById("payment-form")!.style.display = "block";

        // Montar la tarjeta de Stripe
        let elements = this.stripe.elements();
        this.card = elements.create("card"); // Guardamos en this.card
        this.card.mount("#card-element");

        // Escuchar el evento submit del formulario
        let form = document.getElementById("payment-form");
        form!.addEventListener("submit", (event) => {
          event.preventDefault();
          this.payWithCard();
        });
      },
      error: (err) => {
        alert("Error al iniciar el pago: " + err.message);
      }
    });
  }

  payWithCard() {
    // 1. Evitar doble clic
    if (this.processing) return;
    this.processing = true; 
    
    // Cambiar texto del botón para que se vea que hace algo
    const btn = document.getElementById("submit") as HTMLButtonElement;
    btn.disabled = true;
    btn.innerText = "Procesando pago...";

    // 2. Confirmar pago con Stripe
    this.stripe.confirmCardPayment(this.clientSecret, {
      payment_method: {
        card: this.card // Usamos la variable de clase
      }
    }).then((result: any) => {
      if (result.error) {
        // Error de Stripe (ej: tarjeta rechazada)
        this.processing = false;
        btn.disabled = false;
        btn.innerText = "Pagar 10,00 €";
        
        const errorElement = document.getElementById('card-error');
        errorElement!.textContent = result.error.message;
        alert(result.error.message);
      } else {
        // ¡ÉXITO!
        if (result.paymentIntent.status === 'succeeded') {
          alert("¡Pago realizado con éxito! Redirigiendo al Login...");
          this.router.navigate(['/login']);
        }
      }
    });
  }
}