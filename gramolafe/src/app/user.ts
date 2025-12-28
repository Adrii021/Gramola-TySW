import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class User {
  private apiUrl = 'http://localhost:8080/users/register'; // He quitado el espacio extra al inicio de la URL

  constructor(private http: HttpClient) {}

  register(email: string, pwd1: string, pwd2: string, bar: string, clientId: string, clientSecret: string) {
    let info = {
      email: email,
      pwd1: pwd1,
      pwd2: pwd2,
      bar: bar,
      clientId: clientId,
      clientSecret: clientSecret
    }
    return this.http.post<any>(this.apiUrl, info);
  }

  login(email: string, pwd: string) {
      let info = { email: email, pwd: pwd };
      return this.http.post<any>('http://localhost:8080/users/login', info);
  }
}