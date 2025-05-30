import { AfterViewInit, Component, effect, ElementRef, OnDestroy, QueryList, ViewChild, ViewChildren } from "@angular/core";
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
export class LiveComponent implements OnDestroy, AfterViewInit {

  @ViewChild("modal", { read: ElementRef })
  public modal!: ElementRef;

  @ViewChildren('widgetItem', { read: ElementRef })
  protected widgetItems!: QueryList<ElementRef>;
  private resizeObservers: ResizeObserver[] = [];

  protected edge: Edge | null = null;
  protected config: EdgeConfig | null = null;
  protected widgets: Widgets | null = null;
  protected isModbusTcpWidgetAllowed: boolean = false;
  protected showRefreshDragDown: boolean = false;
  protected showNewFooter: boolean = false;
  protected masonry: NgxMasonryComponent;
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
      });
      this.checkIfRefreshNeeded();
    });
  }

  @ViewChild(NgxMasonryComponent)
  set masonrySetter(masonry: NgxMasonryComponent) {
    this.masonry = masonry;
    if (this.masonry) {
      this.updateMasonryLayout();
    }
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

  ngAfterViewInit() {
    this.widgetItems.changes.subscribe(() => {
      this.observeWidgetSizes();
      this.updateMasonryLayout();
    });
    this.observeWidgetSizes();
    this.updateMasonryLayout();
  }

  private observeWidgetSizes() {
    this.resizeObservers.forEach(obs => obs.disconnect());
    this.resizeObservers = [];

    this.widgetItems.forEach((element) => {
      const observer = new ResizeObserver(() => {
        this.updateMasonryLayout();
      });
      observer.observe(element.nativeElement);
      this.resizeObservers.push(observer);
    });
  }

  protected updateMasonryLayout() {
    if (this.masonry) {
      this.masonry.reloadItems();
      this.masonry.layout();
    }
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
