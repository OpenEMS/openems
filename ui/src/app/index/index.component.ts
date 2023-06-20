import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { InfiniteScrollCustomEvent } from '@ionic/angular';
import { Subject } from 'rxjs';
import { filter, take } from 'rxjs/operators';
import { environment } from 'src/environments';

import { AuthenticateWithPasswordRequest } from '../shared/jsonrpc/request/authenticateWithPasswordRequest';
import { Edge, Service, Utils, Websocket } from '../shared/shared';
import { Role } from '../shared/type/role';

@Component({
  selector: 'index',
  templateUrl: './index.component.html'
})
export class IndexComponent implements OnInit, OnDestroy {

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
  public filteredEdges: Edge[] = [];


  private stopOnDestroy: Subject<void> = new Subject<void>();
  private page = 0;
  private query: string | null = null;

  /** Limits edges in pagination response */
  private readonly limit: number = 20;
  /** True, if all available edges for this user had been retrieved */
  private limitReached: boolean = false;

  protected formIsDisabled: boolean = false;
  protected onlyOneEdgeAvailable: boolean = false;
  protected spinnerId: string = 'index';
  protected loading: boolean = false;

  constructor(
    public service: Service,
    public websocket: Websocket,
    public utils: Utils,
    private router: Router,
    private route: ActivatedRoute
  ) { }

  ngOnInit() {
    this.page = 0;
    this.filteredEdges = [];
    this.limitReached = false;
    this.service.metadata.pipe(filter(metadata => !!metadata), take(1)).subscribe(() => {
      this.init();
    });
  }

  async ionViewWillEnter() {

    // Execute Login-Request if url path matches 'demo' 
    if (this.route.snapshot.routeConfig.path == 'demo') {

      // Wait for Websocket
      await new Promise((resolve) => setTimeout(() => {
        if (this.websocket.status == 'waiting for credentials') {
          resolve(this.websocket.login(new AuthenticateWithPasswordRequest({ username: 'demo@fenecon.de', password: 'femsdemo' })));
        }
      }, 2000)).then(() => { this.service.setCurrentComponent('', this.route); });
    } else {
      this.service.setCurrentComponent('', this.route);
    }
  }

  /**
   * Search on change, triggered by searchbar input-event.
   * 
   * @param event from template passed event
   */
  protected searchOnChange() {
    this.filteredEdges = [];
    this.page = 0;
    this.limitReached = false;

    this.loadNextPage().then((edges) => {
      this.filteredEdges = edges;
      this.page++;
    });
  }

  /**
   * Login to OpenEMS Edge or Backend.
   * 
   * @param param data provided in login form
   */
  public doLogin(param: { username?: string, password: string }) {
    this.query = "";
    this.limitReached = false;

    // Prevent that user submits via keyevent 'enter' multiple times
    if (this.formIsDisabled) {
      return;
    }

    this.formIsDisabled = true;
    this.websocket.login(new AuthenticateWithPasswordRequest(param))
      .finally(() => {

        // Unclean
        this.ngOnInit();
        this.formIsDisabled = false;
      });
  }

  private init() {
    this.loadNextPage().then((edges) => {
      this.service.metadata
        .pipe(
          filter(metadata => !!metadata),
          take(1)
        )
        .subscribe(metadata => {

          let edgeIds = Object.keys(metadata.edges);
          this.onlyOneEdgeAvailable = edgeIds.length <= 1;
          this.noEdges = edgeIds.length === 0;
          this.loggedInUserCanInstall = Role.isAtLeast(metadata.user.globalRole, "installer");

          // Forward directly to device page, if
          // - Direct local access to Edge
          // - No installer (i.e. guest or owner) and access to only one Edge
          if (environment.backend == 'OpenEMS Edge' || (!this.loggedInUserCanInstall && edgeIds.length == 1)) {
            let edge = metadata.edges[edgeIds[0]];
            this.router.navigate(['/device', edge.id]);
            return;
          }
          this.filteredEdges = edges;
        });
    });
  }

  /**
   * Updates available edges on scroll-event  
   * 
   * @param infiniteScroll the InfiniteScrollCustomEvent
   */
  doInfinite(infiniteScroll: InfiniteScrollCustomEvent) {
    setTimeout(() => {
      this.page++;
      this.loadNextPage().then((edges) => {
        this.filteredEdges.push(...edges);
        infiniteScroll.target.complete();
      }).catch(() => {
        infiniteScroll.target.complete();
      });
    }, 200);
  }

  ngOnDestroy() {
    this.stopOnDestroy.next();
    this.stopOnDestroy.complete();
  }

  loadNextPage(): Promise<Edge[]> {

    this.loading = true;
    return new Promise<Edge[]>((resolve, reject) => {
      if (this.limitReached) {
        resolve([]);
        return;
      }
      this.service.getEdges(this.page, this.query, this.limit)
        .then((edges) => {
          this.limitReached = edges.length < this.limit;
          resolve(edges);
        }).catch((err) => {
          reject(err);
        });
    }).finally(() =>
      this.loading = false);
  }
}