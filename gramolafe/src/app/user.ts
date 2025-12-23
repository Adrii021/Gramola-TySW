import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class User {
  private apiUrl = ' http://localhost:8080/users/register';

  constructor(private http: HttpClient) {}

  register(email : string, pwd1 : string, pwd2 : string) {
    let info = {
      email : email,
      pwd1 : pwd1, 
      pwd2 : pwd2
    }
    return this.http.post<any>(this.apiUrl, info);
  }
}

