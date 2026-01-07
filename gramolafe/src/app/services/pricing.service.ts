import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class PricingService {
  private url = 'http://localhost:8080/pricing';

  constructor(private http: HttpClient) { }

  getPrices(): Observable<any[]> {
    return this.http.get<any[]>(`${this.url}/all`);
  }
}