import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { FormGroup, FormBuilder } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil, take } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';

import { environment } from '../../environments';

import { Websocket, Utils, Service } from '../shared/shared';
import { Edge } from '../shared/edge/edge';

@Component({
  selector: 'index',
  templateUrl: './index.component.html'
})
export class IndexComponent {
  public env = environment;
  public form: FormGroup;
  private filter: string = '';

  private stopOnDestroy: Subject<void> = new Subject<void>();
  private allEdgeIds: string[] = [];
  private edges: Edge[] = [];
  private filteredTruncated: boolean = false;
  private static maxFilteredEdges = 20;

  constructor(
    public websocket: Websocket,
    public utils: Utils,
    private translate: TranslateService,
    private formBuilder: FormBuilder,
    private router: Router,
    private service: Service) {
    this.form = this.formBuilder.group({
      "password": this.formBuilder.control('user')
    });

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
    this.allEdgeIds = Object.keys(edges)
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
      this.filteredTruncated = true;
      this.edges = filteredEdges.slice(0, IndexComponent.maxFilteredEdges);
    } else {
      this.filteredTruncated = false;
      this.edges = filteredEdges;
    }
  }

  doLogin() {
    let password: string = this.form.value['password'];
    this.websocket.logIn(password);
  }

  onDestroy() {
    this.stopOnDestroy.next();
    this.stopOnDestroy.complete();
  }
}
