import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Service, Utils, Websocket, EdgeConfig } from '../../../../shared/shared';
import { IGNORE_NATURES } from '../shared/shared';
import { TranslateService } from '@ngx-translate/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';

@Component({
  selector: IndexComponent.SELECTOR,
  templateUrl: './index.component.html'
})
export class IndexComponent implements OnInit {

  private static readonly SELECTOR = "indexComponentInstall";

  public list: {
    readonly nature: EdgeConfig.Nature,
    isNatureClicked: Boolean,
    readonly allFactories: EdgeConfig.Factory[]
    filteredFactories: EdgeConfig.Factory[]
  }[] = [];
  public showAllFactories = false;

  constructor(
    private route: ActivatedRoute,
    private service: Service,
    private translate: TranslateService
  ) {
  }

  ngOnInit() {
    this.service.setCurrentComponent(this.translate.instant('Edge.Config.Index.AddComponents'), this.route);
    this.service.getConfig().then(config => {
      for (let natureId in config.natures) {
        if (IGNORE_NATURES.includes(natureId)) {
          continue;
        }

        let nature = config.natures[natureId];
        let factories = [];
        for (let factoryId of nature.factoryIds) {
          factories.push(config.factories[factoryId]);
        }
        this.list.push({
          nature: nature,
          isNatureClicked: false,
          allFactories: factories,
          filteredFactories: factories
        });
      }
      this.updateFilter("");
    });
  }

  updateFilter(completeFilter: string) {
    // take each space-separated string as an individual and-combined filter
    let filters = completeFilter.split(' ');
    let countFilteredFactories = 0;
    for (let entry of this.list) {
      entry.filteredFactories = entry.allFactories.filter(factory =>
        // Search for filter strings
        Utils.matchAll(filters, [
          factory.id.toLowerCase(),
          factory.name.toLowerCase(),
          factory.description.toLowerCase()]),
      );
      countFilteredFactories += entry.filteredFactories.length;
    }
    // If not more than 10 Factories survived filtering -> show all of them immediately
    if (countFilteredFactories > 10) {
      this.showAllFactories = false;
    } else {
      this.showAllFactories = true;
    }
  }
}