import { Component, effect, ElementRef, OnDestroy, ViewChild, inject } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { RefresherCustomEvent } from "@ionic/angular";
import { Subject } from "rxjs";
import { NavigationService } from "src/app/shared/components/navigation/service/navigation.service";
import { ViewUtils } from "src/app/shared/components/navigation/view/shared/shared";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { Edge, EdgeConfig, EdgePermission, Service, Utils, Websocket, Widgets } from "src/app/shared/shared";
import { TSignalValue } from "src/app/shared/type/utility";
import { DateTimeUtils } from "src/app/shared/utils/datetime/datetime-utils";

@Component({
  selector: "live",
  templateUrl: "./live.component.html",
  standalone: false,
})
export class LiveComponent implements OnDestroy {
  private route = inject(ActivatedRoute);
  service = inject(Service);
  protected utils = inject(Utils);
  protected websocket = inject(Websocket);
  private dataService = inject(DataService);
  private router = inject(Router);
  protected navigationService = inject(NavigationService);


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

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {

    effect(() => {
      const edge = this.service.currentEdge();
      const position = this.navigationService.position();
      this.paddingBottom = LiveComponent.calculatePaddingBottom(position);
      this.edge = edge;
      this.isModbusTcpWidgetAllowed = EdgePermission.isModbusTcpApiWidgetAllowed(edge);

      this.service.getConfig().then(config => {
        this.config = config;
        this.widgets = config.widgets;
      });
      this.checkIfRefreshNeeded();
    });
  }

  private static calculatePaddingBottom(position: TSignalValue<NavigationService["position"]> | null) {
    return 100 - ViewUtils.getViewHeight(position);
  }

  public ionViewWillEnter() {
    if (this.widgets?.list) {
      this.showNewFooter = this.widgets?.list.filter(item => item.name == "Evse.Controller.Single" || item.name == "Controller.IO.Heating.Room")?.length > 0;
    }
  }

  ionViewWillLeave() {
    this.ngOnDestroy();
  }

  public ngOnDestroy() {
    clearInterval(this.interval);
    this.stopOnDestroy.next();
    this.stopOnDestroy.complete();
  }

  protected handleRefresh: (ev: RefresherCustomEvent) => void = (ev: RefresherCustomEvent) => this.dataService.refresh(ev);

  protected checkIfRefreshNeeded() {
    this.interval = setInterval(async () => {

      if (this.edge?.isOnline === false) {
        this.showRefreshDragDown = false;
        return;
      }

      const lastUpdate: Date | null = this.dataService.lastUpdated();
      if (lastUpdate == null) {
        this.showRefreshDragDown = true;
        return;
      }
      this.showRefreshDragDown = DateTimeUtils.isDifferenceInSecondsGreaterThan(20, new Date(), lastUpdate);
    }, 5000);
  }
}
