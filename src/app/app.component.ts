import { Component, OnInit } from '@angular/core';
import { WebSocketService } from './service/websocket.service';
import { Connection, ActiveConnection } from './service/connection';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  private menuitems: any[];
  public connections: string;

  constructor(
    private websocket: WebSocketService) {
  }

  ngOnInit() {
    this.menuitems = [
      { label: 'Aktuelle Daten', routerLink: '/monitor/current' },
      { label: 'Historie', routerLink: '/monitor/history' }
    ];

    this.websocket.connectionsChanged.subscribe(() => {
      this.connections = "";
      for (var url in this.websocket.connections) {
        var connection: Connection = this.websocket.connections[url];
        if(connection instanceof ActiveConnection) {
          if (connection.username != null) { 
            this.connections += (this.connections != "" ? ", " : "") + connection.username + "@" + connection.name;
          }
        }
      }
    }, error => {
      this.connections = ""
    }, () => {
      this.connections = ""
    });
  }
}
