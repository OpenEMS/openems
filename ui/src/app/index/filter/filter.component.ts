import { Component, EventEmitter, OnInit, Output } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { v4 as uuidv4 } from "uuid";
import { Service } from "src/app/shared/shared";
import { TKeyValue } from "src/app/shared/type/utility";
import { StringUtils } from "src/app/shared/utils/string/string.utils";
import { environment } from "src/environments";
import { SUM_STATES } from "../shared/sumState";

@Component({
    selector: "oe-filter",
    templateUrl: "./filter.component.html",
    standalone: false,
})
export class FilterComponent implements OnInit {

    @Output() protected setSearchParams: EventEmitter<Map<string, ChosenFilter["value"]>> = new EventEmitter<Map<string, ChosenFilter["value"]>>();
    protected filters: (Filter<string | null> | SortOrderFilter | null)[] = [
        environment.PRODUCT_TYPES(this.translate),
        SUM_STATES(this.translate),
    ].map(FilterComponent.ADD_UNIQUE_ID_TO_FILTER_OPTION);
    protected searchParams: Map<string, ChosenFilter["value"]> = new Map();
    protected defaultFilterValues: FilterCategory = {};

    constructor(private translate: TranslateService, public service: Service) { }

    /**
     * Gets default filter values.
     *
     * @param filters the filters
     * @returns a list of either id of default values or null
     */
    private static getDefaultFilterValues(filters: (Filter | SortOrderFilter | null)[]): FilterCategory {
        return filters.reduce((obj, item) => {
            if (item == null) {
                return obj;
            }
            const optionId = item?.options?.find(el => el.option?.default)?.option?.id ?? null;

            obj[item.category] = optionId;
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
    private static getFilterValues<T = string>(ids: string[], filter: Filter): T[] {
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

    public ngOnInit() {
        this.defaultFilterValues = FilterComponent.getDefaultFilterValues(this.filters);
    }

    /**
     * Collects the search params for a {@link GetEdgesRequest}
     *
     * @param event the event
     * @param filter the chosen filter
     */
    public searchOnChange(event: Event, filter: Filter): void {
        const selectElement = event.target as HTMLIonSelectElement;
        const input = selectElement.value;

        // If no value provided
        if (input == null || input?.length === 0) {
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

        this.setSearchParams.emit(this.searchParams);
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
        id?: string, // Set automatically
        default?: boolean,
    }
};
type FilterCategoryOptionId = Filter["options"][number]["option"]["id"] | null;
type FilterCategory = { [category: Filter["category"]]: FilterCategoryOptionId | null };
