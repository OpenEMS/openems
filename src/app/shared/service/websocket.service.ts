import { Injectable, EventEmitter } from '@angular/core';
import { Subject } from 'rxjs/Subject';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { Router, ActivatedRoute, Params } from '@angular/router';

import { environment } from '../../../environments';
import { Websocket } from '../websocket';
import { WebappService, Notification } from './webapp.service';
import { Device } from '../device';

export { Websocket };

@Injectable()
export class WebsocketService {
  public websockets: { [name: string]: Websocket } = {};
  public event = new Subject<Notification>();
  public currentDevice = new BehaviorSubject<Device>(null);

  constructor(
    private router: Router,
    private webappService: WebappService,
  ) {
    // initialize websockets
    for (var defaultWebsocket of environment.websockets) {
      // load default websockets
      let websocket = new Websocket(defaultWebsocket.name, defaultWebsocket.url, defaultWebsocket.backend, webappService);
      this.websockets[websocket.name] = websocket;
      // try to connect using token or session_id
      websocket.connectWithTokenOrSessionId();

      websocket.event.subscribe(notification => {
        let n = {
          type: notification.type,
          message: websocket.name + ": " + notification.message
        }
        this.event.next(n);
        if (!websocket.isConnected) {
          let device = this.currentDevice.getValue();
          if (device) {
            if (device.websocket === websocket) {
              this.router.navigate(['/overview']);
            }
          }
        }
      });
    }
  }

  /**
   * Parses the route params, sets the current device and returns it - or redirects to overview and returns null
   */
  public setCurrentDevice(params: Params): BehaviorSubject<Device> {
    let timeout = null;
    let retryCounter = 0;
    let worker = (params: Params): boolean => {
      retryCounter++;
      if ('websocket' in params && 'device' in params) {
        let websocketName = params['websocket'];
        let deviceName = params['device'];
        let websocket = this.getWebsocket(websocketName);
        if (websocket) {
          let device = websocket.getDevice(deviceName);
          if (device) {
            // found it -> we quit here
            this.currentDevice.next(device);
            return;
          }
        }
      }
      if (retryCounter < 10) {
        // retry 10 times
        timeout = setTimeout(() => {
          worker(params);
        }, 1000);
      } else {
        // failed -> redirect to /overview
        this.currentDevice.next(null);
        this.router.navigate(['/overview']);
      }
    }
    worker(params);
    return this.currentDevice;
  }

  public clearCurrentDevice() {
    this.currentDevice.next(null);
  }

  /**
   * Returns the websocket with the given name
   */
  public getWebsocket(name: string) {
    if (this.websockets[name]) {
      return this.websockets[name];
    } else {
      return null;
    }
  }

  /**
   * Closes the given websocket
   */
  public closeConnection(websocket: Websocket) {
    console.info("Closing websocket[" + websocket.name + "; " + websocket.url + "]");
    websocket.close();
    //this.event.emit({ type: "info", message: "Verbindung beendet" });
  }
}