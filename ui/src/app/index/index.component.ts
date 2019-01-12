import { Component } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { environment } from '../../environments';
import { AuthenticateWithPasswordRequest } from '../shared/jsonrpc/request/authenticateWithPasswordRequest';
import { AuthenticateWithPasswordResponse } from '../shared/jsonrpc/response/authenticateWithPasswordResponse';
import { Edge, Service, Utils, Websocket } from '../shared/shared';

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
  private static maxFilteredEdges = Infinity;
  private slice: number = 20;

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
    service.edges.pipe(takeUntil(this.stopOnDestroy)).subscribe(edges => {
      let edgeIds = Object.keys(edges);
      if (edgeIds.length == 1) {
        let edge = edges[edgeIds[0]];
        if (edge.isOnline) {
          this.router.navigate(['/device', edge.id]);
        }
      }
      this.updateFilteredEdges();
    })
  }

  updateFilteredEdges() {
    let edges = this.service.edges.getValue();
    this.allEdgeIds = Object.keys(edges)
    let filter = this.filter.toLowerCase();
    let filteredEdges = Object.keys(edges)
      .filter(edgeId => {
        let edge = edges[edgeId];
        if (/* name */ edge.id.toLowerCase().includes(filter)
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
    let request = new AuthenticateWithPasswordRequest({ password: password });
    this.websocket.sendRequest(request).then(response => {
      this.handleAuthenticateWithPasswordResponse(response as AuthenticateWithPasswordResponse);
    }).then(reason => {
      console.error("Error...");
    })
  }

  /**
   * Handles a AuthenticateWithPasswordResponse.
   * 
   * @param message 
   */
  private handleAuthenticateWithPasswordResponse(message: AuthenticateWithPasswordResponse) {
    this.service.handleAuthentication(message.result.token, message.result.edges);
  }

  doInfinite(infiniteScroll) {
    setTimeout(() => {
      this.slice += 5;
      infiniteScroll.target.complete();
    }, 200);
  }

  onDestroy() {
    this.stopOnDestroy.next();
    this.stopOnDestroy.complete();
  }
}
