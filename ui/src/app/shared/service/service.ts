import { ErrorHandler, Injectable } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { Cookie } from 'ng2-cookies';
import { BehaviorSubject, Subject } from 'rxjs';
import { filter, first, map } from 'rxjs/operators';
import { Edge } from '../edge/edge';
import { EdgeConfig } from '../edge/edgeconfig';
import { Edges } from '../jsonrpc/shared';
import { LanguageTag, Language } from '../translate/language';
import { Role } from '../type/role';
import { DefaultTypes } from './defaulttypes';

@Injectable()
export class Service implements ErrorHandler {

  public static readonly TIMEOUT = 15_000;

  public notificationEvent: Subject<DefaultTypes.Notification> = new Subject<DefaultTypes.Notification>();

  /**
   * Holds the currently selected Edge.
   */
  public readonly currentEdge: BehaviorSubject<Edge> = new BehaviorSubject<Edge>(null);

  /**
   * Holds references of Edge-IDs (=key) to Edge objects (=value)
   */
  public readonly edges: BehaviorSubject<{ [edgeId: string]: Edge }> = new BehaviorSubject({});

  /**
   * Holds reference to Websocket. This is set by Websocket in constructor.
   */
  public websocket = null;

  constructor(
    private router: Router,
    public translate: TranslateService
  ) {
    // add language
    translate.addLangs(Language.getLanguages());
    // this language will be used as a fallback when a translation isn't found in the current language
    translate.setDefaultLang(LanguageTag.DE);
  }

  /**
   * Reset everything to default
   */
  public initialize() {
    console.log("initialize")
    this.edges.next({});
  }

  /**
   * Sets the application language
   */
  public setLang(id: LanguageTag) {
    this.translate.use(id);
    // TODO set locale for date-fns: https://date-fns.org/docs/I18n
  }

  /**
   * Gets the token from the cookie
   */
  public getToken(): string {
    return Cookie.get("token");
  }

  /**
   * Sets the token in the cookie
   */
  public setToken(token: string) {
    Cookie.set("token", token);
  }

  /**
   * Removes the token from the cookie
   */
  public removeToken() {
    Cookie.delete("token");
  }

  /**
   * Shows a nofication using toastr
   */
  public notify(notification: DefaultTypes.Notification) {
    this.notificationEvent.next(notification);
  }

  /**
   * Handles an application error
   */
  public handleError(error: any) {
    console.error(error);
    // TODO: show notification
    // let notification: Notification = {
    //     type: "error",
    //     message: error
    // };
    // this.notify(notification);
  }

  /**
   * Parses the route params and sets the current edge
   */
  public setCurrentEdge(activatedRoute: ActivatedRoute): Promise<Edge> {
    return new Promise((resolve, reject) => {
      let route = activatedRoute.snapshot;
      let edgeId = route.params["edgeId"];

      let timeout = setTimeout(() => {
        if (edgeId != null) {
          // Timeout: redirect to index
          this.router.navigate(['/index']);
        }
        subscription.unsubscribe();
        setCurrentEdge.apply(null);
      }, Service.TIMEOUT);

      let setCurrentEdge = (edge: Edge) => {
        if (edge != null) {
          if (edge != this.currentEdge.value) {
            edge.markAsCurrentEdge(this.websocket);
          }
        }
        if (edge != this.currentEdge.value) {
          this.currentEdge.next(edge);
        }
        resolve(edge);
      }

      let subscription = this.edges
        .pipe(
          filter(edges => edgeId in edges),
          first(),
          map(edges => edges[edgeId])
        )
        .subscribe(edge => {
          clearTimeout(timeout);
          setCurrentEdge(edge);

        }, error => {
          clearTimeout(timeout);
          console.error("Error while setting current edge: ", error);
          setCurrentEdge(null);
        })
    });
  }

  /**
   * Gets the current Edge - or waits for a Edge if it is not available yet.
   */
  public getCurrentEdge(): Promise<Edge> {
    return this.currentEdge.pipe(
      filter(edge => edge != null),
      first()
    ).toPromise();
  }

  /**
   * Gets the EdgeConfig of the current Edge - or waits for Edge and Config if they are not available yet.
   */
  public getConfig(): Promise<EdgeConfig> {
    return new Promise<EdgeConfig>((resolve, reject) => {
      this.getCurrentEdge().then(edge => {
        edge.getConfig(this.websocket).pipe(
          filter(config => config.isValid()),
          first()
        ).toPromise()
          .then(config => resolve(config))
          .catch(reason => reject(reason));
      })
        .catch(reason => reject(reason));
    });
  }

  /**
   * Handles being authenticated. Updates the list of Edges.
   */
  public handleAuthentication(token: string, edges: Edges) {
    this.websocket.status = 'online';

    // received login token -> save in cookie
    this.setToken(token);

    // Metadata
    let newEdges = {};
    for (let edge of edges) {
      let newEdge = new Edge(
        edge.id,
        edge.comment,
        edge.producttype,
        ("version" in edge) ? edge["version"] : "0.0.0",
        Role.getRole(edge.role),
        edge.isOnline
      );
      newEdges[newEdge.id] = newEdge;
    }
    this.edges.next(newEdges);
  }
}