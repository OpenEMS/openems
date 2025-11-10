import { TranslateService } from "@ngx-translate/core";
import { environment } from "src/environments";
import { SortOrderFilter } from "../filter/filter.component";

export const ORDER_STATES = (translate: TranslateService): SortOrderFilter => ({
    multiple: false,
    placeholder: translate.instant("ORDER.SORTING"),
    category: "orderState",
    options: [
        {
            name: translate.instant("ORDER.ASCENDING", { edgeShortName: environment.edgeShortName }),
            option: {
                value: [{
                    field: "id",
                    sortOrder: "ASC",
                }],
                default: true,
            },
        },
        {
            name: translate.instant("ORDER.DESCENDING", { edgeShortName: environment.edgeShortName }),
            option: {
                value: [{
                    field: "id",
                    sortOrder: "DESC",
                }],
            },
        },
        {
            name: translate.instant("ORDER.A_TO_Z", { edgeShortName: environment.edgeShortName }),
            option: {
                value: [{
                    field: "comment",
                    sortOrder: "ASC",
                }],
            },
        },
        {
            name: translate.instant("ORDER.Z_TO_A", { edgeShortName: environment.edgeShortName }),
            option: {
                value: [{
                    field: "comment",
                    sortOrder: "DESC",
                }],
            },
        },
        {
            name: translate.instant("ORDER.OK_TO_FAULT", { edgeShortName: environment.edgeShortName }),
            option: {
                value: [{
                    field: "sumState",
                    sortOrder: "ASC",
                }],
            },
        },
        {
            name: translate.instant("ORDER.FAULT_TO_OK"),
            option: {
                value: [{
                    field: "sumState",
                    sortOrder: "DESC",
                }],
            },
        },
    ],
});
