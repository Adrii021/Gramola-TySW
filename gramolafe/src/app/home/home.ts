import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { PricingService } from '../services/pricing.service';

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
  
  // Variables de estado
  showingPlaylist: boolean = false;
  showingSettings: boolean = false; 
  editData = { name: '', password: '' };
  
  // Variables Pago
  user: any;
  showPaymentModal: boolean = false;
  pendingTrack: any = null;
  songPrice: number = 0.99;

  constructor(
    private router: Router, 
    private http: HttpClient, 
    private pricingService: PricingService
  ) {}

  ngOnInit() {
    const userJson = localStorage.getItem('currentUser');
    if (userJson) {
      this.user = JSON.parse(userJson);
      this.barName = this.user.name || this.user.bar || "Usuario"; 
      this.refreshPlaylistData(); 
      
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
    if (!this.query.trim()) return;
    let info = { query: this.query, userId: this.user.email };
    this.showingPlaylist = false;
    
    this.http.post<any[]>("http://localhost:8080/music/search", info).subscribe({
      next: (res) => this.tracks = res,
      error: (e) => alert("Error buscando: " + e.message)
    });
  }

  preAdd(track: any) {
    if (this.user.creationToken && !this.user.creationToken.used) {
      alert("⚠️ Debes completar la suscripción del bar primero.");
      if(this.user.creationToken.id) window.location.href = '/payment?token=' + this.user.creationToken.id;
      return;
    }
    this.pendingTrack = track;
    this.showPaymentModal = true;
  }

  addNormal() {
    this.processAdd(this.pendingTrack, false);
    this.closeModal();
  }

  addPriority() {
    if(confirm(`Vas a pagar ${this.songPrice}€ para poner la canción YA. ¿Confirmar?`)) {
      this.processAdd(this.pendingTrack, true);
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
      priority: isPriority
    };

    this.http.post("http://localhost:8080/music/add", info).subscribe({
      next: () => {
        alert(isPriority ? "¡Canción prioritaria enviada a la cola! 🚀" : "Canción enviada a la cola");
        this.refreshPlaylistData(); 
      },
      error: (err) => alert("Error al añadir: " + err.message)
    });
  }

  refreshPlaylistData() {
    this.http.post<any[]>("http://localhost:8080/music/playlist", { userId: this.user.email }).subscribe({
      next: (lista) => this.playlist = lista,
      error: (err) => console.error(err)
    });
  }

  remove(trackId: string) {
    if(!confirm("¿Borrar canción de la lista visual? (Seguirá sonando en Spotify si ya se cargó)")) return;
    
    let info = { userId: this.user.email, trackId: trackId };
    this.http.post("http://localhost:8080/music/remove", info).subscribe({
      next: () => this.refreshPlaylistData(),
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