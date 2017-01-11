import { Injectable, EventEmitter } from '@angular/core';
import { Subject } from 'rxjs/Subject';
import { Router, ActivatedRoute, Params } from '@angular/router';

import { Websocket } from './websocket';
import { WebappService, Notification } from './webapp.service';
import { Device } from './device';

export { Websocket } from './websocket';

//TODO const DEFAULT_PASSWORD: string = "guest";

const DEFAULT_WEBSOCKETS = [{
  name: "Lokales FEMS",
  //url: "ws://" + location.hostname + ":8085"
  url: "ws://localhost:8085"
}];

@Injectable()
export class WebsocketService {
  public websockets: { [name: string]: Websocket } = {};
  public event = new Subject<Notification>();
  public currentDevice: Device = null;

  constructor(
    private router: Router,
    private webappService: WebappService,
  ) {
    // initialize websockets
    for (var defaultWebsocket of DEFAULT_WEBSOCKETS) {
      // load default websockets
      let websocket = new Websocket(defaultWebsocket.name, defaultWebsocket.url, webappService);
      this.websockets[websocket.name] = websocket;
      // try to connect using token
      websocket.connectWithToken();

      websocket.event.subscribe(notification => {
        let n = {
          type: notification.type,
          message: websocket.name + ": " + notification.message
        }
        this.event.next(n);
      });
    }
  }

  /**
   * Parses the route params, sets the current device and returns it - or redirects to login and returns null
   */
  public setCurrentDevice(params: Params): Device {
    if ('websocket' in params && 'device' in params) {
      let websocketName = params['websocket'];
      let deviceName = params['device'];
      let websocket = this.getWebsocket(websocketName);
      if (websocket) {
        let device = websocket.getDevice(deviceName);
        if (device) {
          this.currentDevice = device;
          return device;
        }
      }
    }
    // TODO retry once after 500 ms
    this.currentDevice = null;
    this.router.navigate(['/login']);
  }

  public clearCurrentDevice() {
    this.currentDevice = null;
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
    console.log("Closing websocket[" + websocket.name + "; " + websocket.url + "]");
    websocket.close();
    //this.event.emit({ type: "info", message: "Verbindung beendet" });
  }
}