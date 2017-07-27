import { Component, OnInit, OnDestroy } from '@angular/core';
import { MdSnackBar } from '@angular/material';
import { Subject } from 'rxjs/Subject';
import { TranslateService } from '@ngx-translate/core';

import * as moment from 'moment';

import { environment } from '../environments';
import { WebappService, WebsocketService, Device, Notification } from './shared/shared';

@Component({
  selector: 'root',
  templateUrl: './app.component.html'
})
export class AppComponent implements OnInit, OnDestroy {
  public environment = environment;
  public currentDevice: Device;

  private navCollapsed: boolean = true;
  private ngUnsubscribe: Subject<void> = new Subject<void>();

  constructor(
    public websocketService: WebsocketService,
    private webappService: WebappService,
    private snackBar: MdSnackBar
  ) {
    webappService.setLang('de');
  }

  ngOnInit() {
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

