import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { FormGroup, FormBuilder } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil, takeWhile } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';

import { environment } from '../../environments';

import { Websocket, Utils, Service } from '../shared/shared';
import { enableDebugTools } from '../../../node_modules/@angular/platform-browser';

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
    private router: Router,
    private service: Service) {
    this.service.backUrl = null;
    this.form = this.formBuilder.group({
      "password": this.formBuilder.control('user')
    });
    this.filter = this.formBuilder.group({
      "filter": this.formBuilder.control('')
    });

    //Forwarding to device overview if there is only 1 edge

    websocket.edges.pipe(takeUntil(this.stopOnDestroy)).subscribe(edges => {
      if (Object.keys(edges).length == 1) {
        let edge = edges[Object.keys(edges)[0]];
        this.router.navigate(['/device', edge.name]);
      }
    })
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
