import { Component } from "@angular/core";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";

@Component({
    selector: "oe-common-autarchy-overview",
    templateUrl: "./overview.html",
    standalone: false,
})
export class CommonAutarchyOverviewComponent extends AbstractHistoryChartOverview { }
