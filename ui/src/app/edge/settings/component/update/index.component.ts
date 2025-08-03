// @ts-strict-ignore
import { Component, OnInit, inject } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { CategorizedComponents } from "src/app/shared/components/edge/edgeconfig";
import { EdgeConfig, Service, Utils } from "../../../../shared/shared";

interface MyCategorizedComponents extends CategorizedComponents {
  isNatureClicked?: boolean,
  filteredComponents?: EdgeConfig.Component[],
}

@Component({
  selector: IndexComponent.SELECTOR,
  templateUrl: "./index.component.html",
  standalone: false,
})
export class IndexComponent implements OnInit {
  private service = inject(Service);
  private translate = inject(TranslateService);


  private static readonly SELECTOR = "indexComponentUpdate";

  public config: EdgeConfig | null = null;
  public list: MyCategorizedComponents[];

  public showAllEntries = false;

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {
  }

  public ngOnInit() {
    this.service.getConfig().then(config => {
      this.config = config;
      const categorizedComponentIds: string[] = [];
      this.list = config.listActiveComponents(categorizedComponentIds, this.translate);
      for (const entry of this.list) {
        entry.isNatureClicked = false;
        entry.filteredComponents = entry.components;
      }
      this.updateFilter("");
    });
  }

  updateFilter(completeFilter: string) {
    // take each space-separated string as an individual and-combined filter
    const filters = completeFilter.toLowerCase().split(" ");
    let countFilteredEntries = 0;
    for (const entry of this.list) {
      entry.filteredComponents = entry.components.filter(entry =>
        // Search for filter strings in Component-ID, -Alias and Factory-ID
        Utils.matchAll(filters, [
          entry.id.toLowerCase(),
          entry.alias.toLowerCase(),
          entry.factoryId,
        ]),
      );
      countFilteredEntries += entry.filteredComponents.length;
    }
    // If not more than 5 Entries survived filtering -> show all of them immediately
    if (countFilteredEntries > 5) {
      this.showAllEntries = false;
    } else {
      this.showAllEntries = true;
    }
  }
}
