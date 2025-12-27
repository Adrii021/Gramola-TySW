import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';

declare let Stripe: any;

@Component({
  selector: 'app-payment',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './payment.component.html',
  styleUrls: ['./payment.component.css']
})
export class PaymentComponent implements OnInit {

  stripe = Stripe("pk_test_51SixjzR3ux91c7imTZjFULUGxl7oh0SaevRtgx3WqDi34rWh2DGLlC5b9i7BURyReACNsz1sCzB7DR15k2DPTjEO00CEUH7qcB");
  transactionDetails: any;
  token?: string;

  constructor(private http: HttpClient, private router: Router, private route: ActivatedRoute) { }

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
        this.token = params['token'];
    });
  }

  prepay() {
    this.http.get<any>('http://localhost:8080/payments/prepay').subscribe({
      next: (response: any) => {
        this.transactionDetails = JSON.parse(response.data);
        this.showForm();
      },
      error: (err) => alert("Error: " + err.message)
    });
  }

  showForm() {
    let elements = this.stripe.elements();
    let style = {
      base: { color: "#32325d", fontFamily: 'Arial, sans-serif', fontSize: "16px" }
    };

    let card = elements.create("card", { style: style });
    card.mount("#card-element");

    let self = this;
    document.getElementById("payment-form")!.style.display = "block";
    document.getElementById("prepay-btn")!.style.display = "none";

    document.getElementById("payment-form")!.addEventListener("submit", function (event) {
      event.preventDefault();
      self.payWithCard(card);
    });
  }

  payWithCard(card: any) {
    let self = this;
    this.stripe.confirmCardPayment(this.transactionDetails.client_secret, {
      payment_method: { card: card }
    }).then(function (result: any) {
      if (result.error) {
        alert(result.error.message);
      } else {
        if (result.paymentIntent.status === 'succeeded') {
          alert("¡Pago realizado! Cuenta activada.");
          self.router.navigate(["/login"]);
        }
      }
    });
  }
}