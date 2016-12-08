import { Component, OnInit } from '@angular/core';
import { ConnectionService, Connection, ActiveConnection } from './service/connection.service';

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
    private connectionService: ConnectionService) {
  }

  ngOnInit() {
    this.menuitems = [
      { label: 'Aktuelle Daten', routerLink: '/monitor/current' },
      { label: 'Historie', routerLink: '/monitor/history' }
    ];

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
