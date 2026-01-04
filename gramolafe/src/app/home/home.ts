import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

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
  showingPlaylist: boolean = false;

  currentPlayingId: string | null = null;

  showingSettings: boolean = false; 
  editData = {
    name: '',
    password: ''
  };

  constructor(
    private router: Router, 
    private http: HttpClient, 
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit() {
    const userJson = localStorage.getItem('currentUser');
    if (userJson) {
      const user = JSON.parse(userJson);
      this.barName = user.name || user.bar || "Usuario"; 
      this.refreshPlaylistData(); 
    } else {
      this.router.navigate(['/login']);
    }
  }

  logout() {
    localStorage.removeItem('currentUser');
    this.router.navigate(['/login']);
  }

  search() {
    const userJson = localStorage.getItem('currentUser');
    if (!userJson) return;
    const user = JSON.parse(userJson);

    let info = {
      query: this.query,
      userId: user.email 
    };

    this.showingPlaylist = false;
    this.http.post<any[]>("http://localhost:8080/music/search", info).subscribe({
      next: (resultado) => {
        this.tracks = resultado;
      },
      error: (err) => alert("Error buscando: " + err.message)
    });
  }

  add(track: any) {
    const userJson = localStorage.getItem('currentUser');
    if (!userJson) return;
    const user = JSON.parse(userJson);

    // 👇 LÓGICA DE PAGO (CANDADO) 👇
    // Comprobamos si el token existe y si NO está usado
    if (user.creationToken && !user.creationToken.used) {
      alert("⚠️ Funcionalidad Premium\n\nDebes pagar para poder añadir canciones.");
      
      // Redirigimos a la pantalla de pago con su token
      if (user.creationToken.id) {
          window.location.href = '/payment?token=' + user.creationToken.id;
      }
      return; // Cortamos la ejecución aquí
    }
    // 👆 FIN LÓGICA DE PAGO 👆

    let info = {
      track: track,
      userId: user.email
    };

    this.http.post("http://localhost:8080/music/add", info).subscribe({
      next: () => {
        alert("¡Canción añadida!");
        this.refreshPlaylistData(); 
      },
      error: (err) => alert("Error al añadir: " + err.message)
    });
  }

  openPlaylist() {
    this.showingPlaylist = true;
    this.showingSettings = false; 
    this.refreshPlaylistData();
  }

  private refreshPlaylistData() {
    const userJson = localStorage.getItem('currentUser');
    if (!userJson) return;
    const user = JSON.parse(userJson);

    this.http.post<any[]>("http://localhost:8080/music/playlist", { userId: user.email }).subscribe({
      next: (lista) => {
        this.playlist = lista;
      },
      error: (err) => console.error("Error cargando playlist", err)
    });
  }

  play(trackId: string) {
    this.currentPlayingId = trackId;
  }

  getEmbedUrl(trackId: string): SafeResourceUrl {
    const url = `https://open.spotify.com/embed/track/${trackId}?utm_source=generator`;
    return this.sanitizer.bypassSecurityTrustResourceUrl(url);
  }

  remove(trackId: string) {
    const userJson = localStorage.getItem('currentUser');
    if (!userJson) return;
    const user = JSON.parse(userJson);

    if(!confirm("¿Seguro que quieres borrar esta canción?")) return;

    let info = {
      userId: user.email,
      trackId: trackId 
    };

    this.http.post("http://localhost:8080/music/remove", info).subscribe({
      next: () => {
        if (this.currentPlayingId === trackId) {
          this.currentPlayingId = null;
        }
        this.refreshPlaylistData();
      },
      error: (err) => alert("Error al borrar: " + err.message)
    });
  }

  openSettings() {
    this.showingSettings = true;
    this.showingPlaylist = false; 
    this.tracks = []; 

    this.editData.name = this.barName;
    this.editData.password = ""; 
  }

  saveSettings() {
    const userJson = localStorage.getItem('currentUser');
    if (!userJson) return;
    const user = JSON.parse(userJson);

    let info = {
      userId: user.email,
      name: this.editData.name,
      password: this.editData.password
    };

    this.http.post("http://localhost:8080/users/update", info).subscribe({
      next: (updatedUser: any) => {
        alert("¡Datos actualizados!");
        
        localStorage.setItem('currentUser', JSON.stringify(updatedUser));
        this.barName = updatedUser.name || updatedUser.bar; 
        
        this.showingSettings = false; 
      },
      error: (err) => alert("Error al actualizar: " + err.message)
    });
  }

  cancelSettings() {
    this.showingSettings = false;
  }
}