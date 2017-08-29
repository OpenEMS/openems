import { Component, OnInit, OnDestroy } from '@angular/core';
import { MdSnackBar } from '@angular/material';
import { Subject } from 'rxjs/Subject';
import { TranslateService } from '@ngx-translate/core';

import * as moment from 'moment';

import { environment } from '../environments';
import { Service, Websocket, Notification } from './shared/shared';

@Component({
  selector: 'root',
  templateUrl: './app.component.html'
})
export class AppComponent implements OnInit, OnDestroy {
  public env = environment;

  private navCollapsed: boolean = true;
  private ngUnsubscribe: Subject<void> = new Subject<void>();

  constructor(
    public websocket: Websocket,
    private service: Service,
    private snackBar: MdSnackBar
  ) {
    service.setLang('de');
  }

  ngOnInit() {
    this.service.notificationEvent.takeUntil(this.ngUnsubscribe).subscribe(notification => {
      this.snackBar.open(notification.message, null, { duration: 3000 });
    });
  }

  ngOnDestroy() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }
}

