import { Injectable, ViewContainerRef } from '@angular/core';
import { ToastsManager } from 'ng2-toastr/ng2-toastr';
import { MdSnackBar } from '@angular/material'

import { WebsocketService } from './websocket.service';
import { Device } from './device';

type NotificationType = "success" | "error" | "warning" | "info";

export interface Notification {
  type: NotificationType;
  message: string;
}

@Injectable()
export class WebappService {

  constructor(
    private snackBar: MdSnackBar,
    private toastr: ToastsManager
  ) { }

  /**
   * Needs to be called once from AppComponent
   */
  public initializeToastr(vRef: ViewContainerRef) {
    this.toastr.setRootViewContainerRef(vRef);
  }

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
    if (notification.type == "success") {
      this.toastr.success(notification.message);
    } else if (notification.type == "error") {
      this.toastr.error(notification.message);
    } else if (notification.type == "warning") {
      this.toastr.warning(notification.message);
    } else {
      this.toastr.info(notification.message);
      // Material: this.snackBar.open(notification.message, null, { duration: 2000 });
    }
  }
}