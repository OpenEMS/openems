// @ts-strict-ignore
import { Component, EventEmitter, Output } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { TKeyValue } from "src/app/shared/service/defaulttypes";
import { environment } from "src/environments";
import { SUM_STATES } from "../shared/sumState";
import { Utils } from "src/app/shared/shared";

@Component({
  selector: 'oe-filter',
  templateUrl: './filter.component.html',
})
export class FilterComponent {

  @Output() protected setSearchParams: EventEmitter<Map<string, ChosenFilter['value']>> = new EventEmitter<Map<string, ChosenFilter['value']>>();
  protected filters: Filter[] = [environment.PRODUCT_TYPES(this.translate), SUM_STATES(this.translate)];
  protected searchParams: Map<string, ChosenFilter['value']> = new Map();

  constructor(private translate: TranslateService) { }

  /**
   * Collects the search params for a {@link GetEdgesRequest}
   *
   * @param event the event
   * @param filter the chosen filter
   */
  public searchOnChange(event, filter: Filter): void {

    const value = event.target.value;

    // If no value provided
    if (!value) {
      return;
    }

    let didFilterChange: boolean = false;

    if (Array.isArray(this.searchParams.get(filter.category))) {
      didFilterChange = Utils.compareArraysSafely(value, this.searchParams.get(filter.category) as any[]);
    } else {
      didFilterChange = value == this.searchParams.get(filter.category);
    }

    // If Map didn't change
    if (this.searchParams.has(filter.category) && didFilterChange) {
      return;
    }

    let additionalFilter: ChosenFilter;
    if (filter.setAdditionalFilter) {
      additionalFilter = filter.setAdditionalFilter();
    }

    if (value?.length === 0) {
      this.searchParams.delete(filter.category);

      if (additionalFilter) {
        this.searchParams.delete(additionalFilter.key);
      }
    } else {
      this.searchParams.set(filter.category, value);

      if (additionalFilter) {
        this.searchParams.set(additionalFilter.key, additionalFilter.value);
      }
    }

    this.setSearchParams.emit(this.searchParams);
  }
}

export type ChosenFilter = TKeyValue<string | string[] | boolean | null>

export type Filter = {
  placeholder: string,
  category: string,
  options: FilterOption[],

  // sets additional filter
  setAdditionalFilter?: () => ChosenFilter
}

export type FilterOption = {
  name: string,
  value: string | null
}
