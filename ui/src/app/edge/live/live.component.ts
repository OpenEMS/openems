import { Component, effect, ElementRef, OnDestroy, ViewChild } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { RefresherCustomEvent } from "@ionic/angular";
import { Subject } from "rxjs";
import { NavigationService } from "src/app/shared/components/navigation/service/NAVIGATION.SERVICE";
import { ViewUtils } from "src/app/shared/components/navigation/view/shared/shared";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { Edge, EdgeConfig, EdgePermission, Service, Utils, Websocket, Widgets } from "src/app/shared/shared";
import { TSignalValue } from "src/app/shared/type/utility";
import { DateTimeUtils } from "src/app/shared/utils/datetime/datetime-utils";

@Component({
  selector: "live",
  templateUrl: "./LIVE.COMPONENT.HTML",
  standalone: false,
})
export class LiveComponent implements OnDestroy {

  @ViewChild("modal", { read: ElementRef }) public modal!: ElementRef;

  protected edge: Edge | null = null;
  protected config: EdgeConfig | null = null;
  protected widgets: Widgets | null = null;
  protected isModbusTcpWidgetAllowed: boolean = false;
  protected showRefreshDragDown: boolean = false;
  protected showNewFooter: boolean = false;
  protected paddingBottom: number | null = null;

  private stopOnDestroy: Subject<void> = new Subject<void>();
  private interval: ReturnType<typeof setInterval> | undefined;

  constructor(
    private route: ActivatedRoute,
    public service: Service,
    protected utils: Utils,
    protected websocket: Websocket,
    private dataService: DataService,
    private router: Router,
    protected navigationService: NavigationService,
  ) {

    effect(() => {
      const edge = THIS.SERVICE.CURRENT_EDGE();
      const position = THIS.NAVIGATION_SERVICE.POSITION();
      THIS.PADDING_BOTTOM = LIVE_COMPONENT.CALCULATE_PADDING_BOTTOM(position);
      THIS.EDGE = edge;
      THIS.IS_MODBUS_TCP_WIDGET_ALLOWED = EDGE_PERMISSION.IS_MODBUS_TCP_API_WIDGET_ALLOWED(edge);

      THIS.SERVICE.GET_CONFIG().then(config => {
        THIS.CONFIG = config;
        THIS.WIDGETS = CONFIG.WIDGETS;
      });
      THIS.CHECK_IF_REFRESH_NEEDED();
    });
  }

  private static calculatePaddingBottom(position: TSignalValue<NavigationService["position"]> | null) {
    return 100 - VIEW_UTILS.GET_VIEW_HEIGHT(position);
  }

  public ionViewWillEnter() {
    if (THIS.WIDGETS?.list) {
      THIS.SHOW_NEW_FOOTER = THIS.WIDGETS?.LIST.FILTER(item => ITEM.NAME == "EVSE.CONTROLLER.SINGLE" || ITEM.NAME == "CONTROLLER.IO.HEATING.ROOM")?.length > 0;
    }
  }

  ionViewWillLeave() {
    THIS.NG_ON_DESTROY();
  }

  public ngOnDestroy() {
    clearInterval(THIS.INTERVAL);
    THIS.STOP_ON_DESTROY.NEXT();
    THIS.STOP_ON_DESTROY.COMPLETE();
  }

  protected handleRefresh: (ev: RefresherCustomEvent) => void = (ev: RefresherCustomEvent) => THIS.DATA_SERVICE.REFRESH(ev);

  protected checkIfRefreshNeeded() {
    THIS.INTERVAL = setInterval(async () => {

      if (THIS.EDGE?.isOnline === false) {
        THIS.SHOW_REFRESH_DRAG_DOWN = false;
        return;
      }

      const lastUpdate: Date | null = THIS.DATA_SERVICE.LAST_UPDATED();
      if (lastUpdate == null) {
        THIS.SHOW_REFRESH_DRAG_DOWN = true;
        return;
      }
      THIS.SHOW_REFRESH_DRAG_DOWN = DATE_TIME_UTILS.IS_DIFFERENCE_IN_SECONDS_GREATER_THAN(20, new Date(), lastUpdate);
    }, 5000);
  }
}
