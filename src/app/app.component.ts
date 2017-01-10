import { Component, OnInit, ViewContainerRef } from '@angular/core';
import { Router } from '@angular/router';
import { ToastsManager } from 'ng2-toastr/ng2-toastr';
import { WebappService } from './service/webapp.service';
import { WebsocketService } from './service/websocket.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  private navCollapsed: boolean = true;
  private menuitems: any[];
  private connections: string;

  private options = {
    position: ["bottom", "left"],
    timeOut: 5000,
    lastOnBottom: true
  }

  constructor(
    private router: Router,
    private webappService: WebappService,
    private websocketService: WebsocketService,
    private vRef: ViewContainerRef
  ) { }

  ngOnInit() {
    this.webappService.initializeToastr(this.vRef);
    this.menuitems = [
      { label: 'Übersicht', routerLink: '/monitor' },
      { label: 'Konfiguration', routerLink: '/config' }/*,
      { label: 'Aktuelle Daten', routerLink: '/monitor/current' },
      { label: 'Historie', routerLink: '/monitor/history' }*/
    ];

    /*
    var ws = new WebSocket("ws://localhost:8087");
    ws.onopen = () => {
      console.log("WS opened");
      ws.send(JSON.stringify({ "Hallo": "Welt" }));
    }
    ws.onclose = () => {
      console.log("WS closed");
    }
    ws.onmessage = (msg) => {
      let data = JSON.parse(msg.data);
      console.log("Message", data);
    }
    */
  }
}
