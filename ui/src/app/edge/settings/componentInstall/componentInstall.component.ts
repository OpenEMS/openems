import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Service, Utils, Websocket, EdgeConfig } from '../../../shared/shared';

@Component({
  selector: ComponentInstallComponent.SELECTOR,
  templateUrl: './componentInstall.component.html'
})
export class ComponentInstallComponent implements OnInit, OnDestroy {

  private static readonly SELECTOR = "componentInstall";

  public factory: EdgeConfig.Factory = null;

  constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private websocket: Websocket,
    private service: Service,
  ) {
  }

  ngOnInit() {
    this.service.setCurrentEdge(this.route);
    let factoryId = this.route.snapshot.params["factoryId"];
    this.service.getConfig().then(config => {
      this.factory = config.factories[factoryId];
    });
  }

  ngOnDestroy() {
  }
}