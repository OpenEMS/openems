import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';

import { environment } from '../../environments';

import { Websocket, Utils, Service } from '../shared/shared';
import { Edge } from '../shared/edge/edge';

import { HttpClient } from '@angular/common/http';


@Component({
  selector: 'index',
  templateUrl: './index.component.html'
})
export class IndexComponent {
  public env = environment;
  public form: FormGroup;
  public wpForm: FormGroup;
  private filter: string = '';

  private stopOnDestroy: Subject<void> = new Subject<void>();
  private static maxFilteredEdges = 20;

  constructor(
    public websocket: Websocket,
    public utils: Utils,
    private formBuilder: FormBuilder,
    private wpformBuilder: FormBuilder,
    private router: Router,
    private http: HttpClient,
    private service: Service) {
    this.form = this.formBuilder.group({
      "password": this.formBuilder.control('user')
    });

    this.wpForm = this.wpformBuilder.group({
      username: ['', Validators.required],
      password: ['', Validators.required]
    }),


      //Forwarding to device index if there is only 1 edge
      websocket.edges.pipe(takeUntil(this.stopOnDestroy)).subscribe(edges => {
        let edgeIds = Object.keys(edges);
        if (edgeIds.length == 1) {
          let edge = edges[edgeIds[0]];
          this.router.navigate(['/device', edge.name]);
        }

        this.updateFilteredEdges();
      })
  }

  updateFilteredEdges() {
    let edges = this.websocket.edges.getValue();
    let filter = this.filter.toLowerCase();
    let filteredEdges = Object.keys(edges)
      .filter(edgeId => {
        let edge = edges[edgeId];
        if (/* name */ edge.name.toLowerCase().includes(filter)
          || /* comment */ edge.comment.toLowerCase().includes(filter)) {
          return true;
        }
        return false;
      })
      .map(edgeId => edges[edgeId]);

    if (filteredEdges.length > IndexComponent.maxFilteredEdges) {
    } else {
    }
  }

  doLogin() {
    let password: string = this.form.value['password'];
    this.websocket.logIn(password);
  }

  doWPLogin() {
    if (this.wpForm.invalid) {
      return;
    }

    let password: string = this.wpForm.value['password'];
    let username: string = this.wpForm.value['username'];
    this.http.get('https://www.energydepot.de/api/auth/generate_auth_cookie/?username=' + username + '&password=' + password)

      .subscribe(data => { this.service.setWPCookies(data['cookie'], data['cookie_name']); this.websocket.wpconnect(); });



  }

  onDestroy() {
    this.stopOnDestroy.next();
    this.stopOnDestroy.complete();
  }
}
