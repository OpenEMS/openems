import { ActivatedRoute } from '@angular/router';
import { CategorizedFactories } from 'src/app/shared/edge/edgeconfig';
import { Component, OnInit } from '@angular/core';
import { Service, Utils, EdgeConfig } from '../../../../shared/shared';
import { TranslateService } from '@ngx-translate/core';

interface MyCategorizedFactories extends CategorizedFactories {
  isClicked?: boolean,
  filteredFactories?: EdgeConfig.Factory[]
}

@Component({
  selector: IndexComponent.SELECTOR,
  templateUrl: './index.component.html'
})
export class IndexComponent implements OnInit {

  private static readonly SELECTOR = "indexComponentInstall";

  public list: MyCategorizedFactories[];

  public showAllFactories = false;

  constructor(
    private route: ActivatedRoute,
    private service: Service,
    private translate: TranslateService
  ) {
  }

  ngOnInit() {
    this.service.setCurrentComponent({ languageKey: 'Edge.Config.Index.addComponents' }, this.route);
    this.service.getConfig().then(config => {
      this.list = config.listAvailableFactories();
      for (let entry of this.list) {
        entry.isClicked = false;
        entry.filteredFactories = entry.factories;
      }
      this.updateFilter("");
    });
  }

  updateFilter(completeFilter: string) {
    // take each space-separated string as an individual and-combined filter
    let filters = completeFilter.split(' ');
    let countFilteredEntries = 0;
    for (let entry of this.list) {
      entry.filteredFactories = entry.factories.filter(entry =>
        // Search for filter strings in Factory-ID, -Name and Description
        Utils.matchAll(filters, [
          entry.id.toLowerCase(),
          entry.name.toLowerCase(),
          entry.description.toLowerCase()
        ])
      );
      countFilteredEntries += entry.filteredFactories.length;
    }
    // If not more than 10 Factories survived filtering -> show all of them immediately
    if (countFilteredEntries > 10) {
      this.showAllFactories = false;
    } else {
      this.showAllFactories = true;
    }
  }
}
