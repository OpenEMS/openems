import { ErrorHandler, Injectable } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ToastController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import { Subject, BehaviorSubject, Subscription } from 'rxjs';
import { Cookie } from 'ng2-cookies';
import { HttpClient } from '@angular/common/http';
import { SpinnerDialog } from '@ionic-native/spinner-dialog/ngx';
import { Widget, WidgetNature, WidgetFactory, Widgets } from '../type/widget';
import { Edge, EdgeConfig } from '../shared';
import { filter, first, map } from 'rxjs/operators';
import { JsonrpcResponseError } from '../jsonrpc/base';
import { QueryHistoricTimeseriesEnergyRequest } from '../jsonrpc/request/queryHistoricTimeseriesEnergyRequest';
import { QueryHistoricTimeseriesEnergyResponse } from '../jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { Edges } from '../jsonrpc/shared';
import { ChannelAddress } from '../shared';
import { Language, LanguageTag } from '../translate/language';
import { Role } from '../type/role';
import { LoadingController } from '@ionic/angular';
import { DefaultTypes } from './defaulttypes';
import { ExecuteSystemCommandRequest } from '../jsonrpc/request/executeCommandRequest';
import { ComponentJsonApiRequest } from '../jsonrpc/request/componentJsonApiRequest';
import { ExecuteSystemCommandResponse } from '../jsonrpc/response/executeSystemCommandResponse';
import { UpdateSoftwareRequest } from '../jsonrpc/request/updateSoftwareRequest';
import { RestartSoftwareRequest } from '../jsonrpc/request/restartSoftwareRequest';

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

  loader: any;

  constructor(
    private router: Router,
    public translate: TranslateService,
    private http: HttpClient,
    private toaster: ToastController,
    public spinnerDialog: SpinnerDialog,
    public loadingController: LoadingController
  ) {
    // add language
    translate.addLangs(Language.getLanguages());
    // this language will be used as a fallback when a translation isn't found in the current language
    translate.setDefaultLang(LanguageTag.DE);
    // initialize history period
    this.historyPeriod = new DefaultTypes.HistoryPeriod(new Date(), new Date());
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
    return new Promise((resolve) => {
      // Set the currentPageTitle only once per ActivatedRoute
      if (this.currentActivatedRoute != activatedRoute) {
        if (currentPageTitle == null || currentPageTitle.trim() === '') {
          this.currentPageTitle = 'hy-control UI';
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
          filter(config => config != null && config.isValid()),
          first()
        ).toPromise()
          .then(config => resolve(config))
          .catch(reason => { console.error(reason), reject(reason) });
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
   * Gets the ChannelAdresses for cumulated values that should be queried.
   * 
   * @param edge the current Edge
   */
  public getChannelAddresses(edge: Edge, channels: ChannelAddress[]): Promise<ChannelAddress[]> {
    return new Promise((resolve) => {
      resolve(channels);
    });
  };

  /**
   * Sends the Historic Timeseries Data Query and makes sure the result is not empty.
   * 
   * @param fromDate the From-Date
   * @param toDate   the To-Date
   * @param edge     the current Edge
   * @param ws       the websocket
   */
  public queryEnergy(fromDate: Date, toDate: Date, channels: ChannelAddress[]): Promise<QueryHistoricTimeseriesEnergyResponse> {
    // keep only the date, without time
    fromDate.setHours(0, 0, 0, 0);
    toDate.setHours(0, 0, 0, 0);
    let promise = { resolve: null, reject: null };
    let response = new Promise<QueryHistoricTimeseriesEnergyResponse>((resolve, reject) => {
      promise.resolve = resolve;
      promise.reject = reject;
    });
    this.queryEnergyQueue.push(
      { fromDate: fromDate, toDate: toDate, channels: channels, promises: [promise] }
    );
    // try to merge requests within 100 ms
    if (this.queryEnergyTimeout == null) {
      this.queryEnergyTimeout = setTimeout(() => {

        // merge requests
        let mergedRequests: {
          fromDate: Date, toDate: Date, channels: ChannelAddress[], promises: { resolve, reject }[];
        }[] = [];
        let request;
        while (request = this.queryEnergyQueue.pop()) {
          if (mergedRequests.length == 0) {
            mergedRequests.push(request);
          } else {
            let merged = false;
            for (let mergedRequest of mergedRequests) {
              if (mergedRequest.fromDate.valueOf() === request.fromDate.valueOf()
                && mergedRequest.toDate.valueOf() === request.toDate.valueOf()) {
                // same date -> merge
                mergedRequest.promises = mergedRequest.promises.concat(request.promises);
                for (let newChannel of request.channels) {
                  let isAlreadyThere = false;
                  for (let existingChannel of mergedRequest.channels) {
                    if (existingChannel.channelId == newChannel.channelId && existingChannel.componentId == newChannel.componentId) {
                      isAlreadyThere = true;
                      break;
                    }
                  }
                  if (!isAlreadyThere) {
                    mergedRequest.channels.push(newChannel);
                  }
                }
                merged = true;
              }
            }
            if (!merged) {
              mergedRequests.push(request);
            }
          }
        }

        // send merged requests
        this.getCurrentEdge().then(edge => {
          for (let source of mergedRequests) {
            let request = new QueryHistoricTimeseriesEnergyRequest(source.fromDate, source.fromDate, source.channels);
            edge.sendRequest(this.websocket, request).then(response => {
              let result = (response as QueryHistoricTimeseriesEnergyResponse).result;
              if (Object.keys(result.data).length != 0) {
                for (let promise of source.promises) {
                  promise.resolve(response as QueryHistoricTimeseriesEnergyResponse);
                }
              } else {
                for (let promise of source.promises) {
                  promise.reject(new JsonrpcResponseError(response.id, { code: 0, message: "Result was empty" }));
                }
              }
            }).catch(reason => {
              for (let promise of source.promises) {
                promise.reject(reason);
              }
            });
          }
        });
      }, 100);
    }
    return response;
  }

  private queryEnergyQueue: {
    fromDate: Date, toDate: Date, channels: ChannelAddress[], promises: { resolve, reject }[]
  }[] = [];
  private queryEnergyTimeout: any = null;

  public async toast(message: string, level: 'success' | 'warning' | 'danger') {
    const toast = await this.toaster.create({
      message: message,
      color: level,
      duration: 2000,
      cssClass: 'container'
    });
    toast.present();
  }

  /**
   * checks if fems is allowed to show kWh
   */
  public isKwhAllowed(edge: Edge): boolean {
    return true;
  }

  /**
   * Currently selected history period
   */
  public historyPeriod: DefaultTypes.HistoryPeriod;

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

  public showLoader() {
    this.loader = this.loadingController.create({ message: 'Loading...' }).then((res) => { res.present(); });
  }

  public hideLoader() {
    this.loadingController.dismiss();
  }
  /**
   * Currently selected history period string
   * 
   * initialized as day, is getting changed by pickdate component
   */
  public periodString: DefaultTypes.PeriodString = 'day';

  public forceRestart() {
    this.getCurrentEdge().then(edge => {
      edge.sendRequest(this.websocket,
        new ComponentJsonApiRequest({
          componentId: "_host",
          payload: new RestartSoftwareRequest()
        })).then(response => {
          let result = (response as ExecuteSystemCommandResponse).result;

        }).catch(reason => {

        })
    });
  }

}