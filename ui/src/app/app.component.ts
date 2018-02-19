import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs/Subject';
import 'rxjs/add/operator/takeUntil';
import { TranslateService } from '@ngx-translate/core';
import { ToasterService, BodyOutputType } from 'angular2-toaster';

import { environment } from '../environments';
import { Service, Websocket } from './shared/shared';

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
    private toaster: ToasterService
  ) {
    service.setLang('de');
  }

  ngOnInit() {
    this.service.notificationEvent.takeUntil(this.ngUnsubscribe).subscribe(notification => {
      this.toaster.pop({ type: notification.type, body: notification.message });
    });
  }

  ngOnDestroy() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }
}

