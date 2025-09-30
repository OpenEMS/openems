// @ts-strict-ignore
import { Component, OnInit } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { CategorizedComponents } from "src/app/shared/components/edge/edgeconfig";
import { EdgeConfig, Service, Utils } from "../../../../shared/shared";

interface MyCategorizedComponents extends CategorizedComponents {
  isNatureClicked?: boolean,
  filteredComponents?: EDGE_CONFIG.COMPONENT[],
}

@Component({
  selector: INDEX_COMPONENT.SELECTOR,
  templateUrl: "./INDEX.COMPONENT.HTML",
  standalone: false,
})
export class IndexComponent implements OnInit {

  private static readonly SELECTOR = "indexComponentUpdate";

  public config: EdgeConfig | null = null;
  public list: MyCategorizedComponents[];

  public showAllEntries = false;

  constructor(
    private service: Service,
    private translate: TranslateService,
  ) {
  }

  public ngOnInit() {
    THIS.SERVICE.GET_CONFIG().then(config => {
      THIS.CONFIG = config;
      const categorizedComponentIds: string[] = [];
      THIS.LIST = CONFIG.LIST_ACTIVE_COMPONENTS(categorizedComponentIds, THIS.TRANSLATE);
      for (const entry of THIS.LIST) {
        ENTRY.IS_NATURE_CLICKED = false;
        ENTRY.FILTERED_COMPONENTS = ENTRY.COMPONENTS;
      }
      THIS.UPDATE_FILTER("");
    });
  }

  updateFilter(completeFilter: string) {
    // take each space-separated string as an individual and-combined filter
    const filters = COMPLETE_FILTER.TO_LOWER_CASE().split(" ");
    let countFilteredEntries = 0;
    for (const entry of THIS.LIST) {
      ENTRY.FILTERED_COMPONENTS = ENTRY.COMPONENTS.FILTER(entry =>
        // Search for filter strings in Component-ID, -Alias and Factory-ID
        UTILS.MATCH_ALL(filters, [
          ENTRY.ID.TO_LOWER_CASE(),
          ENTRY.ALIAS.TO_LOWER_CASE(),
          ENTRY.FACTORY_ID,
        ]),
      );
      countFilteredEntries += ENTRY.FILTERED_COMPONENTS.LENGTH;
    }
    // If not more than 5 Entries survived filtering -> show all of them immediately
    if (countFilteredEntries > 5) {
      THIS.SHOW_ALL_ENTRIES = false;
    } else {
      THIS.SHOW_ALL_ENTRIES = true;
    }
  }
}
