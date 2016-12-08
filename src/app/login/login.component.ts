import { Component, OnInit } from '@angular/core';
import { LocalstorageService } from '../service/localstorage.service';
import { ConnectionService, Connection, ActiveConnection } from '../service/connection.service';
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
    var connection: Connection = this.connectionService.getDefaultWithLogin(password);
    if(connection != null) {
       this.router.navigate(['/monitor/current']);
    }
  }

  constructor(
    public formbuilder: FormBuilder,
    private localstorageService: LocalstorageService,
    private connectionService: ConnectionService,
    private router: Router) {
  }

  private loginForm = this.formbuilder.group({
    password: ["", Validators.required]
  });

  ngOnInit() {
    this.localstorageService.removeToken();
    this.connectionService.closeDefault();
  }
}
