import { Injectable } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ModalController, ToastController } from '@ionic/angular';
import { LangChangeEvent, TranslateService } from '@ngx-translate/core';
import { NgxSpinnerService } from 'ngx-spinner';
import { BehaviorSubject, Subject, Subscription } from 'rxjs';
import { filter, first, map, take } from 'rxjs/operators';
import { environment } from 'src/environments';
import { Edge } from '../edge/edge';
import { EdgeConfig } from '../edge/edgeconfig';
import { JsonrpcResponseError } from '../jsonrpc/base';
import { QueryHistoricTimeseriesEnergyRequest } from '../jsonrpc/request/queryHistoricTimeseriesEnergyRequest';
import { QueryHistoricTimeseriesEnergyResponse } from '../jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { User } from '../jsonrpc/shared';
import { ChannelAddress } from '../shared';
import { Language } from '../type/language';
import { AbstractService } from './abstractservice';
import { DefaultTypes } from './defaulttypes';
import { Websocket } from './websocket';

@Injectable()
export class Service extends AbstractService {

  public static readonly TIMEOUT = 15_000;

  public notificationEvent: Subject<DefaultTypes.Notification> = new Subject<DefaultTypes.Notification>();

  /**
   * Represents the resolution of used device
   * Checks if smartphone resolution is used
   */
  public deviceHeight: number = 0;
  public deviceWidth: number = 0;
  public isSmartphoneResolution: boolean = false;
  public isSmartphoneResolutionSubject: Subject<boolean> = new Subject<boolean>();

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
  public readonly metadata: BehaviorSubject<{
    user: User, edges: { [edgeId: string]: Edge }
  }> = new BehaviorSubject(null);

  /**
   * Holds reference to Websocket. This is set by Websocket in constructor.
   */
  public websocket: Websocket = null;

  constructor(
    private router: Router,
    private spinner: NgxSpinnerService,
    private toaster: ToastController,
    public modalCtrl: ModalController,
    public translate: TranslateService,
  ) {
    super();
    // add language
    translate.addLangs(Language.ALL.map(l => l.key));
    // this language will be used as a fallback when a translation isn't found in the current language
    translate.setDefaultLang(Language.DEFAULT.key);

    // initialize history period
    this.historyPeriod = new DefaultTypes.HistoryPeriod(new Date(), new Date());

    // React on Language Change and update language
    translate.onLangChange.subscribe((event: LangChangeEvent) => {
      this.setLang(Language.getByKey(event.lang));
    });
  }

  public setLang(language: Language) {
    if (language !== null) {
      this.translate.use(language.key);
    } else {
      this.translate.use(Language.DEFAULT.key);
    }
    // TODO set locale for date-fns: https://date-fns.org/docs/I18n
  }

  public getDocsLang(): string {
    if (this.translate.currentLang == "de") {
      return "de";
    } else {
      return "en";
    }
  }

  public notify(notification: DefaultTypes.Notification) {
    this.notificationEvent.next(notification);
  }

  public handleError(error: any) {
    console.error(error);
    // TODO: show notification
    // let notification: Notification = {
    //     type: "error",
    //     message: error
    // };
    // this.notify(notification);
  }

  public setCurrentComponent(currentPageTitle: string | { languageKey: string }, activatedRoute: ActivatedRoute): Promise<Edge> {
    return new Promise((resolve) => {
      // Set the currentPageTitle only once per ActivatedRoute
      if (this.currentActivatedRoute != activatedRoute) {
        if (typeof currentPageTitle === 'string') {
          // Use given page title directly
          if (currentPageTitle == null || currentPageTitle.trim() === '') {
            this.currentPageTitle = environment.uiTitle;
          } else {
            this.currentPageTitle = currentPageTitle;
          }

        } else {
          // Translate from key
          this.translate.get(currentPageTitle.languageKey).pipe(
            take(1),
          ).subscribe(title => this.currentPageTitle = title);
        }
      }
      this.currentActivatedRoute = activatedRoute;

      // Get Edge-ID. If not existing -> resolve null
      let route = activatedRoute.snapshot;
      let edgeId = route.params["edgeId"];
      if (edgeId == null) {
        // allow modal components to get edge id
        if (route.url.length == 0) {
          this.getCurrentEdge().then(edge => {
            resolve(edge);
          })
        } else {
          resolve(null);
        }
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

      subscription = this.metadata
        .pipe(
          filter(metadata => metadata != null && edgeId in metadata.edges),
          first(),
          map(metadata => metadata.edges[edgeId])
        )
        .subscribe(edge => {
          setCurrentEdge(edge);
        }, error => {
          console.error("Error while setting current edge: ", error);
          onError();
        })
    });
  }

  public getCurrentEdge(): Promise<Edge> {
    return this.currentEdge.pipe(
      filter(edge => edge != null),
      first()
    ).toPromise();
  }

  public getConfig(): Promise<EdgeConfig> {
    return new Promise<EdgeConfig>((resolve, reject) => {
      this.getCurrentEdge().then(edge => {
        edge.getConfig(this.websocket).pipe(
          filter(config => config != null && config.isValid()),
          first()
        ).toPromise()
          .then(config => resolve(config))
          .catch(reason => reject(reason));
      })
        .catch(reason => reject(reason));
    });
  }

  public onLogout() {
    this.currentEdge.next(null);
    this.metadata.next(null);
    this.router.navigate(['/index']);
  }

  public getChannelAddresses(edge: Edge, channels: ChannelAddress[]): Promise<ChannelAddress[]> {
    return new Promise((resolve) => {
      resolve(channels);
    });
  };

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

        this.queryEnergyTimeout = null;

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
            let request = new QueryHistoricTimeseriesEnergyRequest(source.fromDate, source.toDate, source.channels);
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

  public startSpinner(selector: string) {
    this.spinner.show(selector, {
      type: "ball-clip-rotate-multiple",
      fullScreen: false,
      bdColor: "rgba(0, 0, 0, 0.8)",
      size: "medium",
      color: "#fff"
    })
  }

  public startSpinnerTransparentBackground(selector: string) {
    this.spinner.show(selector, {
      type: "ball-clip-rotate-multiple",
      fullScreen: false,
      bdColor: "rgba(0, 0, 0, 0)",
      size: "medium",
      color: "var(--ion-color-primary)"
    })
  }

  public stopSpinner(selector: string) {
    this.spinner.hide(selector)
  }

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
   * Currently selected history period
   */
  public historyPeriod: DefaultTypes.HistoryPeriod;

  /**
   * Currently selected history period string
   * 
   * initialized as day, is getting changed by pickdate component
   */
  public periodString: DefaultTypes.PeriodString = 'day';
}
