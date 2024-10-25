import { Component, OnDestroy, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { RefresherCustomEvent } from "@ionic/angular";
import MasonrySimple from 'masonry-simple';
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
  private masonry: MasonrySimple | null = null;

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
    this.masonry = new MasonrySimple({
      container: '.masonry-grid',
    });
    this.masonry.init()
  }

  protected handleRefresh: (ev: RefresherCustomEvent) => void = (ev: RefresherCustomEvent) => {
    this.dataService.refresh(ev);
  }
}
