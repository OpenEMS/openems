import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { FormGroup, FormBuilder } from '@angular/forms';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { Subscription } from 'rxjs/Subscription';
import { Subject } from 'rxjs/Subject';
import { TranslateService } from '@ngx-translate/core';

import { environment } from '../../environments';

import { WebappService, WebsocketService, Websocket, Notification, TemplateHelper } from '../shared/shared';

@Component({
  selector: 'overview',
  templateUrl: './overview.component.html'
})
export class OverviewComponent implements OnInit, OnDestroy {

  public forms: FormGroup[] = [];

  private ngUnsubscribe: Subject<void> = new Subject<void>();

  constructor(
    private websocketService: WebsocketService,
    private webappService: WebappService,
    private tmpl: TemplateHelper,
    private router: Router,
    private formBuilder: FormBuilder,
    private translate: TranslateService) {
  }

  ngOnInit() {
    this.websocketService.clearCurrentDevice();
    for (let websocketName in this.websocketService.websockets) {
      let websocket = this.websocketService.websockets[websocketName];
      websocket.event.takeUntil(this.ngUnsubscribe).subscribe(notification => this.websocketEvent(notification));
      let form: FormGroup = this.formBuilder.group({
        "password": this.formBuilder.control('user')
      });
      form['_websocket'] = websocket;
      this.forms.push(form);
    }
  }

  ngOnDestroy() {
    this.ngUnsubscribe.next();
    this.ngUnsubscribe.complete();
  }

  doLogin(form: FormGroup) {
    let websocket: Websocket = form['_websocket'];
    let password: string = form.value['password'];
    websocket.connectWithPassword(password);
  }

  doLogout(form: FormGroup) {
    let websocket: Websocket = form['_websocket'];
    websocket.isConnected = false;
    websocket.close();
  }

  websocketEvent(notification: Notification) {
    let allConnected = true;
    let noOfConnectedDevices = 0;
    let lastDevice = null;
    for (let websocketName in this.websocketService.websockets) {
      let websocket = this.websocketService.websockets[websocketName];
      if (websocket.isConnected) {
        for (let deviceName in websocket.devices) {
          noOfConnectedDevices++;
          lastDevice = websocket.devices[deviceName];
        }
      } else {
        allConnected = false;
        break;
      }
    }
    if (allConnected) {
      this.webappService.notify({
        type: "success",
        message: this.translate.instant('Overview.AllConnected')
      });
    }
  }
}
