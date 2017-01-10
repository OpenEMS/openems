import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';

import { WebsocketService } from '../../service/websocket.service';

@Component({
  selector: 'app-monitor-current',
  templateUrl: './current.component.html'
})
export class MonitorCurrentComponent implements OnInit, OnDestroy {

  constructor(
    private route: ActivatedRoute,
    private websocketService: WebsocketService
  ) { }

  ngOnInit() {
    let device = this.websocketService.setCurrentDevice(this.route.snapshot.params);
    console.log("CurrentComponent", device);
    //this.connectionService.connectionsChanged.subscribe((/* value */) => {
    // subscribe to each connection for overview
    /*for (let conn in this.connectionService.connections) {
      var connection = this.connectionService.connections[conn];
      connection.event.subscribe(() => {
        if (connection.isConnected) {
          connection.subscribeData();
        }
      });
    }
  })*/
  }

  ngOnDestroy() {
    // TODO unsubscribe to connection for overview
  }
}
