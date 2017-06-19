import { Component, ViewContainerRef, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { MdSnackBar } from '@angular/material';
import { Subject } from 'rxjs/Subject';

import { environment } from '../environments';
import { WebappService, WebsocketService, Notification } from './shared/shared';

import * as moment from 'moment';

@Component({
  selector: 'root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  public environment = environment;
  public ngUnsubscribe: Subject<void> = new Subject<void>();

  private navCollapsed: boolean = true;
  private menuitems: any[];
  private connections: string;

  constructor(
    public websocketService: WebsocketService,
    private router: Router,
    private webappService: WebappService,
    private snackBar: MdSnackBar
  ) {
    moment.locale("de");
  }

  ngOnInit() {
    if (this.webappService.notificationEvent) {
      this.webappService.notificationEvent.takeUntil(this.ngUnsubscribe).subscribe(notification => {
        this.snackBar.open(notification.message, null, { duration: 2000 });
      });
    }
  }

  ngOnDestroy() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }

}
