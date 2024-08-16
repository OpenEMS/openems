import { Component } from "@angular/core";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";

@Component({
    selector: 'overview',
    templateUrl: './overview.html',
})
export class OverviewComponent extends AbstractHistoryChartOverview { }
