import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { FormGroup, FormBuilder } from '@angular/forms';

import { WebsocketService, Websocket } from '../service/websocket.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html'
})
export class LoginComponent implements OnInit {

  private forms: FormGroup[] = [];

  constructor(
    private websocketService: WebsocketService,
    private router: Router,
    private formBuilder: FormBuilder) {
  }

  ngOnInit() {
    this.websocketService.clearCurrentDevice();
    for (let websocketName in this.websocketService.websockets) {
      let websocket = this.websocketService.websockets[websocketName];
      let form: FormGroup = this.formBuilder.group({
        "password": this.formBuilder.control('')
      });
      form['_websocket'] = websocket;
      this.forms.push(form);
    }
  }

  loginOrLogout(form: FormGroup) {
    let websocket: Websocket = form['_websocket'];
    let password: string = form.value['password']
    if (websocket.isConnected) {
      websocket.close();
    } else {
      websocket.connectWithPassword(password);
    }
  }
}
