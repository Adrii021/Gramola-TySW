import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { PricingService } from '../services/pricing.service'; // 👇 Importar

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './home.html',
  styleUrls: ['./home.css']
})
export class HomeComponent implements OnInit {
  barName: string = "Usuario";
  query: string = "";
  tracks: any[] = [];
  playlist: any[] = [];
  currentPlayingId: string | null = null;
  showingPlaylist: boolean = false;
  showingSettings: boolean = false; 
  editData = { name: '', password: '' };
  
  // 👇 VARIABLES NUEVAS PARA EL MODAL DE PAGO
  user: any;
  showPaymentModal: boolean = false;
  pendingTrack: any = null;
  songPrice: number = 0.99; // Precio por defecto

  constructor(
    private router: Router, 
    private http: HttpClient, 
    private sanitizer: DomSanitizer,
    private pricingService: PricingService // 👇 Inyectar
  ) {}

  ngOnInit() {
    const userJson = localStorage.getItem('currentUser');
    if (userJson) {
      this.user = JSON.parse(userJson);
      this.barName = this.user.name || this.user.bar || "Usuario"; 
      this.refreshPlaylistData(); 
      
      // 👇 Cargar precio de canción
      this.pricingService.getPrices().subscribe(prices => {
        const p = prices.find(x => x.type === 'SONG');
        if(p) this.songPrice = p.price;
      });

    } else {
      this.router.navigate(['/login']);
    }
  }

  logout() {
    localStorage.removeItem('currentUser');
    this.router.navigate(['/login']);
  }

  search() {
    // Igual que antes...
    let info = { query: this.query, userId: this.user.email };
    this.showingPlaylist = false;
    this.http.post<any[]>("http://localhost:8080/music/search", info).subscribe({
      next: (res) => this.tracks = res,
      error: (e) => alert("Error buscando: " + e.message)
    });
  }

  // 👇 LÓGICA DE AÑADIR MODIFICADA (Abre el Modal)
  preAdd(track: any) {
    // Si no ha pagado suscripción del bar, no dejamos hacer nada (bloqueo original)
    if (this.user.creationToken && !this.user.creationToken.used) {
      alert("⚠️ Debes completar la suscripción del bar primero.");
      if(this.user.creationToken.id) window.location.href = '/payment?token=' + this.user.creationToken.id;
      return;
    }
    
    // Abrimos el modal para elegir Priority o Normal
    this.pendingTrack = track;
    this.showPaymentModal = true;
  }

  addNormal() {
    this.processAdd(this.pendingTrack, false);
    this.closeModal();
  }

  addPriority() {
    if(confirm(`Vas a pagar ${this.songPrice}€ para poner la canción YA. ¿Confirmar?`)) {
      this.processAdd(this.pendingTrack, true); // true = priority
      this.closeModal();
    }
  }

  closeModal() {
    this.showPaymentModal = false;
    this.pendingTrack = null;
  }

  processAdd(track: any, isPriority: boolean) {
    let info = {
      track: track,
      userId: this.user.email,
      priority: isPriority // 👇 Enviamos prioridad al backend
    };

    this.http.post("http://localhost:8080/music/add", info).subscribe({
      next: () => {
        alert(isPriority ? "¡Canción prioritaria añadida! 🚀" : "Canción añadida a la cola");
        this.refreshPlaylistData(); 
      },
      error: (err) => alert("Error al añadir: " + err.message)
    });
  }

  // ... (Resto de métodos: play, getEmbedUrl, remove, settings... IGUAL QUE ANTES) ...
  // Solo asegúrate de copiar el resto tal cual lo tenías:
  
  refreshPlaylistData() {
    this.http.post<any[]>("http://localhost:8080/music/playlist", { userId: this.user.email }).subscribe({
      next: (lista) => this.playlist = lista,
      error: (err) => console.error(err)
    });
  }
  
  play(trackId: string) { this.currentPlayingId = trackId; }
  
  getEmbedUrl(trackId: string): SafeResourceUrl {
    return this.sanitizer.bypassSecurityTrustResourceUrl(`https://open.spotify.com/embed/track/${trackId}?utm_source=generator`);
  }

  remove(trackId: string) {
    if(!confirm("¿Borrar canción?")) return;
    let info = { userId: this.user.email, trackId: trackId };
    this.http.post("http://localhost:8080/music/remove", info).subscribe({
      next: () => {
        if (this.currentPlayingId === trackId) this.currentPlayingId = null;
        this.refreshPlaylistData();
      },
      error: (err) => alert("Error: " + err.message)
    });
  }

  openPlaylist() { this.showingPlaylist = true; this.refreshPlaylistData(); }
  openSettings() { this.showingSettings = true; this.showingPlaylist = false; this.tracks = []; this.editData.name = this.barName; this.editData.password = ""; }
  
  saveSettings() {
    let info = { userId: this.user.email, name: this.editData.name, password: this.editData.password };
    this.http.post("http://localhost:8080/users/update", info).subscribe({
      next: (updatedUser: any) => {
        alert("¡Datos actualizados!");
        localStorage.setItem('currentUser', JSON.stringify(updatedUser));
        this.barName = updatedUser.name || updatedUser.bar;
        this.showingSettings = false;
      },
      error: (err) => alert("Error: " + err.message)
    });
  }
}