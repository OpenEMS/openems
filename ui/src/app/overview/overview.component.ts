import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { FormGroup, FormBuilder } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil, takeWhile } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';

import { environment } from '../../environments';

import { Websocket, Utils } from '../shared/shared';

@Component({
  selector: 'overview',
  templateUrl: './overview.component.html'
})
export class OverviewComponent implements OnInit {
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
    // websocket.edges.takeUntil(this.stopOnDestroy).subscribe(edges => {
    // if (Object.keys(edges).length == 1) {
    // redirect if only one edge
    // let edge = edges[Object.keys(devices)[0]];
    // this.router.navigate(['/device', edge.name]);
    // }
    // })

    websocket.edges.pipe(takeUntil(this.stopOnDestroy)).subscribe(edges => {
      if (Object.keys(edges).length == 1) {
        this.websocket.MTO = false;
        let edge = edges[Object.keys(edges)[0]];
        this.router.navigate(['/device', edge.name]);
      }
    })
  }

  ngOnInit() {
    console.log("Overview geladen");
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
