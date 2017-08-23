import { Injectable, ErrorHandler } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Subject } from 'rxjs/Subject';

import * as moment from 'moment';

import { WebsocketService } from './websocket.service';
import { Device } from '../shared';

type NotificationType = "success" | "error" | "warning" | "info";

export interface Notification {
  type: NotificationType;
  message: string;
}

@Injectable()
export class WebappService implements ErrorHandler {

  public notificationEvent: Subject<Notification> = new Subject<Notification>();

  constructor(
    public translate: TranslateService
  ) {
    // add language
    translate.addLangs(["de", "en", "cz", "nl"]);
    // this language will be used as a fallback when a translation isn't found in the current language
    translate.setDefaultLang('de');
  }

  /**
   * Sets the application language
   */
  public setLang(id: 'de' | 'en' | 'cz' | 'nl') {
    this.translate.use(id);
    moment.locale(id);
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
    this.notificationEvent.next(notification);
  }

  /**
   * Handles an application error
   */
  public handleError(error: any) {
    console.error(error);
    let notification: Notification = {
      type: "error",
      message: error
    };
    this.notify(notification);
  }
}