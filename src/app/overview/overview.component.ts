import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { FormGroup, FormBuilder } from '@angular/forms';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { Subscription } from 'rxjs/Subscription';

import { environment } from '../../environments';

import { WebappService, WebsocketService, Websocket, Notification } from '../shared/shared';

@Component({
  selector: 'overview',
  templateUrl: './overview.component.html'
})
export class OverviewComponent implements OnInit, OnDestroy {

  public forms: FormGroup[] = [];
  private websocketSubscriptions: Subscription[] = [];

  constructor(
    private websocketService: WebsocketService,
    private router: Router,
    private formBuilder: FormBuilder,
    private webapp: WebappService
  ) {

  }

  ngOnInit() {
    this.websocketService.clearCurrentDevice();
    for (let websocketName in this.websocketService.websockets) {
      let websocket = this.websocketService.websockets[websocketName];
      this.websocketSubscriptions.push(websocket.event.subscribe((value) => this.websocketEvent(value)));
      let form: FormGroup = this.formBuilder.group({
        "password": this.formBuilder.control('owner')
      });
      form['_websocket'] = websocket;
      this.forms.push(form);
    }
  }

  ngOnDestroy() {
    for (let subscription of this.websocketSubscriptions) {
      subscription.unsubscribe();
    }
  }

  doLogin(form: FormGroup) {
    let websocket: Websocket = form['_websocket'];
    let password: string = form.value['password'];
    websocket.connectionClosed = false;
    websocket.connectWithPassword(password);
  }

  doLogout(form: FormGroup) {
    let websocket: Websocket = form['_websocket'];
    websocket.connectionClosed = true;
    websocket.close();
  }

  reconnectFemsserver(form: FormGroup) {
    let websocket: Websocket = form['_websocket'];
    websocket.connectWithTokenOrSessionId();
  }

  websocketEvent(value: Notification) {
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
      this.webapp.notify({
        type: "success",
        message: "Alle Verbindungen hergestellt."
      });
      if (noOfConnectedDevices == 1) {
        console.log("device");
      }
    }
  }
}
