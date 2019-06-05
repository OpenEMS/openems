import { Injectable, ErrorHandler } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Subject, BehaviorSubject, Subscription } from 'rxjs';
import { Cookie } from 'ng2-cookies';
import { DefaultTypes } from './defaulttypes';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { SpinnerDialog } from '@ionic-native/spinner-dialog/ngx';
import { Widget, WidgetNature, WidgetFactory } from '../type/widget';
import { ToastController } from '@ionic/angular';
import { Edge, EdgeConfig } from '../shared';
import { Language, LanguageTag } from '../translate/language';
import { filter, first, map } from 'rxjs/operators';
import { Edges } from '../jsonrpc/shared';
import { Role } from '../type/role';

@Injectable()
export class Service implements ErrorHandler {




  public static readonly TIMEOUT = 15_000;

  public notificationEvent: Subject<DefaultTypes.Notification> = new Subject<DefaultTypes.Notification>();

  /**
   * Holds the currenty selected Page Title.
   */
  public currentPageTitle: string;

  /**
   * Holds the current Activated Route
   */
  private currentActivatedRoute: ActivatedRoute = null;

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

  private auth: string;

  constructor(
    private router: Router,
    public translate: TranslateService,
    private http: HttpClient,
    private toaster: ToastController,
    public spinnerDialog: SpinnerDialog
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
   * Parses the route params and sets the current edge
   */
  public setCurrentComponent(currentPageTitle: string, activatedRoute: ActivatedRoute): Promise<Edge> {
    return new Promise((resolve, reject) => {
      // Set the currentPageTitle only once per ActivatedRoute
      if (this.currentActivatedRoute != activatedRoute) {
        if (currentPageTitle == null || currentPageTitle.trim() === '') {
          this.currentPageTitle = 'hy-EMS UI';
        } else {
          this.currentPageTitle = currentPageTitle;
        }
      }
      this.currentActivatedRoute = activatedRoute;

      // Get Edge-ID. If not existing -> resolve null
      let route = activatedRoute.snapshot;
      let edgeId = route.params["edgeId"];
      if (edgeId == null) {
        resolve(null);
      }

      let subscription: Subscription = null;
      let onError = () => {
        if (subscription != null) {
          subscription.unsubscribe();
        }
        setCurrentEdge.apply(null);
        // redirect to index
        this.router.navigate(['/index']);
      }

      let timeout = setTimeout(() => {
        console.error("Timeout while setting current edge");
        //  onError();
      }, Service.TIMEOUT);

      let setCurrentEdge = (edge: Edge) => {
        clearTimeout(timeout);
        if (edge != this.currentEdge.value) {
          if (edge != null) {
            edge.markAsCurrentEdge(this.websocket);
          }
          this.currentEdge.next(edge);
        }
        resolve(edge);
      }

      subscription = this.edges
        .pipe(
          filter(edges => edgeId in edges),
          first(),
          map(edges => edges[edgeId])
        )
        .subscribe(edge => {
          setCurrentEdge(edge);
        }, error => {
          console.error("Error while setting current edge: ", error);
          onError();
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
        edge.isOnline,
        edge.role
      );
      newEdges[newEdge.id] = newEdge;
    }
    this.edges.next(newEdges);
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
   * Defines the widgets that should be shown.
   */
  public getWidgets(): Promise<Widget[]> {
    return new Promise<Widget[]>((resolve, reject) => {
      this.getConfig().then(config => {
        let widgets = [];
        for (let nature of Object.values(WidgetNature).filter(v => typeof v === 'string')) {
          for (let componentId of config.getComponentIdsImplementingNature(nature)) {
            widgets.push({ name: nature, componentId: componentId })
          }
        }
        for (let factory of Object.values(WidgetFactory).filter(v => typeof v === 'string')) {
          for (let componentId of config.getComponentIdsByFactory(factory)) {
            widgets.push({ name: factory, componentId: componentId })
          }
        }
        resolve(widgets.sort((w1, w2) => {
          // explicitely sort ChannelThresholdControllers by their outputChannelAddress
          const outputChannelAddress1 = config.getComponentProperties(w1.componentId)['outputChannelAddress'];
          const outputChannelAddress2 = config.getComponentProperties(w2.componentId)['outputChannelAddress'];
          if (outputChannelAddress1 && outputChannelAddress2) {
            return outputChannelAddress1.localeCompare(outputChannelAddress2);
          } else if (outputChannelAddress1) {
            return 1;
          }

          return w1.componentId.localeCompare(w1.componentId);
        }));
      })
    });
  }

  public async toast(message: string, level: 'success' | 'warning' | 'danger') {
    const toast = await this.toaster.create({
      message: message,
      color: level,
      duration: 2000
    });
    toast.present();
  }


  public sendWPPasswordRetrieve(username: string): Promise<any> {


    return this.http.get("https://www.energydepot.de/api/user/retrieve_password/?user_login=" + username).toPromise();
    /*
  .then((data) => {
      if (data['status'] === "ok") {
        return "ok";
      }
      if (data['status'] === "error") {
        return data['error'];
      }

  */
  }

  public setWPCookies(cookie_name: string, cookie: string) {
    Cookie.set("wpcookie", cookie);
    Cookie.set(cookie, cookie_name);
    // localStorage.setItem(cookie, cookie_name);
    //sessionStorage.setItem(cookie, cookie_name);
    console.info('COOKIES: ' + Cookie.get(cookie));
  }

  public getWPCookieParam(): string {
    let cookie: string = Cookie.get("wpcookie");
    if (cookie.length > 10) {
      return cookie + "=" + Cookie.get(cookie) + ";";
    } else {
      return "";
    }

  }

  public setAuth(username: string, password: string) {

    this.auth = btoa(username + ":" + password);
  }

  public getAuth(): string {
    return this.auth;
  }

}
