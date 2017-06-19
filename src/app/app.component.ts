import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subject } from 'rxjs/Subject';

import { environment } from '../environments';
import { WebappService, WebsocketService, Device } from './shared/shared';

import * as moment from 'moment';

@Component({
  selector: 'root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit, OnDestroy {
  public environment = environment;
  public currentDevice: Device;

  private navCollapsed: boolean = true;
  private ngUnsubscribe: Subject<void> = new Subject<void>();

  constructor(
    public websocketService: WebsocketService,
    private router: Router,
    private webappService: WebappService
  ) { }

  ngOnInit() {
    moment.locale("de");

    this.websocketService.currentDevice.takeUntil(this.ngUnsubscribe).subscribe(currentDevice => {
      setTimeout(() => {
        this.currentDevice = currentDevice;
      })
    });
  }

  ngOnDestroy() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }
}

