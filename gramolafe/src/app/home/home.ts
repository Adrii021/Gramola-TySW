import { Component, OnInit, OnDestroy } from '@angular/core';
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
  currentPlayback: any = null;
  devices: any[] = [];
  private eventSource: EventSource | null = null;
  
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
      this.startSse();
      
      this.pricingService.getPrices().subscribe(prices => {
        const p = prices.find(x => x.type === 'SONG');
        if(p) this.songPrice = p.price;
      });

    } else {
      this.router.navigate(['/login']);
    }
  }

  ngOnDestroy(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
  }
  startSse() {
    try {
      if (this.eventSource) this.eventSource.close();
      this.eventSource = new EventSource('http://localhost:8080/music/events');

      this.eventSource.addEventListener('state', (e: any) => {
        try {
          const payload = JSON.parse(e.data);
          // current playback
          this.currentPlayback = payload.current || null;
          // devices: try common shapes
          this.devices = (payload.devices && payload.devices.devices) ? payload.devices.devices : (payload.devices || []);
          // queue: backend sends full queue; filter to current user for "Mi playlist"
          const queue = payload.queue || [];
          this.playlist = queue.filter((t: any) => t.userId === this.user.email);
        } catch (err) {
          console.error('Error parsing SSE state', err);
        }
      });

      this.eventSource.onerror = (err) => {
        console.error('SSE error', err);
        // try to reconnect after a short delay
        if (this.eventSource) {
          this.eventSource.close();
          this.eventSource = null;
        }
        setTimeout(() => this.startSse(), 3000);
      };
    } catch (err) {
      console.error('Failed to start SSE', err);
    }
  }

  isOwner(): boolean {
    return !!(this.user && this.user.clientId);
  }

  play() {
    this.http.post("http://localhost:8080/music/play", {}).subscribe({ next: () => {}, error: e => alert('Error play: ' + e.message) });
  }

  pause() {
    this.http.post("http://localhost:8080/music/pause", {}).subscribe({ next: () => {}, error: e => alert('Error pause: ' + e.message) });
  }

  // Toggle single button: si está reproduciendo -> pausar, si no -> reproducir
  togglePlay() {
    const playing = !!(this.currentPlayback && this.currentPlayback.is_playing);
    if (playing) {
      this.pause();
    } else {
      this.play();
    }
  }

  skip() {
    // Mantengo método por compatibilidad pero UI no lo mostrará
    if(!confirm('¿Saltar a la siguiente pista de la cola?')) return;
    this.http.post("http://localhost:8080/music/skip", {}).subscribe({ next: () => {}, error: e => alert('Error skip: ' + e.message) });
  }

  transferTo(deviceId: string, play: boolean) {
    const body = { deviceId: deviceId, play: play };
    this.http.post("http://localhost:8080/music/transfer", body).subscribe({ next: () => {}, error: e => alert('Error transfer: ' + e.message) });
  }

  formatMs(ms: number | undefined | null): string {
    if (ms === null || ms === undefined) return '--:--';
    const totalSec = Math.floor(ms / 1000);
    const m = Math.floor(totalSec / 60);
    const s = totalSec % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  }

  getPlaybackPercent(): number {
    if (!this.currentPlayback || !this.currentPlayback.item) return 0;

    // Compatibilidad con distintas serializaciones: intentamos varios nombres
    const prog = (this.currentPlayback.progress_ms ?? this.currentPlayback.progressMs ?? this.currentPlayback.progress ?? 0) as number;
    const item = this.currentPlayback.item;
    const dur = (item.duration_ms ?? item.durationMs ?? item.duration ?? 0) as number;

    if (!dur || dur <= 0) return 0;
    const pct = (prog / dur) * 100;
    return Math.max(0, Math.min(100, pct));
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
    // Si el usuario es el propietario (tiene clientId), puede añadir gratis directamente
    if (this.user && this.user.clientId) {
      this.processAdd(track, false);
      return;
    }

    // Para clientes normales mostramos el modal de pago/opciones
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