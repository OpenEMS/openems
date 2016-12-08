import { Component, OnInit } from '@angular/core';
import { LocalstorageService } from '../service/localstorage.service';
import { WebSocketService } from '../service/websocket.service';
import { Connection, ActiveConnection } from '../service/connection';
import { FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html'
})
export class LoginComponent implements OnInit {

  doLogin() {
    var password: string = this.loginForm.value.password;
    var connection: Connection = this.websocket.getDefaultWithLogin(password);
    if(connection != null) {
       this.router.navigate(['/monitor/current']);
    }
  }

  constructor(
    public formbuilder: FormBuilder,
    private localstorage: LocalstorageService,
    private websocket: WebSocketService,
    private router: Router) {
  }

  private loginForm = this.formbuilder.group({
    password: ["", Validators.required]
  });

  ngOnInit() {
    this.localstorage.removeToken();
    this.websocket.closeDefault();
  }
}
