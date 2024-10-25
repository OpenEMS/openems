import { Component, HostListener, OnDestroy, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { RefresherCustomEvent } from "@ionic/angular";
import Masonry from "masonry-layout";
import { Subject } from "rxjs";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { Edge, EdgeConfig, EdgePermission, Service, Utils, Websocket, Widgets } from "src/app/shared/shared";

@Component({
  selector: "live",
  templateUrl: "./live.component.html",
})
export class LiveComponent implements OnInit, OnDestroy {

  public edge: Edge | null = null;
  public config: EdgeConfig | null = null;
  public widgets: Widgets | null = null;
  protected isModbusTcpWidgetAllowed: boolean = false;
  private stopOnDestroy: Subject<void> = new Subject<void>();
  private masonry: Masonry | null = null;

  constructor(
    private route: ActivatedRoute,
    public service: Service,
    protected utils: Utils,
    protected websocket: Websocket,
    private dataService: DataService,
  ) { }

  @HostListener("window:resize")
  onWindowResize() {
    if (this.masonry) {
      this.masonry.layout()
    }
  }

  public ngOnInit() {
    this.service.currentEdge.subscribe((edge) => {
      this.edge = edge;
      this.isModbusTcpWidgetAllowed = EdgePermission.isModbusTcpApiWidgetAllowed(edge);
    });
    this.service.getConfig().then(config => {
      this.config = config;
      this.widgets = config.widgets;
      setTimeout(this.initMasonry, 0);
    });
  }

  public ngOnDestroy() {
    if (this.masonry) {
      this.masonry.destroy();
    }
    this.stopOnDestroy.next();
    this.stopOnDestroy.complete();
  }

  private initMasonry() {
    const masonryGrid = document.querySelector(".masonry-grid");
    if (!masonryGrid) {
      console.error("Masonry grid not found!")
      return;
    }
    this.masonry = new Masonry(masonryGrid, {
      itemSelector: ".masonry-item",
      columnWidth: ".masonry-sizer",
      gutter: 0,
      fitWidth: false,
    });
    setTimeout(() => this.masonry.layout(), 200);
  }

  protected handleRefresh: (ev: RefresherCustomEvent) => void = (ev: RefresherCustomEvent) => {
    this.dataService.refresh(ev);
    this.masonry.layout();
  }
}
