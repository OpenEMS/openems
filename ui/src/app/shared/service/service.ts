// @ts-strict-ignore
import { registerLocaleData } from "@angular/common";
import { Injectable } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { ToastController } from "@ionic/angular";
import { LangChangeEvent, TranslateService } from "@ngx-translate/core";
import { NgxSpinnerService } from "ngx-spinner";
import { BehaviorSubject, Subject } from "rxjs";
import { filter, first, map, take } from "rxjs/operators";
import { ChosenFilter } from "src/app/index/filter/filter.component";
import { environment } from "src/environments";
import { ChartConstants } from "../components/chart/chart.constants";
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
import { Language } from "../type/language";
import { Role } from "../type/role";
import { DateUtils } from "../utils/date/dateutils";
import { AbstractService } from "./abstractservice";
import { DefaultTypes } from "./defaulttypes";
import { Websocket } from "./websocket";

@Injectable()
export class Service extends AbstractService {

  public static readonly TIMEOUT = 15_000;

  public notificationEvent: Subject<DefaultTypes.Notification> = new Subject<DefaultTypes.Notification>();

  /**
 * Currently selected history period
 */
  public historyPeriod: BehaviorSubject<DefaultTypes.HistoryPeriod>;

  /**
   * Currently selected history period string
   *
   * initialized as day, is getting changed by pickdate component
   */
  public periodString: DefaultTypes.PeriodString = DefaultTypes.PeriodString.DAY;

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
  public readonly currentEdge: BehaviorSubject<Edge> = new BehaviorSubject<Edge>(null);

  /**
   * Holds references of Edge-IDs (=key) to Edge objects (=value)
   */
  public readonly metadata: BehaviorSubject<{
    user: User, edges: { [edgeId: string]: Edge }
  }> = new BehaviorSubject(null);

  public currentUser: User | null = null;

  /**
   * Holds the current Activated Route
   */
  private currentActivatedRoute: ActivatedRoute | null = null;

  private queryEnergyQueue: {
    fromDate: Date, toDate: Date, channels: ChannelAddress[], promises: { resolve, reject }[]
  }[] = [];
  private queryEnergyTimeout: any = null;

  constructor(
    private router: Router,
    private spinner: NgxSpinnerService,
    private toaster: ToastController,
    public translate: TranslateService,
  ) {
    super();
    // add language
    translate.addLangs(Language.ALL.map(l => l.key));
    // this language will be used as a fallback when a translation isn't found in the current language
    translate.setDefaultLang(Language.DEFAULT.key);

    // initialize history period
    this.historyPeriod = new BehaviorSubject(new DefaultTypes.HistoryPeriod(new Date(), new Date()));

    // React on Language Change and update language
    translate.onLangChange.subscribe((event: LangChangeEvent) => {
      this.setLang(Language.getByKey(event.lang));
    });
  }

  public setLang(language: Language) {
    if (language !== null) {
      registerLocaleData(Language.getLocale(language.key));
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

  // https://v16.angular.io/api/core/ErrorHandler#errorhandler
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  public override handleError(error: any) {
    console.error(error);
    // TODO: show notification
    // let notification: Notification = {
    //     type: "error",
    //     message: error
    // };
    // this.notify(notification);
  }

  public setCurrentComponent(currentPageTitle: string | { languageKey: string, interpolateParams?: {} }, activatedRoute: ActivatedRoute): Promise<Edge> {
    return new Promise((resolve, reject) => {
      // Set the currentPageTitle only once per ActivatedRoute
      if (this.currentActivatedRoute != activatedRoute) {
        if (typeof currentPageTitle === "string") {
          // Use given page title directly
          if (currentPageTitle == null || currentPageTitle.trim() === "") {
            this.currentPageTitle = environment.uiTitle;
          } else {
            this.currentPageTitle = currentPageTitle;
          }

        } else {
          // Translate from key
          this.translate.get(currentPageTitle.languageKey, currentPageTitle.interpolateParams).pipe(
            take(1),
          ).subscribe(title => this.currentPageTitle = title);
        }
      }
      this.currentActivatedRoute = activatedRoute;

      this.getCurrentEdge().then(edge => {
        resolve(edge);
      }).catch(reject);
    });
  }

  public getCurrentEdge(): Promise<Edge> {
    // TODO maybe timeout
    return new Promise<Edge>((resolve) => {
      this.currentEdge.pipe(
        filter(edge => edge != null),
        first(),
      ).toPromise().then(resolve);
      if (this.currentEdge.value) {
        resolve(this.currentEdge.value);
      }
    });
  }

  /**
   * Gets the current user
   *
   * @returns a Promise of the user
   */
  public getCurrentUser(): Promise<User> {
    return new Promise<User>((resolve) => {
      this.metadata.pipe(
        filter(metadata => metadata != null && metadata.user != null),
        map(metadata => metadata.user),
        first(),
      ).toPromise().then(resolve);
      if (this.currentUser) {
        resolve(this.currentUser);
      }
    });
  }

  public getConfig(): Promise<EdgeConfig> {
    return new Promise<EdgeConfig>((resolve, reject) => {
      this.getCurrentEdge().then(edge => {
        edge.getFirstValidConfig(this.websocket)
          .then(resolve)
          .catch(reject);
      }).catch(reason => reject(reason));
    });
  }

  public onLogout() {
    this.currentEdge.next(null);
    this.metadata.next(null);
    this.websocket.state.set(States.NOT_AUTHENTICATED);
    this.router.navigate(["/login"]);
  }

  public getChannelAddresses(edge: Edge, channels: ChannelAddress[]): Promise<ChannelAddress[]> {
    return new Promise((resolve) => {
      resolve(channels);
    });
  }

  public queryEnergy(fromDate: Date, toDate: Date, channels: ChannelAddress[]): Promise<QueryHistoricTimeseriesEnergyResponse> {
    // keep only the date, without time
    fromDate.setHours(0, 0, 0, 0);
    toDate.setHours(0, 0, 0, 0);
    const promise = { resolve: null, reject: null };
    const response = new Promise<QueryHistoricTimeseriesEnergyResponse>((resolve, reject) => {
      promise.resolve = resolve;
      promise.reject = reject;
    });
    this.queryEnergyQueue.push({
      fromDate: fromDate,
      toDate: toDate,
      channels: channels,
      promises: [promise],
    });

    if (this.queryEnergyTimeout == null) {
      this.queryEnergyTimeout = setTimeout(() => {
        this.queryEnergyTimeout = null;

        const mergedRequests: {
          fromDate: Date,
          toDate: Date,
          channels: ChannelAddress[],
          promises: { resolve, reject }[];
        }[] = [];

        let request;
        while ((request = this.queryEnergyQueue.pop())) {
          if (mergedRequests.length === 0) {
            mergedRequests.push(request);
          } else {
            let merged = false;
            for (const mergedRequest of mergedRequests) {
              if (mergedRequest.fromDate.valueOf() === request.fromDate.valueOf()
                && mergedRequest.toDate.valueOf() === request.toDate.valueOf()) {
                // same date -> merge
                mergedRequest.promises = mergedRequest.promises.concat(request.promises);
                for (const newChannel of request.channels) {
                  if (!mergedRequest.channels.some(existingChannel =>
                    existingChannel.channelId === newChannel.channelId &&
                    existingChannel.componentId === newChannel.componentId)) {
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
          for (const source of mergedRequests) {

            // Jump to next request for empty channelAddresses
            if (source.channels.length === 0) {
              continue;
            }

            const request = new QueryHistoricTimeseriesEnergyRequest(
              DateUtils.maxDate(source.fromDate, edge?.firstSetupProtocol),
              source.toDate,
              source.channels,
            );

            this.activeQueryData = request.id;
            edge.sendRequest(this.websocket, request)
              .then(response => {
                if (this.activeQueryData !== response.id) {
                  return;
                }

                const result = (response as QueryHistoricTimeseriesEnergyResponse).result;

                if (Object.keys(result.data).length === 0) {
                  for (const promise of source.promises) {
                    promise.reject(new JsonrpcResponseError(response.id, { code: 0, message: "Result was empty" }));
                  }
                  return;
                }

                for (const promise of source.promises) {
                  promise.resolve(response as QueryHistoricTimeseriesEnergyResponse);
                }
              })
              .catch(async reason => {
                for (const promise of source.promises) {
                  promise.reject(new JsonrpcResponseError((await response).id, { code: 0, message: "Result was empty" }));
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
   * @param page the page number
   * @param query the query to restrict the edgeId
   * @param limit the number of edges to be retrieved
   * @returns a Promise
   */
  public getEdges(page: number, query?: string, limit?: number, searchParamsObj?: { [id: string]: ChosenFilter["value"] }): Promise<Edge[]> {
    return new Promise<Edge[]>((resolve, reject) => {
      this.websocket.sendSafeRequest(
        new GetEdgesRequest({
          page: page,
          ...(query && query != "" && { query: query }),
          ...(limit && { limit: limit }),
          ...(searchParamsObj && { searchParams: searchParamsObj }),
        })).then((response) => {

          const result = (response as GetEdgesResponse).result;

          // TODO change edges-map to array or other way around
          const value = this.metadata.value;
          const mappedResult = [];
          for (const edge of result.edges) {
            const mappedEdge = new Edge(
              edge.id,
              edge.comment,
              edge.producttype,
              ("version" in edge) ? edge["version"] : "0.0.0",
              Role.getRole(edge.role.toString()),
              edge.isOnline,
              edge.lastmessage,
              edge.sumState,
              DateUtils.stringToDate(edge.firstSetupProtocol?.toString()),
            );
            value.edges[edge.id] = mappedEdge;
            mappedResult.push(mappedEdge);
          }

          this.metadata.next(value);
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
      const existingEdge = this.metadata.value?.edges[edgeId];
      if (existingEdge) {
        this.currentEdge.next(existingEdge);
        resolve(existingEdge);
        return;
      }
      this.websocket.sendSafeRequest(new GetEdgeRequest({ edgeId: edgeId })).then((response) => {
        const edgeData = (response as GetEdgeResponse).result.edge;
        const value = this.metadata.value;
        const currentEdge = new Edge(
          edgeData.id,
          edgeData.comment,
          edgeData.producttype,
          ("version" in edgeData) ? edgeData["version"] : "0.0.0",
          Role.getRole(edgeData.role.toString()),
          edgeData.isOnline,
          edgeData.lastmessage,
          edgeData.sumState,
          DateUtils.stringToDate(edgeData.firstSetupProtocol?.toString()));
        this.currentEdge.next(currentEdge);
        value.edges[edgeData.id] = currentEdge;
        this.metadata.next(value);
        resolve(currentEdge);
      }).catch(reject);
    });
  }

  public startSpinner(selector: string) {
    this.spinner.show(selector, {
      type: "ball-clip-rotate-multiple",
      fullScreen: false,
      bdColor: "rgba(0, 0, 0, 0.8)",
      size: "medium",
      color: "#fff",
    });
  }

  public startSpinnerTransparentBackground(selector: string) {
    this.spinner.show(selector, {
      type: "ball-clip-rotate-multiple",
      fullScreen: false,
      bdColor: "rgba(0, 0, 0, 0)",
      size: "medium",
      color: "var(--ion-color-primary)",
    });
  }

  public stopSpinner(selector: string) {
    this.spinner.hide(selector);
  }

  public async toast(message: string, level: "success" | "warning" | "danger", duration?: number) {
    const toast = await this.toaster.create({
      message: message,
      color: level,
      duration: duration ?? 2000,
      cssClass: "container",
    });
    toast.present();
  }
}
