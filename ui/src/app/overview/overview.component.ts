import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { FormGroup, FormBuilder } from '@angular/forms';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { Subscription } from 'rxjs/Subscription';
import { Subject } from 'rxjs/Subject';
import { Observable } from 'rxjs/Observable';
import { TranslateService } from '@ngx-translate/core';

import { environment } from '../../environments';

import { Service, Websocket, Utils } from '../shared/shared';
import { DefaultMessages } from '../shared/service/defaultmessages';

@Component({
  selector: 'overview',
  templateUrl: './overview.component.html'
})
export class OverviewComponent {
  public env = environment;
  public form: FormGroup;

  constructor(
    private websocket: Websocket,
    private utils: Utils,
    private translate: TranslateService,
    private formBuilder: FormBuilder) {
    this.form = formBuilder.group({
      "password": formBuilder.control('user')
    });
  }

  doLogin() {
    let password: string = this.form.value['password'];
    this.websocket.send(DefaultMessages.authenticateLogin(password));
  }

  doLogout(form: FormGroup) {
    this.websocket.close();
  }
}
