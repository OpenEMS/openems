import { Component } from '@angular/core';
import { Router } from '@angular/router';

import { environment } from '../environments';
import { WebappService, WebsocketService } from './shared/shared';

import * as moment from 'moment';

@Component({
  selector: 'root',
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
  ) {
    moment.locale("de");
  }
}
