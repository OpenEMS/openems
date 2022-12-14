import { Component } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { environment } from 'src/environments';
import { AuthenticateWithPasswordRequest } from '../shared/jsonrpc/request/authenticateWithPasswordRequest';
import { Edge, Service, Utils, Websocket } from '../shared/shared';
import { Role } from '../shared/type/role';

@Component({
  selector: 'index',
  templateUrl: './index.component.html'
})
export class IndexComponent {

  private static readonly EDGE_ID_REGEXP = new RegExp('\\d+');

  public environment = environment;

  /**
   * True, if there is no access to any Edge.
   */
  public noEdges: boolean = false;

  /**
   * True, if the logged in user is allowed to install
   * new edges.
   */
  public loggedInUserCanInstall: boolean = false;

  public form: FormGroup;
  public filter: string = '';
  public filteredEdges: Edge[] = [];
  protected formIsDisabled: boolean = false;

  private stopOnDestroy: Subject<void> = new Subject<void>();
  public slice: number = 20;

  constructor(
    public service: Service,
    public websocket: Websocket,
    public utils: Utils,
    private router: Router,
    private route: ActivatedRoute,
  ) {
    service.metadata
      .pipe(
        takeUntil(this.stopOnDestroy),
        filter(metadata => metadata != null)
      )
      .subscribe(metadata => {
        let edgeIds = Object.keys(metadata.edges);
        this.noEdges = edgeIds.length === 0;
        this.loggedInUserCanInstall = Role.isAtLeast(metadata.user.globalRole, "installer");

        // Forward directly to device page, if
        // - Direct local access to Edge
        // - No installer (i.e. guest or owner) and access to only one Edge
        if (environment.backend == 'OpenEMS Edge' || (!this.loggedInUserCanInstall && edgeIds.length == 1)) {
          let edge = metadata.edges[edgeIds[0]];
          if (edge.isOnline) {
            this.router.navigate(['/device', edge.id]);
          }
        }

        this.updateFilteredEdges();
      })
  }

  updateFilteredEdges() {
    let filter = this.filter.toLowerCase();
    let allEdges = this.service.metadata.value?.edges ?? {};
    this.filteredEdges = Object.keys(allEdges)
      .filter(edgeId => {
        let edge = allEdges[edgeId];
        if (/* name */ edge.id.toLowerCase().includes(filter)
          || /* comment */ edge.comment.toLowerCase().includes(filter)
          || /* producttype */ edge.producttype.toLowerCase().includes(filter)) {
          return true;
        }
        return false;
      })
      .sort((edge1, edge2) => {
        // first: try to compare the number, e.g. 'edge5' < 'edge100'
        let e1match = edge1.match(IndexComponent.EDGE_ID_REGEXP)
        if (e1match != null) {
          let e2match = edge2.match(IndexComponent.EDGE_ID_REGEXP)
          if (e2match != null) {
            let e1 = Number(e1match[0]);
            let e2 = Number(e2match[0]);
            if (!isNaN(e1) && !isNaN(e2)) {
              return e1 - e2;
            }
          }
        }
        // second: apply 'natural sort' 
        return edge1.localeCompare(edge2);
      })
      .map(edgeId => allEdges[edgeId]);
  }

  /**
   * Login to OpenEMS Edge or Backend.
   * 
   * @param param data provided in login form
   */
  public doLogin(param: { username?: string, password: string }) {
    this.formIsDisabled = true;
    this.websocket.login(new AuthenticateWithPasswordRequest(param)).then(() => {
      this.formIsDisabled = false;
    })
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