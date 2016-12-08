import { Injectable } from '@angular/core';

@Injectable()
export class LocalstorageService {
  
  public getToken(): string {
    return localStorage.getItem("token");
  }

  public setToken(token: string) {
    localStorage.setItem("token", token);
  }

  public removeToken() {
    localStorage.removeItem("token");
  }
}
