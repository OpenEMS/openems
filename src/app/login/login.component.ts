import { Component, OnInit } from '@angular/core';
import { LocalstorageService } from '../service/localstorage.service';
import { ConnectionService, Connection } from '../service/connection.service';
import { FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html'
})
export class LoginComponent implements OnInit {

  constructor(
    public formbuilder: FormBuilder,
    private localstorageService: LocalstorageService,
    private connectionService: ConnectionService,
    private router: Router) {
  }

  doLogin(connection: Connection) {
    if("password" in connection) {
      var password: string = connection["password"];
      connection.connectWithPassword(password);
    }
  }

  doLogout(connection: Connection) {
    connection.close();
  }

  ngOnInit() {
  }
}
