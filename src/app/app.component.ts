import { Component, OnInit } from '@angular/core';
import { WebSocketService, WebsocketContainer } from './service/websocket.service';

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

    this.websocket.containersChanged.subscribe(() => {
      this.connections = "";
      for (var url in this.websocket.containers) {
        var container: WebsocketContainer = this.websocket.containers[url];
        if (container.username != null) { 
          this.connections += (this.connections != "" ? ", " : "") + container.username + "@" + container.name;
        }
      }
    }, error => {
      this.connections = ""
    }, () => {
      this.connections = ""
    });
  }
}
