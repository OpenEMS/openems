import { Component } from "@angular/core";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";

@Component({
    selector: "heat-overview",
    templateUrl: "./overview.html",
    standalone: false,
})
export class ControllerHeatOverviewComponent extends AbstractHistoryChartOverview { }
