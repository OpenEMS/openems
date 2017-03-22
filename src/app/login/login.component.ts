import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { FormGroup, FormBuilder } from '@angular/forms';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { Subscription } from 'rxjs/Subscription';

import { environment } from '../../environments';

import { WebappService, WebsocketService, Websocket, Notification } from '../shared/shared';

@Component({
  selector: 'login',
  templateUrl: './login.component.html'
})
export class LoginComponent implements OnInit, OnDestroy {

  private forms: FormGroup[] = [];
  private websocketSubscriptions: Subscription[] = [];
  private backend;

  constructor(
    private websocketService: WebsocketService,
    private router: Router,
    private formBuilder: FormBuilder,
    private webapp: WebappService) {
  }

  ngOnInit() {
    this.backend = environment.backend;

    this.websocketService.clearCurrentDevice();
    for (let websocketName in this.websocketService.websockets) {
      let websocket = this.websocketService.websockets[websocketName];
      this.websocketSubscriptions.push(websocket.event.subscribe((value) => this.websocketEvent(value)));
      let form: FormGroup = this.formBuilder.group({
        "password": this.formBuilder.control('')
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
    let password: string = form.value['password']
    websocket.connectWithPassword(password);
  }

  doLogout(form: FormGroup) {
    let websocket: Websocket = form['_websocket'];
    websocket.close();
  }

  websocketEvent(value: Notification) {
    let allConnected = true;
    for (let websocketName in this.websocketService.websockets) {
      let websocket = this.websocketService.websockets[websocketName];
      if (!websocket.isConnected) {
        allConnected = false;
        break;
      }
    }
    if (allConnected) {
      this.webapp.notify({
        type: "success",
        message: "Alle Verbindungen hergestellt."
      });
      this.router.navigate(['/overview']);
    }
  }
}
