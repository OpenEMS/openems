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

@Component({
  selector: 'overview',
  templateUrl: './overview.component.html'
})
export class OverviewComponent {
  public env = environment;
  public form: FormGroup;
  public filter: FormGroup;

  private stopOnDestroy: Subject<void> = new Subject<void>();

  constructor(
    public websocket: Websocket,
    public utils: Utils,
    private translate: TranslateService,
    private formBuilder: FormBuilder,
    private router: Router) {
    this.form = formBuilder.group({
      "password": formBuilder.control('user')
    });
    this.filter = formBuilder.group({
      "filter": formBuilder.control('')
    });
    // TODO should only forward when automatic login was successful and user did not come to this page on purpose
    // websocket.devices.takeUntil(this.stopOnDestroy).subscribe(devices => {
    // if (Object.keys(devices).length == 1) {
    // redirect if only one device
    // let device = devices[Object.keys(devices)[0]];
    // this.router.navigate(['/device', device.name]);
    // }
    // })
  }

  doLogin() {
    let password: string = this.form.value['password'];
    this.websocket.logIn(password);
  }

  doLogout(form: FormGroup) {
    this.websocket.logOut();
  }

  onDestroy() {
    this.stopOnDestroy.next();
    this.stopOnDestroy.complete();
  }
}
