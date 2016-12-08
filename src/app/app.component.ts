import { Component, OnInit } from '@angular/core';
import { ConnectionService, Connection, ActiveConnection } from './service/connection.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  private navCollapsed: boolean = true;
  private menuitems: any[];
  private connections: string;

  constructor(
    private connectionService: ConnectionService,
    private router: Router
  ) {}

  ngOnInit() {
    this.menuitems = [
      { label: 'Übersicht', routerLink: '/monitor' },
      { label: 'Aktuelle Daten', routerLink: '/monitor/current' },
      { label: 'Historie', routerLink: '/monitor/history' }
    ];

    var connection: Connection = this.connectionService.getDefault();
    if(!(connection instanceof ActiveConnection)) {
      this.router.navigate(['login']);
    } 

    // check connection after a while
    setTimeout(() => {
      if(connection instanceof ActiveConnection && connection.websocket.readyState !== WebSocket.OPEN) {
        //this.error = "Verbindung unmöglich";
        setTimeout(() => {
          if(!(connection instanceof ActiveConnection)) {
            this.router.navigate(['login']);
          }
        }, 1000);
      }
    }, 2000);

    this.connectionService.connectionsChanged.subscribe(() => {
      this.connections = "";
      for (var url in this.connectionService.connections) {
        var connection: Connection = this.connectionService.connections[url];
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
