import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { PricingService } from '../services/pricing.service';

declare var Stripe: any;

@Component({
  selector: 'app-client',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './client.component.html',
  styleUrls: ['./client.component.css']
})
export class ClientComponent implements OnInit {
  barId: string = "";
  barName: string = "Cargando...";
  query: string = "";
  tracks: any[] = [];
  
  // Variables Modal y Pago
  showPaymentModal: boolean = false;
  showStripeForm: boolean = false; // 👇 Nuevo: para alternar vista en el modal
  pendingTrack: any = null;
  songPrice: number = 0.99; 

  // Variables Stripe
  stripe: any;
  card: any;
  clientSecret: string = "";
  processing: boolean = false;
  // SSE and playback
  currentPlayback: any = null;
  queue: any[] = [];
  private eventSource: EventSource | null = null;

  constructor(
    private route: ActivatedRoute,
    private http: HttpClient,
    private router: Router,
    private pricingService: PricingService
  ) {
    // 👇 Inicializamos Stripe con tu clave pública
    this.stripe = Stripe("pk_test_51SixjzR3ux91c7imTZjFULUGxl7oh0SaevRtgx3WqDi34rWh2DGLlC5b9i7BURyReACNsz1sCzB7DR15k2DPTjEO00CEUH7qcB");
  }

  ngOnInit() {
    this.barId = this.route.snapshot.paramMap.get('id') || '';
    if (!this.barId) {
      this.exit();
      return;
    }

    this.http.get<any>(`http://localhost:8080/users/bar-name/${this.barId}`).subscribe({
      next: (res) => this.barName = res.name,
      error: () => this.barName = this.barId
    });

    this.pricingService.getPrices().subscribe(prices => {
      const p = prices.find(x => x.type === 'SONG');
      if (p) this.songPrice = p.price;
    });

    // Start SSE to receive now-playing and queue updates
    this.startSse();
  }

  exit() {
    this.router.navigate(['/login']);
  }

  search() {
    if (!this.query.trim()) return;
    let info = { query: this.query, userId: this.barId };
    
    this.http.post<any[]>("http://localhost:8080/music/search", info).subscribe({
      next: (res) => this.tracks = res,
      error: (e) => alert("Error buscando: " + e.message)
    });
  }

  // --- Lógica del Modal ---

  preAdd(track: any) {
    this.pendingTrack = track;
    this.showPaymentModal = true;
    this.showStripeForm = false; // Resetear vista
  }

  startSse() {
    try {
      if (this.eventSource) this.eventSource.close();
      this.eventSource = new EventSource('http://localhost:8080/music/events');

      this.eventSource.addEventListener('state', (e: any) => {
        try {
          const payload = JSON.parse(e.data);
          this.currentPlayback = payload.current || null;
          const fullQueue = payload.queue || [];
          // filter queue for this bar (barId corresponds to userId stored in SelectedTrack)
          this.queue = fullQueue.filter((t: any) => t.userId === this.barId);
        } catch (err) {
          console.error('Error parsing SSE state', err);
        }
      });

      this.eventSource.onerror = (err) => {
        console.error('SSE error', err);
        if (this.eventSource) { this.eventSource.close(); this.eventSource = null; }
        setTimeout(() => this.startSse(), 3000);
      };
    } catch (err) {
      console.error('Failed to start SSE', err);
    }
  }

  closeModal() {
    this.showPaymentModal = false;
    this.pendingTrack = null;
    this.showStripeForm = false;
    // Importante: Destruir elemento de tarjeta si existe para evitar errores al reabrir
    if(this.card) {
        this.card.destroy();
        this.card = null;
    }
  }

  addNormal() {
    this.processAdd(this.pendingTrack, false);
    this.closeModal();
  }

  // 👇 Paso 1: Usuario elige prioridad -> Preparamos Stripe
  initiatePriorityPayment() {
    this.showStripeForm = true; // Cambiamos la vista del modal
    
    // Pedimos el intento de pago al backend
    let info = { token: "dummy_client", amount: this.songPrice };
    
    this.http.post<any>("http://localhost:8080/payments/prepay", info).subscribe({
      next: (res) => {
        this.clientSecret = res.clientSecret;
        this.mountStripeElement();
      },
      error: (err) => {
        alert("Error iniciando pago: " + err.message);
        this.showStripeForm = false;
      }
    });
  }

  // 👇 Paso 2: Montar el formulario de tarjeta
  mountStripeElement() {
    setTimeout(() => {
        if(!this.stripe) return;
        let elements = this.stripe.elements();
        
        // Estilos para que cuadre con el modo oscuro
        let style = { 
            base: { 
                color: "#ffffff", 
                fontFamily: 'Arial, sans-serif', 
                fontSize: "16px",
                "::placeholder": { color: "#9ca3af" }
            },
            invalid: { color: "#fa755a" }
        };
        
        this.card = elements.create("card", { style: style });
        this.card.mount("#card-element-song"); // Montar en el div específico
    }, 100);
  }

  // 👇 Paso 3: Confirmar pago
  confirmPayment() {
    if (this.processing) return;
    this.processing = true; 
    
    this.stripe.confirmCardPayment(this.clientSecret, {
      payment_method: { card: this.card }
    }).then((result: any) => {
      this.processing = false;
      
      if (result.error) {
        alert(result.error.message);
      } else {
        if (result.paymentIntent.status === 'succeeded') {
          // ¡Pago OK! -> Añadimos la canción con prioridad
          this.processAdd(this.pendingTrack, true);
          this.closeModal();
        }
      }
    });
  }

  // Método final que llama al backend de música
  processAdd(track: any, isPriority: boolean) {
    let info = {
      track: track,
      userId: this.barId,
      priority: isPriority
    };

    this.http.post("http://localhost:8080/music/add", info).subscribe({
      next: () => {
        alert(isPriority ? "¡Pago recibido! Canción colada 🚀" : "Canción añadida a la cola");
        this.tracks = []; 
        this.query = "";
      },
      error: (err) => alert("Error: " + err.message)
    });
  }
}