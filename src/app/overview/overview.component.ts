import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

import { WebsocketService, Websocket } from './../service/websocket.service';
import { Device } from './../service/device';
import { WebappService } from './../service/webapp.service';

@Component({
  selector: 'app-overview',
  templateUrl: './overview.component.html'
})
export class OverviewComponent implements OnInit {

  private devices: Device[] = [];

  constructor(
    private websocketService: WebsocketService,
    private webappService: WebappService,
    private router: Router
  ) { }

  ngOnInit() {
    this.websocketService.clearCurrentDevice();

    /*
     * Redirect after a short delay
     * - to /login if nothing is connected
     * - to /monitor/... if exactly one device is connected
     */
    setTimeout(() => {
      let numberOfConnectedWebsockets = 0;
      let lastConnectedWebsocket: Websocket = null;
      for (let websocketName in this.websocketService.websockets) {
        if (this.websocketService.websockets[websocketName].isConnected) {
          numberOfConnectedWebsockets++;
          lastConnectedWebsocket = this.websocketService.websockets[websocketName];
        }
      }
      if (numberOfConnectedWebsockets == 0) {
        this.webappService.notify({ type: "info", message: "Bitte melden Sie sich an." });
        this.router.navigate(['/login']);
      } else if (numberOfConnectedWebsockets == 1) {
        let deviceNames = Object.keys(lastConnectedWebsocket.devices);
        let numberOfConnectedDevices = deviceNames.length;
        if (numberOfConnectedDevices == 0) {
          this.webappService.notify({ type: "info", message: "Keine Geräte gefunden." });
          this.router.navigate(['/login']);
        } else if (numberOfConnectedDevices == 1) {
          this.router.navigate(['/device', lastConnectedWebsocket.name, deviceNames[0]]);
        }
      }
    }, 500);
  }
}
