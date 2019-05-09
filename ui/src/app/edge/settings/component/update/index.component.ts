import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Service, Utils, Websocket, EdgeConfig } from '../../../../shared/shared';
import { IGNORE_NATURES } from '../shared/shared';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: IndexComponent.SELECTOR,
  templateUrl: './index.component.html'
})
export class IndexComponent implements OnInit, OnDestroy {

  private static readonly SELECTOR = "indexComponentUpdate";

  public list: {
    readonly nature: EdgeConfig.Nature,
    readonly entries: {
      readonly component: EdgeConfig.Component
      readonly factory: EdgeConfig.Factory
    }[]
  }[] = [];

  constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private websocket: Websocket,
    private service: Service,
    private translate: TranslateService
  ) {
  }

  ngOnInit() {
    this.service.setCurrentComponent(this.translate.instant('Edge.Config.Index.AdjustComponents'), this.route);
    this.service.getConfig().then(config => {
      for (let natureId in config.natures) {
        if (IGNORE_NATURES.includes(natureId)) {
          continue;
        }

        let nature = config.natures[natureId];
        let components = config.getComponentsImplementingNature(natureId);

        if (components.length > 0) {
          let entries = [];

          for (let component of components) {
            let factory = config.factories[component.factoryId];
            entries.push({
              component: component,
              factory: factory
            })
          }

          this.list.push({
            nature: nature,
            entries: entries
          });
        }
      }
    });
  }

  ngOnDestroy() {
  }
}