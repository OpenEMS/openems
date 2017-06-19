import { Component, OnInit, OnDestroy } from '@angular/core';
import { MdSnackBar } from '@angular/material';
import { Router } from '@angular/router';
import { Subject } from 'rxjs/Subject';

import * as moment from 'moment';

import { environment } from '../environments';
import { WebappService, WebsocketService, Device, Notification } from './shared/shared';

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
    private webappService: WebappService,
    private router: Router,
    private snackBar: MdSnackBar
  ) { }

  ngOnInit() {
    moment.locale("de");

    this.websocketService.currentDevice.takeUntil(this.ngUnsubscribe).subscribe(currentDevice => {
      setTimeout(() => {
        this.currentDevice = currentDevice;
      })
    });

    this.webappService.notificationEvent.takeUntil(this.ngUnsubscribe).subscribe(notification => {
      this.snackBar.open(notification.message, null, { duration: 3000 });
    });
  }

  ngOnDestroy() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }
}

