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
  public environment = environment;

  private navCollapsed: boolean = true;
  private menuitems: any[];
  private connections: string;

  constructor(
    public websocketService: WebsocketService,
    private router: Router,
    private webappService: WebappService
  ) {
    moment.locale("de");
  }
}
