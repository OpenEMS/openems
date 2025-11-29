import { Component, EventEmitter, Input, Output } from "@angular/core";
import { v4 as uuidv4 } from "uuid";

import { CommonUiModule } from "src/app/shared/common-ui.module";
import { Service } from "src/app/shared/shared";
import { TKeyValue } from "src/app/shared/type/utility";
import { ArrayUtils } from "src/app/shared/utils/array/array.utils";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { JsonUtils } from "src/app/shared/utils/json/json-utils";
import { NumberUtils } from "src/app/shared/utils/number/number-utils";
import { StringUtils } from "src/app/shared/utils/string/string.utils";

@Component({
    selector: "oe-filter",
    templateUrl: "./filter.component.html",
    standalone: true,
    imports: [
        CommonUiModule,
    ],
})
export class FilterComponent {

    @Output() public setSearchParams: EventEmitter<Map<string, ChosenFilter["value"]>> = new EventEmitter<Map<string, ChosenFilter["value"]>>();
    protected searchParams: Map<string, ChosenFilter["value"]> = new Map();
    protected defaultFilterValues: FilterCategory = {};
    protected allFilters: (Filter<string | null> | SortOrderFilter | null)[] = [];
    protected columnSize: number = 0;

    constructor(public service: Service) { }

    @Input() public set filters(_filters: (Filter<string> | SortOrderFilter)[]) {
        this.allFilters = _filters
            .filter(f => f != null)
            .map(FilterComponent.ADD_UNIQUE_ID_TO_FILTER_OPTION);

        if (this.allFilters == null || this.allFilters.length === 0) {
            return;
        }
        this.columnSize = Math.max(NumberUtils.divideSafely(12, this.allFilters.length) ?? 0, 4);

        this.defaultFilterValues = FilterComponent.getDefaultFilterValues(this.allFilters);

        for (const [category, ids] of Object.entries(this.defaultFilterValues)) {
            if (category === null || ids === null || ids === undefined) {
                return;

            }
            const validIds = ids.reduce(ArrayUtils.ReducerFunctions.STRINGIFY_SAFELY, []);
            const filter = this.allFilters.find(el => el?.options.find(e => StringUtils.isInArr(e.option?.id ?? null, validIds)));

            if (filter == null) {
                continue;
            }
            const defaultValue = FilterComponent.getFilterValues(validIds, filter).flat();
            this.searchParams.set(category, defaultValue);
        }
        this.setSearchParams.emit(this.searchParams);
    }

    /**
     * Gets id of a filter option by its value.
     *
     * @param filter the filter
     * @param value the value
     * @returns a array with ids for this filter
     */
    public static getIdByValue<T>(filter: Filter<string | null> | SortOrderFilter | null, value: T): string[] {
        AssertionUtils.assertIsDefined(filter);
        return filter.options.reduce((arr, opt) => {
            if (typeof opt.option.value === "string" && Array.isArray(value) && StringUtils.isInArr(opt.option.value, value)) {
                AssertionUtils.assertIsDefined(opt.option.id);
                arr.push(opt.option.id);
            }
            return arr;
        }, [] as string[]);
    }

    /**
     * Gets default filter values.
     *
     * @param filters the filters
     * @returns a list of either id of default values or null
     */
    private static getDefaultFilterValues(filters: (Filter | SortOrderFilter | null)[]): FilterCategory {

        const persistedSelection = FilterComponent.getPersistedSelection(filters);
        function getPersistedSelectionOptionIds(filter: (Filter | SortOrderFilter | null)) {
            AssertionUtils.assertIsDefined(filter);
            return filter.category in persistedSelection && persistedSelection[filter.category]?.length > 0 ? persistedSelection[filter.category] : filter?.options?.filter(el => el.option?.default).map(el => el.option.id);
        }

        return filters.reduce((obj, filter) => {
            if (filter == null) {
                return obj;
            }
            const optionIds = getPersistedSelectionOptionIds(filter);
            obj[filter.category] = optionIds;
            return obj;
        }, {} as FilterCategory);
    }

    /**
     * Adds a unique id to each filter option.
     *
     * @description unique needed for IonSelects value property mapping for objects
     *
     * @param filter the filter
     * @returns the filter with unique id
     */
    private static ADD_UNIQUE_ID_TO_FILTER_OPTION(filter: Filter | SortOrderFilter | null): Filter<string | null> | SortOrderFilter | null {
        if (filter == null) {
            return filter;
        }
        filter.options.map(el => {
            el.option.id = uuidv4();
            return el;
        });

        return filter;
    }

    /**
     * Gets the filter values.
     *
     * @param ids the unique ids
     * @param filter the filter
     * @returns the filter values
     */
    private static getFilterValues<T = string>(ids: string[], filter: (Filter<string | null> | SortOrderFilter)): T[] {
        return filter.options
            .reduce((arr: T[], item) => {
                if (item?.option?.id == null) {
                    return arr;
                }

                const matchesOptionId = StringUtils.isInArr(item?.option?.id, ids);
                if (matchesOptionId === false) {
                    return arr;
                }

                arr.push(item.option.value as T);
                return arr;
            }, []);
    }

    private static getPersistedSelection(filters: (Filter | SortOrderFilter | null)[]) {
        AssertionUtils.assertIsDefined(filters);
        return filters.reduce(((obj, filter) => {
            AssertionUtils.assertIsDefined(filter);
            const persistedFilterValue: (Filter<string> | SortOrderFilter)["options"][number]["option"]["value"] | null = JsonUtils.safeJsonParse(localStorage.getItem(filter?.category ?? "") ?? "");
            const id = this.getIdByValue(filter, persistedFilterValue);
            if (id == null) {
                return obj;
            }

            obj[filter.category] = id;
            return obj;
        }), {} as FilterCategory);
    }

    /**
     * Collects the search params for a {@link GetEdgesRequest}
     *
     * @param event the event
     * @param filter the chosen filter
     */
    public searchOnChange(event: Event, filter: SortOrderFilter | Filter<string>): void {
        const selectElement = event.target as HTMLIonSelectElement;
        const input = selectElement.value;

        // If no value provided
        if (input == null) {
            return;
        }

        const ids: string[] = Array.isArray(input) ? input : [input];
        const values = FilterComponent.getFilterValues(ids, filter).flat();

        let additionalFilter: ChosenFilter | null = null;
        if (filter.setAdditionalFilter) {
            additionalFilter = filter.setAdditionalFilter();
        }

        if (values?.length === 0) {
            this.searchParams.delete(filter.category);

            if (additionalFilter) {
                this.searchParams.delete(additionalFilter.key);
            }
        } else {
            this.searchParams.set(filter.category, values);
        }

        if (additionalFilter) {
            this.searchParams.set(additionalFilter.key, additionalFilter.value);
        }

        this.persistSelection(filter);
        this.setSearchParams.emit(this.searchParams);
    }

    /**
     * Persist the current selection
     *
     * @param filter the filter
     */
    private persistSelection(filter: Filter<string> | SortOrderFilter) {
        localStorage.setItem(filter.category, JSON.stringify(this.searchParams.get(filter.category) ?? []));
    }
}

export type ChosenFilter = TKeyValue<string | string[] | boolean | null>;

export type Filter<T = string | null> = {
    multiple: HTMLIonSelectElement["multiple"],
    placeholder: string,
    category: string,
    options: FilterOption<T>[],

    // sets additional filter
    setAdditionalFilter?: () => ChosenFilter
};

export type SortOrderFilter = Filter<{
    field: string,
    sortOrder: "ASC" | "DESC",
}[]>;

export type FilterOption<T> = {
    name: string,
    option: {
        value: T,
        id?: string,
        default?: boolean,
    }
};
type FilterCategoryOptionId = Filter["options"][number]["option"]["id"] | null;
type FilterCategory = { [category: Filter["category"]]: (FilterCategoryOptionId | null)[] };
