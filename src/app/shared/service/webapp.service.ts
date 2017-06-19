import { Injectable, ViewContainerRef } from '@angular/core';
import { MdSnackBar } from '@angular/material'

import { WebsocketService } from './websocket.service';
import { Device } from '../shared';

type NotificationType = "success" | "error" | "warning" | "info";

export interface Notification {
  type: NotificationType;
  message: string;
}

@Injectable()
export class WebappService {

  constructor(
    private snackBar: MdSnackBar
  ) { }

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
    this.snackBar.open(notification.message, null, { duration: 2000 });
    // if (notification.type == "success") {
    //   this.toastr.success(notification.message);
    // } else if (notification.type == "error") {
    //   this.toastr.error(notification.message);
    // } else if (notification.type == "warning") {
    //   this.toastr.warning(notification.message);
    // } else {
    //   //this.toastr.info(notification.message);
    //   this.snackBar.open(notification.message, null, { duration: 2000 });
    // }

  }
}