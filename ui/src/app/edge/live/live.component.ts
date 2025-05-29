import { Component, effect, ElementRef, OnDestroy, ViewChild } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { RefresherCustomEvent } from "@ionic/angular";
import { NgxMasonryComponent, NgxMasonryOptions } from "ngx-masonry";
import { Subject } from "rxjs";
import { NavigationService } from "src/app/shared/components/navigation/service/navigation.service";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { Edge, EdgeConfig, EdgePermission, Service, Utils, Websocket, Widgets } from "src/app/shared/shared";
import { DateTimeUtils } from "src/app/shared/utils/datetime/datetime-utils";

@Component({
  selector: "live",
  templateUrl: "./live.component.html",
  standalone: false,
})
export class LiveComponent implements OnDestroy {

  @ViewChild("modal", { read: ElementRef })
  public modal!: ElementRef;

  @ViewChild(NgxMasonryComponent)
  set masonrySetter(masonry: NgxMasonryComponent) {
    this.masonry = masonry;
    if (this.masonry) this.updateMasonryLayout();
  }
  protected masonry: NgxMasonryComponent;

  protected edge: Edge | null = null;
  protected config: EdgeConfig | null = null;
  protected widgets: Widgets | null = null;
  protected isModbusTcpWidgetAllowed: boolean = false;
  protected showRefreshDragDown: boolean = false;
  protected showNewFooter: boolean = false;
  protected masonryOptions: NgxMasonryOptions = {
    itemSelector: ".masonry-item",
    columnWidth: ".masonry-sizer",
    gutter: 0,
    fitWidth: false,
    percentPosition: false,
  };

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
      const edge = this.service.currentEdge();
      this.edge = edge;
      this.isModbusTcpWidgetAllowed = EdgePermission.isModbusTcpApiWidgetAllowed(edge);
      this.service.getConfig().then(config => {
        this.config = config;
        this.widgets = config.widgets;
        setTimeout(() => this.updateMasonryLayout(), 200);
      });
      this.checkIfRefreshNeeded();
    });
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

  protected updateMasonryLayout() {
    if (!this.masonry) {
      setTimeout(() => this.updateMasonryLayout(), 200);
      return;
    }
    this.masonry.reloadItems();
    this.masonry.layout();
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
