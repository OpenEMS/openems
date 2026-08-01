// @ts-strict-ignore
import { Component, ChangeDetectionStrategy } from "@angular/core";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";

@Component({
    selector: "gridoptimizedcharge-chart-overview",
    templateUrl: "./overview.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class ControllerEssGridOptimizedChargeOverviewComponent extends AbstractHistoryChartOverview {}
