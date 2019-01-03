import { Injectable, ErrorHandler } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Subject, BehaviorSubject } from 'rxjs';
import { Cookie } from 'ng2-cookies';

import { DefaultTypes } from './defaulttypes';
import { ActivatedRoute, Router } from '@angular/router';
import { Edge } from '../edge/edge';
import { filter, first, map } from 'rxjs/operators';

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

  constructor(
    private router: Router,
    public translate: TranslateService
  ) {
    // add language
    translate.addLangs(["de", "en", "cz", "nl", "es"]);
    // this language will be used as a fallback when a translation isn't found in the current language
    translate.setDefaultLang('de');
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
  public setLang(id: DefaultTypes.LanguageTag) {
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
          edge.markAsCurrentEdge();
        }
        this.currentEdge.next(edge);
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

}