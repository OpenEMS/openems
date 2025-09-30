// @ts-strict-ignore
import { Component, EventEmitter, Output } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { Utils } from "src/app/shared/shared";
import { TKeyValue } from "src/app/shared/type/utility";
import { environment } from "src/environments";
import { SUM_STATES } from "../shared/sumState";

@Component({
  selector: "oe-filter",
  templateUrl: "./FILTER.COMPONENT.HTML",
  standalone: false,
})
export class FilterComponent {

  @Output() protected setSearchParams: EventEmitter<Map<string, ChosenFilter["value"]>> = new EventEmitter<Map<string, ChosenFilter["value"]>>();
  protected filters: Filter[] = [environment.PRODUCT_TYPES(THIS.TRANSLATE), SUM_STATES(THIS.TRANSLATE)];
  protected searchParams: Map<string, ChosenFilter["value"]> = new Map();

  constructor(private translate: TranslateService) { }

  /**
   * Collects the search params for a {@link GetEdgesRequest}
   *
   * @param event the event
   * @param filter the chosen filter
   */
  public searchOnChange(event, filter: Filter): void {

    const value = EVENT.TARGET.VALUE;

    // If no value provided
    if (!value) {
      return;
    }

    let didFilterChange: boolean = false;

    if (ARRAY.IS_ARRAY(THIS.SEARCH_PARAMS.GET(FILTER.CATEGORY))) {
      didFilterChange = UTILS.COMPARE_ARRAYS_SAFELY(value, THIS.SEARCH_PARAMS.GET(FILTER.CATEGORY) as any[]);
    } else {
      didFilterChange = value == THIS.SEARCH_PARAMS.GET(FILTER.CATEGORY);
    }

    // If Map didn't change
    if (THIS.SEARCH_PARAMS.HAS(FILTER.CATEGORY) && didFilterChange) {
      return;
    }

    let additionalFilter: ChosenFilter;
    if (FILTER.SET_ADDITIONAL_FILTER) {
      additionalFilter = FILTER.SET_ADDITIONAL_FILTER();
    }

    if (value?.length === 0) {
      THIS.SEARCH_PARAMS.DELETE(FILTER.CATEGORY);

      if (additionalFilter) {
        THIS.SEARCH_PARAMS.DELETE(ADDITIONAL_FILTER.KEY);
      }
    } else {
      THIS.SEARCH_PARAMS.SET(FILTER.CATEGORY, value);

      if (additionalFilter) {
        THIS.SEARCH_PARAMS.SET(ADDITIONAL_FILTER.KEY, ADDITIONAL_FILTER.VALUE);
      }
    }

    THIS.SET_SEARCH_PARAMS.EMIT(THIS.SEARCH_PARAMS);
  }
}

export type ChosenFilter = TKeyValue<string | string[] | boolean | null>;

export type Filter = {
  placeholder: string,
  category: string,
  options: FilterOption[],

  // sets additional filter
  setAdditionalFilter?: () => ChosenFilter
};

export type FilterOption = {
  name: string,
  value: string | null
};
