import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { WebappService } from './service/webapp.service';
import { WebsocketService } from './service/websocket.service';
import { environment } from '../environments';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  private navCollapsed: boolean = true;
  private menuitems: any[];
  private connections: string;
  private environment = environment;

  constructor(
    private router: Router,
    private webappService: WebappService,
    private websocketService: WebsocketService
  ) { }
}
