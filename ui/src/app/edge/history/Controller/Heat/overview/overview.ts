import { Component } from "@angular/core";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";

@Component({
    selector: "heat-overview",
    templateUrl: "./OVERVIEW.HTML",
    standalone: false,
})
export class OverviewComponent extends AbstractHistoryChartOverview { }
