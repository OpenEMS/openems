import { Component, effect, ElementRef, OnDestroy, ViewChild } from "@angular/core";
import { ActivatedRoute, NavigationEnd, Router } from "@angular/router";
import { RefresherCustomEvent } from "@ionic/angular";
import { Subject } from "rxjs";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { Edge, EdgeConfig, EdgePermission, Service, Utils, Websocket, Widgets } from "src/app/shared/shared";
import { DateTimeUtils } from "src/app/shared/utils/datetime/datetime-utils";

@Component({
  selector: "live",
  templateUrl: "./live.component.html",
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

  private stopOnDestroy: Subject<void> = new Subject<void>();
  private interval: ReturnType<typeof setInterval> | undefined;

  constructor(
    private route: ActivatedRoute,
    public service: Service,
    protected utils: Utils,
    protected websocket: Websocket,
    private dataService: DataService,
    private router: Router,
  ) {

    router.events.subscribe(async event => {
      if (event instanceof NavigationEnd) {
        const url = event.urlAfterRedirects;
        const topLevelSegment = url.split("/").pop();

        const some = await this.service.getConfig();
        const chips = some.widgets.list.filter(item => item.name == "Evse.Controller.Single" || item.name == "Controller.IO.Heating.Room");
        if (topLevelSegment != "live") {
          this.hideModal();
        } else {
          this.showNewFooter = chips?.length > 0;
        }
      }
    });
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

  async hideModal() {
    this.showNewFooter = false;
    if (this.modal) {
      await this.modal.nativeElement.dismiss(); // Properly dismiss the modal
      this.modal.nativeElement.remove(); // Remove from DOM safely
    }
  }

  public ionViewWillEnter() {
    if (this.widgets?.list) {
      this.showNewFooter = this.widgets?.list.filter(item => item.name == "Evse.Controller.Single" || item.name == "Controller.IO.Heating.Room")?.length > 0;
    }
  }

  public ionViewDidLeave() {
    this.hideModal();
  }


  ionViewWillLeave() {
    this.hideModal();
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
