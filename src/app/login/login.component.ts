import { Component, OnInit } from '@angular/core';
import { LocalstorageService } from '../service/localstorage.service';
import { WebSocketService, WebsocketContainer } from '../service/websocket.service';
import { FormBuilder, Validators } from '@angular/forms';
import { DataService } from '../service/data.service';
import { Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html'
})
export class LoginComponent implements OnInit {

  doLogin() {
    var container: WebsocketContainer = this.dataservice.getWebsocketContainerWithLogin(this.loginForm.value.password);
    if(container != null) {
       this.router.navigate(['/monitor/current']);
    }
  }

  constructor(
    public formbuilder: FormBuilder,
    private localstorage: LocalstorageService,
    private websocket: WebSocketService,
    private dataservice: DataService,
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
