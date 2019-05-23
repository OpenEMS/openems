import { ErrorHandler, Injectable } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { Cookie } from 'ng2-cookies';
import { BehaviorSubject, Subject, Subscription } from 'rxjs';
import { filter, first, map } from 'rxjs/operators';
import { Edge } from '../edge/edge';
import { EdgeConfig } from '../edge/edgeconfig';
import { Edges } from '../jsonrpc/shared';
import { LanguageTag, Language } from '../translate/language';
import { Role } from '../type/role';
import { DefaultTypes } from './defaulttypes';
import { Widget, WidgetNature, WidgetFactory } from '../type/widget';
import { ToastController } from '@ionic/angular';
import { addDays, format, getDate, getMonth, getYear, isSameDay, subDays } from 'date-fns';
import { IMyDate, IMyDateRange, IMyDateRangeModel, IMyDrpOptions } from 'mydaterangepicker';

type PeriodString = "today" | "yesterday" | "lastWeek" | "lastMonth" | "lastYear" | "otherPeriod";

@Injectable()
export class Service implements ErrorHandler {

  public readonly TODAY = new Date();
  public readonly YESTERDAY = subDays(new Date(), 1);
  public readonly TOMORROW = addDays(new Date(), 1);
  public activePeriod: PeriodString = "today";
  public fromDate = this.TODAY;
  public toDate = this.TODAY;
  public dateRange: IMyDateRange;
  public activePeriodText: string = "";
  public dateRangePickerOptions: IMyDrpOptions = {
    inline: true,
    showClearBtn: false,
    showApplyBtn: false,
    dateFormat: 'dd.mm.yyyy',
    disableUntil: { day: 1, month: 1, year: 2013 }, // TODO start with date since the edge is available
    disableSince: this.toIMyDate(this.TOMORROW),
    showWeekNumbers: true,
    showClearDateRangeBtn: false,
    editableDateRangeField: false,
    openSelectorOnInputClick: true,
  };

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

  constructor(
    private router: Router,
    public translate: TranslateService,
    private toaster: ToastController
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
  public setCurrentComponent(currentPageTitle: string, activatedRoute: ActivatedRoute): Promise<Edge> {
    return new Promise((resolve, reject) => {
      // Set the currentPageTitle only once per ActivatedRoute
      if (this.currentActivatedRoute != activatedRoute) {
        if (currentPageTitle == null || currentPageTitle.trim() === '') {
          this.currentPageTitle = 'OpenEMS UI';
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
        edge.isOnline
      );
      newEdges[newEdge.id] = newEdge;
    }
    this.edges.next(newEdges);
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

  ///////////////////
  public clickOtherPeriod() {
    if (this.activePeriod === 'otherPeriod') {
      this.setPeriod("today");
    } else {
      this.setPeriod("otherPeriod", this.YESTERDAY, this.TODAY);
    }
  }

  public onDateRangeChanged(event: IMyDateRangeModel) {
    console.log("ONDATERANGECHANGED")
    let fromDate = event.beginJsDate;
    let toDate = event.endJsDate;
    if (isSameDay(fromDate, toDate)) {
      // only one day selected
      if (isSameDay(this.TODAY, fromDate)) {
        this.setPeriod("today");
        return;
      } else if (isSameDay(this.YESTERDAY, fromDate)) {
        this.setPeriod("yesterday");
        return;
      }
    }
    this.setPeriod("otherPeriod", fromDate, toDate);
  }

  /**
   * This is called by the input button on the UI.
   * 
   * @param period
   * @param from
   * @param to
   */
  public setPeriod(period: PeriodString, fromDate?: Date, toDate?: Date) {
    this.activePeriod = period;
    switch (period) {
      case "yesterday": {
        let yesterday = subDays(new Date(), 1);
        this.setDateRange(yesterday, yesterday);
        this.activePeriodText = this.translate.instant('Edge.History.Yesterday') + ", " + format(yesterday, this.translate.instant('General.DateFormat'));
        break;
      }
      case "otherPeriod":
        if (fromDate > toDate) {
          toDate = fromDate;
        }
        this.setDateRange(fromDate, toDate);
        this.activePeriodText = this.translate.instant('General.PeriodFromTo', {
          value1: format(fromDate, this.translate.instant('General.DateFormat')),
          value2: format(toDate, this.translate.instant('General.DateFormat'))
        });
        break;
      case "today":
      default:
        let today = new Date();
        this.setDateRange(today, today);
        this.activePeriodText = this.translate.instant('Edge.History.Today') + ", " + format(today, this.translate.instant('General.DateFormat'));
        break;
    }
  }

  /**
   * Sets the current time period.
   * 
   * @param fromDate the starting date
   * @param toDate   the end date
   */
  public setDateRange(fromDate: Date, toDate: Date) {
    this.fromDate = fromDate;
    this.toDate = toDate;
    this.dateRange = {
      beginDate: this.toIMyDate(fromDate),
      endDate: this.toIMyDate(toDate)
    }
  }

  /**
   * Converts a 'Date' to 'IMyDate' format.
   * 
   * @param date the 'Date'
   * @returns the 'IMyDate'
   */
  public toIMyDate(date: Date): IMyDate {
    return { year: getYear(date), month: getMonth(date) + 1, day: getDate(date) }
  }

  //////////////////////////////

  public async toast(message: string, level: 'success' | 'warning' | 'danger') {
    const toast = await this.toaster.create({
      message: message,
      color: level,
      duration: 2000
    });
    toast.present();
  }
}