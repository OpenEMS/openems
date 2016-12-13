import { Injectable } from '@angular/core';

@Injectable()
export class LocalstorageService {
  
  public getToken(id: string): string {
    return localStorage.getItem(id + "_token");
  }

  public setToken(id: string, token: string) {
    localStorage.setItem(id + "_token", token);
  }

  public removeToken(id: string) {
    localStorage.removeItem(id + "_token");
  }
}
