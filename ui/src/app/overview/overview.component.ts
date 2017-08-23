import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { FormGroup, FormBuilder } from '@angular/forms';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { Subscription } from 'rxjs/Subscription';
import { Subject } from 'rxjs/Subject';
import { TranslateService } from '@ngx-translate/core';

import { environment } from '../../environments';

import { Service, Websocket, Notification, Utils } from '../shared/shared';

@Component({
  selector: 'overview',
  templateUrl: './overview.component.html'
})
export class OverviewComponent implements OnInit, OnDestroy {
  public env = environment;
  public form: FormGroup;

  private ngUnsubscribe: Subject<void> = new Subject<void>();

  constructor(
    private websocket: Websocket,
    private utils: Utils,
    private translate: TranslateService,
    formBuilder: FormBuilder) {
    this.form = formBuilder.group({
      "password": formBuilder.control('user')
    });
  }

  ngOnInit() {
    this.websocket.clearCurrentDevice();
  }

  ngOnDestroy() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }

  doLogin() {
    let password: string = this.form.value['password'];
    this.websocket.connectWithPassword(password);
  }

  doLogout(form: FormGroup) {
    this.websocket.close();
  }
}
