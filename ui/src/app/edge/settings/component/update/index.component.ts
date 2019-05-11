import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Service, Utils, Websocket, EdgeConfig } from '../../../../shared/shared';
import { IGNORE_NATURES } from '../shared/shared';
import { TranslateService } from '@ngx-translate/core';

interface ListEntry {
  readonly component: EdgeConfig.Component
  readonly factory: EdgeConfig.Factory
};

@Component({
  selector: IndexComponent.SELECTOR,
  templateUrl: './index.component.html'
})
export class IndexComponent implements OnInit {

  private static readonly SELECTOR = "indexComponentUpdate";

  public list: {
    readonly nature: EdgeConfig.Nature,
    isNatureClicked: Boolean,
    readonly allEntries: ListEntry[],
    filteredEntries: ListEntry[]
  }[] = [];
  public showAllEntries = false;

  constructor(
    private route: ActivatedRoute,
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
              isFiltered: true,
              factory: factory
            })
          }

          this.list.push({
            nature: nature,
            isNatureClicked: false,
            allEntries: entries,
            filteredEntries: entries
          });
        }
      }
      this.updateFilter("");
    });
  }

  updateFilter(completeFilter: string) {
    // take each space-separated string as an individual and-combined filter
    let filters = completeFilter.split(' ');
    let countFilteredEntries = 0;
    for (let entry of this.list) {
      entry.filteredEntries = entry.allEntries.filter(entry =>
        // Search for filter strings in Component-ID, -Alias and Factory-ID
        Utils.matchAll(filters, [
          entry.component.id.toLowerCase(),
          entry.component.alias.toLowerCase(),
          entry.factory.id.toLowerCase()])
      );
      countFilteredEntries += entry.filteredEntries.length;
    }
    // If not more than 5 Entries survived filtering -> show all of them immediately
    if (countFilteredEntries > 5) {
      this.showAllEntries = false;
    } else {
      this.showAllEntries = true;
    }
  }
}