import { Component, OnInit, OnDestroy } from '@angular/core';
import { ConnectionService, Connection } from './../../service/connection.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-monitor-overview',
  templateUrl: './overview.component.html'
})
export class MonitorOverviewComponent implements OnInit, OnDestroy {

  constructor(
    private connectionService: ConnectionService,
    private router: Router
  ) { }

  ngOnInit() {
    this.connectionService.connectionsChanged.subscribe((/* value */) => {
      // subscribe to each connection for overview
      for (let conn in this.connectionService.connections) {
        var connection = this.connectionService.connections[conn];
        connection.event.subscribe(() => {
          if (connection.isConnected) {
            connection.subscribeData();
          }
        });
      }
    })
  }

  ngOnDestroy() {
    console.log("Overview Out");
    // unsubscribe to each connection for overview
    for (let conn in this.connectionService.connections) {
      var connection = this.connectionService.connections[conn];
      if (connection.isConnected) {
        connection.unsubscribeData();
      }
    }
  }

  private contains(array: string[], tag: string): boolean {
    return array.indexOf(tag) != -1
  }
}
