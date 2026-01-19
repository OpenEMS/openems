import { Component } from "@angular/core";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";

@Component({
    templateUrl: "./overview.html",
    standalone: false,
})
export class OverviewComponent extends AbstractHistoryChartOverview { }
