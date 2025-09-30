// @ts-strict-ignore
import { registerLocaleData } from "@angular/common";
import { effect, inject, Injectable, Injector, runInInjectionContext, signal, untracked, WritableSignal } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { ToastController } from "@ionic/angular";
import { LangChangeEvent, TranslateService } from "@ngx-translate/core";
import { NgxSpinnerService } from "ngx-spinner";
import { BehaviorSubject, Subject } from "rxjs";
import { take } from "rxjs/operators";
import { environment } from "src/environments";
import { ChartConstants } from "../components/chart/CHART.CONSTANTS";
import { Edge } from "../components/edge/edge";
import { EdgeConfig } from "../components/edge/edgeconfig";
import { JsonrpcResponseError } from "../jsonrpc/base";
import { GetEdgeRequest } from "../jsonrpc/request/getEdgeRequest";
import { GetEdgesRequest } from "../jsonrpc/request/getEdgesRequest";
import { QueryHistoricTimeseriesEnergyRequest } from "../jsonrpc/request/queryHistoricTimeseriesEnergyRequest";
import { GetEdgeResponse } from "../jsonrpc/response/getEdgeResponse";
import { GetEdgesResponse } from "../jsonrpc/response/getEdgesResponse";
import { QueryHistoricTimeseriesEnergyResponse } from "../jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { User } from "../jsonrpc/shared";
import { States } from "../ngrx-store/states";
import { ChannelAddress } from "../shared";
import { DefaultTypes } from "../type/defaulttypes";
import { Language } from "../type/language";
import { Role } from "../type/role";
import { DateUtils } from "../utils/date/dateutils";
import { AbstractService } from "./abstractservice";
import { RouteService } from "./ROUTE.SERVICE";
import { Websocket } from "./websocket";

@Injectable()
export class Service extends AbstractService {

  public static readonly TIMEOUT = 15_000;

  public notificationEvent: Subject<DEFAULT_TYPES.NOTIFICATION> = new Subject<DEFAULT_TYPES.NOTIFICATION>();

  /**
 * Currently selected history period
 */
  public historyPeriod: BehaviorSubject<DEFAULT_TYPES.HISTORY_PERIOD>;

  /**
   * Currently selected history period string
   *
   * initialized as day, is getting changed by pickdate component
   */
  public periodString: DEFAULT_TYPES.PERIOD_STRING = DEFAULT_TYPES.PERIOD_STRING.DAY;

  /**
   * Represents the resolution of used device
   * Checks if smartphone resolution is used
   */
  public deviceHeight: number = 0;
  public deviceWidth: number = 0;
  public isSmartphoneResolution: boolean = false;
  public isSmartphoneResolutionSubject: Subject<boolean> = new Subject<boolean>();
  public activeQueryData: string;

  /**
   * Holds the currenty selected Page Title.
   */
  public currentPageTitle: string;

  /**
   * Holds reference to Websocket. This is set by Websocket in constructor.
  */
  public websocket: Websocket = null;
  /**
   * Holds the currently selected Edge.
   */
  public readonly currentEdge: WritableSignal<Edge> = signal(null);

  /**
   * Holds references of Edge-IDs (=key) to Edge objects (=value)
   */
  public readonly metadata: BehaviorSubject<{
    user: User, edges: { [edgeId: string]: Edge }
  }> = new BehaviorSubject(null);

  /**
   * Holds the current Activated Route
   */
  private currentActivatedRoute: ActivatedRoute | null = null;

  private queryEnergyQueue: {
    fromDate: Date, toDate: Date, channels: ChannelAddress[], promises: { resolve, reject }[]
  }[] = [];
  private queryEnergyTimeout: any = null;
  private injector = inject(Injector);

  constructor(
    private router: Router,
    public spinner: NgxSpinnerService,
    private toaster: ToastController,
    public translate: TranslateService,
    private _injector: Injector,
    private routeService: RouteService,
  ) {

    super();
    // add language
    TRANSLATE.ADD_LANGS(LANGUAGE.ALL.MAP(l => L.KEY));
    // this language will be used as a fallback when a translation isn't found in the current language
    TRANSLATE.SET_DEFAULT_LANG(LANGUAGE.DEFAULT.KEY);

    // initialize history period
    THIS.HISTORY_PERIOD = new BehaviorSubject(new DEFAULT_TYPES.HISTORY_PERIOD(new Date(), new Date()));

    // React on Language Change and update language
    TRANSLATE.ON_LANG_CHANGE.SUBSCRIBE((event: LangChangeEvent) => {
      THIS.SET_LANG(LANGUAGE.GET_BY_KEY(EVENT.LANG));
    });
  }

  public setLang(language: Language) {
    if (language !== null) {
      registerLocaleData(LANGUAGE.GET_LOCALE(LANGUAGE.KEY));
      THIS.TRANSLATE.USE(LANGUAGE.KEY);
    } else {
      THIS.TRANSLATE.USE(LANGUAGE.DEFAULT.KEY);
    }
    // TODO set locale for date-fns: https://date-FNS.ORG/docs/I18n
  }

  public getDocsLang(): string {
    if (THIS.TRANSLATE.CURRENT_LANG == "de") {
      return "de";
    } else {
      return "en";
    }
  }

  public notify(notification: DEFAULT_TYPES.NOTIFICATION) {
    THIS.NOTIFICATION_EVENT.NEXT(notification);
  }

  // https://V16.ANGULAR.IO/api/core/ErrorHandler#errorhandler

  public override handleError(error: any) {
    CONSOLE.ERROR(error);
    // TODO: show notification
    // let notification: Notification = {
    //     type: "error",
    //     message: error
    // };
    // THIS.NOTIFY(notification);
  }

  public setCurrentComponent(currentPageTitle: string | { languageKey: string, interpolateParams?: {} }, activatedRoute: ActivatedRoute): Promise<Edge> {
    return new Promise((resolve, reject) => {
      // Set the currentPageTitle only once per ActivatedRoute
      if (THIS.CURRENT_ACTIVATED_ROUTE != activatedRoute) {
        if (typeof currentPageTitle === "string") {
          // Use given page title directly
          if (currentPageTitle == null || CURRENT_PAGE_TITLE.TRIM() === "") {
            THIS.CURRENT_PAGE_TITLE = ENVIRONMENT.UI_TITLE;
          } else {
            THIS.CURRENT_PAGE_TITLE = currentPageTitle;
          }

        } else {
          // Translate from key
          THIS.TRANSLATE.GET(CURRENT_PAGE_TITLE.LANGUAGE_KEY, CURRENT_PAGE_TITLE.INTERPOLATE_PARAMS).pipe(
            take(1),
          ).subscribe(title => THIS.CURRENT_PAGE_TITLE = title);
        }
      }
      THIS.CURRENT_ACTIVATED_ROUTE = activatedRoute;

      THIS.GET_CURRENT_EDGE().then(edge => {
        resolve(edge);
      }).catch(reject);
    });
  }

  public getCurrentEdge(): Promise<Edge> {
    return new Promise<Edge>((resolve) => {
      let isResolved = false; // Flag to ensure the Promise resolves only once

      // Use runInInjectionContext to provide the injector context
      const dispose = runInInjectionContext(THIS.INJECTOR, () => {
        return untracked(() => {
          return effect(() => {
            const edge = THIS.CURRENT_EDGE();
            if (edge != null && !isResolved) {
              isResolved = true; // Mark as resolved
              resolve(edge); // Resolve the Promise with the non-null value
              DISPOSE.DESTROY();
            }
          });
        });
      });
    });
  }

  public getConfig(): Promise<EdgeConfig> {
    return new Promise<EdgeConfig>((resolve, reject) => {
      THIS.GET_CURRENT_EDGE().then(edge => {
        EDGE.GET_FIRST_VALID_CONFIG(THIS.WEBSOCKET)
          .then(resolve)
          .catch(reject);
      }).catch(reason => reject(reason));
    });
  }

  public getNextConfig(): Promise<EdgeConfig> {
    return new Promise<EdgeConfig>((resolve, reject) => {
      THIS.GET_CURRENT_EDGE().then(edge => {
        EDGE.GET_FIRST_VALID_CONFIG(THIS.WEBSOCKET)
          .then(resolve)
          .catch(reject);
      }).catch(reason => reject(reason));
    });
  }

  public onLogout() {
    THIS.CURRENT_EDGE.SET(null);

    THIS.METADATA.NEXT(null);
    THIS.WEBSOCKET.STATE.SET(States.NOT_AUTHENTICATED);
    THIS.ROUTER.NAVIGATE(["/login"]);
  }

  public getChannelAddresses(edge: Edge, channels: ChannelAddress[]): Promise<ChannelAddress[]> {
    return new Promise((resolve) => {
      resolve(channels);
    });
  }

  public queryEnergy(fromDate: Date, toDate: Date, channels: ChannelAddress[]): Promise<QueryHistoricTimeseriesEnergyResponse> {
    // keep only the date, without time
    FROM_DATE.SET_HOURS(0, 0, 0, 0);
    TO_DATE.SET_HOURS(0, 0, 0, 0);
    const promise = { resolve: null, reject: null };
    const response = new Promise<QueryHistoricTimeseriesEnergyResponse>((resolve, reject) => {
      PROMISE.RESOLVE = resolve;
      PROMISE.REJECT = reject;
    });
    THIS.QUERY_ENERGY_QUEUE.PUSH({
      fromDate: fromDate,
      toDate: toDate,
      channels: channels,
      promises: [promise],
    });

    if (THIS.QUERY_ENERGY_TIMEOUT == null) {
      THIS.QUERY_ENERGY_TIMEOUT = setTimeout(() => {
        THIS.QUERY_ENERGY_TIMEOUT = null;

        const mergedRequests: {
          fromDate: Date,
          toDate: Date,
          channels: ChannelAddress[],
          promises: { resolve, reject }[];
        }[] = [];

        let request;
        while ((request = THIS.QUERY_ENERGY_QUEUE.POP())) {
          if (MERGED_REQUESTS.LENGTH === 0) {
            MERGED_REQUESTS.PUSH(request);
          } else {
            let merged = false;
            for (const mergedRequest of mergedRequests) {
              if (MERGED_REQUEST.FROM_DATE.VALUE_OF() === REQUEST.FROM_DATE.VALUE_OF()
                && MERGED_REQUEST.TO_DATE.VALUE_OF() === REQUEST.TO_DATE.VALUE_OF()) {
                // same date -> merge
                MERGED_REQUEST.PROMISES = MERGED_REQUEST.PROMISES.CONCAT(REQUEST.PROMISES);
                for (const newChannel of REQUEST.CHANNELS) {
                  if (!MERGED_REQUEST.CHANNELS.SOME(existingChannel =>
                    EXISTING_CHANNEL.CHANNEL_ID === NEW_CHANNEL.CHANNEL_ID &&
                    EXISTING_CHANNEL.COMPONENT_ID === NEW_CHANNEL.COMPONENT_ID)) {
                    MERGED_REQUEST.CHANNELS.PUSH(newChannel);
                  }
                }
                merged = true;
              }
            }
            if (!merged) {
              MERGED_REQUESTS.PUSH(request);
            }
          }
        }

        // send merged requests
        THIS.GET_CURRENT_EDGE().then(edge => {
          for (const source of mergedRequests) {

            // Jump to next request for empty channelAddresses
            if (!source?.channels?.length) {
              continue;
            }

            const request = new QueryHistoricTimeseriesEnergyRequest(
              DATE_UTILS.MAX_DATE(SOURCE.FROM_DATE, edge?.firstSetupProtocol),
              SOURCE.TO_DATE,
              SOURCE.CHANNELS,
            );

            THIS.ACTIVE_QUERY_DATA = REQUEST.ID;
            EDGE.SEND_REQUEST(THIS.WEBSOCKET, request)
              .then(response => {
                if (THIS.ACTIVE_QUERY_DATA !== RESPONSE.ID) {
                  return;
                }

                const result = (response as QueryHistoricTimeseriesEnergyResponse).result;

                if (OBJECT.KEYS(RESULT.DATA).length === 0) {
                  for (const promise of SOURCE.PROMISES) {
                    PROMISE.REJECT(new JsonrpcResponseError(RESPONSE.ID, { code: 0, message: "Result was empty" }));
                  }
                  return;
                }

                for (const promise of SOURCE.PROMISES) {
                  PROMISE.RESOLVE(response as QueryHistoricTimeseriesEnergyResponse);
                }
              })
              .catch(async reason => {
                for (const promise of SOURCE.PROMISES) {
                  PROMISE.REJECT(new JsonrpcResponseError((await response).id, { code: 0, message: "Result was empty" }));
                }
              });
          }
        });
      }, ChartConstants.REQUEST_TIMEOUT);
    }
    return response;
  }

  /**
   * Gets the page for the given number.
   *
   * @param req the get edges request
   * @returns a promise with the resulting edges
   */
  public getEdges(req: GetEdgesRequest): Promise<Edge[]> {
    return new Promise<Edge[]>((resolve, reject) => {
      THIS.WEBSOCKET.SEND_SAFE_REQUEST(req)
        .then((response) => {

          const result = (response as GetEdgesResponse).result;

          // TODO change edges-map to array or other way around
          const value = THIS.METADATA.VALUE;
          const mappedResult = [];
          for (const edge of RESULT.EDGES) {
            const mappedEdge = new Edge(
              EDGE.ID,
              EDGE.COMMENT,
              EDGE.PRODUCTTYPE,
              ("version" in edge) ? edge["version"] : "0.0.0",
              ROLE.GET_ROLE(EDGE.ROLE.TO_STRING()),
              EDGE.IS_ONLINE,
              EDGE.LASTMESSAGE,
              EDGE.SUM_STATE,
              DATE_UTILS.STRING_TO_DATE(EDGE.FIRST_SETUP_PROTOCOL?.toString()),
            );
            VALUE.EDGES[EDGE.ID] = mappedEdge;
            MAPPED_RESULT.PUSH(mappedEdge);
          }

          THIS.METADATA.NEXT(value);
          resolve(mappedResult);
        }).catch((err) => {
          reject(err);
        });
    });
  }

  /**
   * Updates the currentEdge in metadata
   *
   * @param edgeId the edgeId
   * @returns a empty Promise
   */
  public updateCurrentEdge(edgeId: string): Promise<Edge> {
    return new Promise<Edge>((resolve, reject) => {
      const existingEdge = THIS.METADATA.VALUE?.edges[edgeId];
      if (existingEdge) {
        THIS.CURRENT_EDGE.SET(existingEdge);
        resolve(existingEdge);
        return;
      }
      THIS.WEBSOCKET.SEND_SAFE_REQUEST(new GetEdgeRequest({ edgeId: edgeId })).then((response) => {
        const edgeData = (response as GetEdgeResponse).RESULT.EDGE;
        const value = THIS.METADATA.VALUE;
        const currentEdge = new Edge(
          EDGE_DATA.ID,
          EDGE_DATA.COMMENT,
          EDGE_DATA.PRODUCTTYPE,
          ("version" in edgeData) ? edgeData["version"] : "0.0.0",
          ROLE.GET_ROLE(EDGE_DATA.ROLE.TO_STRING()),
          EDGE_DATA.IS_ONLINE,
          EDGE_DATA.LASTMESSAGE,
          EDGE_DATA.SUM_STATE,
          DATE_UTILS.STRING_TO_DATE(EDGE_DATA.FIRST_SETUP_PROTOCOL?.toString()));
        THIS.CURRENT_EDGE.SET(currentEdge);
        VALUE.EDGES[EDGE_DATA.ID] = currentEdge;
        THIS.METADATA.NEXT(value);
        resolve(currentEdge);
      }).catch(reject);
    });
  }

  public startSpinner(selector: string) {
    THIS.SPINNER.SHOW(selector, {
      type: "ball-clip-rotate-multiple",
      fullScreen: false,
      bdColor: "rgba(0, 0, 0, 0.8)",
      size: "medium",
      color: "#fff",
    });
  }

  public startSpinnerTransparentBackground(selector: string) {
    THIS.SPINNER.SHOW(selector, {
      type: "ball-clip-rotate-multiple",
      fullScreen: false,
      bdColor: "rgba(0, 0, 0, 0)",
      size: "medium",
      color: "var(--ion-color-primary)",
    });
  }

  public stopSpinner(selector: string) {
    THIS.SPINNER.HIDE(selector);
  }

  public async toast(message: string, level: "success" | "warning" | "danger", duration?: number) {
    const toast = await THIS.TOASTER.CREATE({
      message: message,
      color: level,
      duration: duration ?? 2000,
      cssClass: "container",
    });
    TOAST.PRESENT();
  }
}
