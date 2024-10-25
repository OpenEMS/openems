import { Component, OnDestroy, OnInit, ViewChild } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { RefresherCustomEvent } from "@ionic/angular";
import { NgxMasonryComponent } from "ngx-masonry";
import { Subject } from "rxjs";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { Edge, EdgeConfig, EdgePermission, Service, Utils, Websocket, Widgets } from "src/app/shared/shared";

@Component({
  selector: "live",
  templateUrl: "./live.component.html",
})
export class LiveComponent implements OnInit, OnDestroy {

  @ViewChild(NgxMasonryComponent)
  private masonry: NgxMasonryComponent;

  public edge: Edge | null = null;
  public config: EdgeConfig | null = null;
  public widgets: Widgets | null = null;
  protected isModbusTcpWidgetAllowed: boolean = false;
  protected masonryOptions = {
    itemSelector: ".masonry-item",
    columnWidth: ".masonry-sizer",
    gutter: 0,
    fitWidth: false,
  };
  private stopOnDestroy: Subject<void> = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    public service: Service,
    protected utils: Utils,
    protected websocket: Websocket,
    private dataService: DataService,
  ) { }

  public ngOnInit() {
    this.service.currentEdge.subscribe((edge) => {
      this.edge = edge;
      this.isModbusTcpWidgetAllowed = EdgePermission.isModbusTcpApiWidgetAllowed(edge);
    });
    this.service.getConfig().then(config => {
      this.config = config;
      this.widgets = config.widgets;
      setTimeout(() => {
        this.masonry.reloadItems();
        this.masonry.layout();
      }, 0);
    });
  }

  public ngOnDestroy() {
    this.stopOnDestroy.next();
    this.stopOnDestroy.complete();
  }

  protected handleRefresh: (ev: RefresherCustomEvent) => void = (ev: RefresherCustomEvent) => {
    this.dataService.refresh(ev);
  };
}
