import { Injectable, ErrorHandler } from '@angular/core';

import { WebsocketService } from './websocket.service';
import { Device } from '../shared';

import { Subject } from 'rxjs/Subject';

type NotificationType = "success" | "error" | "warning" | "info";

export interface Notification {
  type: NotificationType;
  message: string;
}

@Injectable()
export class WebappService implements ErrorHandler {

  public notificationEvent: Subject<Notification> = new Subject<Notification>();

  constructor() { }

  /**
   * Gets the token for this id from localstorage
   */
  public getToken(id: string): string {
    return localStorage.getItem(id + "_token");
  }

  /**
   * Sets the token for this id in localstorage
   */
  public setToken(id: string, token: string) {
    localStorage.setItem(id + "_token", token);
  }

  /**
   * Removes the token for this id from localstorage
   */
  public removeToken(id: string) {
    localStorage.removeItem(id + "_token");
  }

  /**
   * Shows a nofication using toastr
   */
  public notify(notification: Notification) {
    this.notificationEvent.next(notification);
  }

  /**
   * Handles an application error
   */
  public handleError(error: any) {
    let notification: Notification = {
      type: "error",
      message: error
    };
    this.notify(notification);
  }
}