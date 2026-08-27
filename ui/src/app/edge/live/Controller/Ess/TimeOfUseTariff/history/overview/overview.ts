import { Component, ChangeDetectionStrategy } from "@angular/core";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";

@Component({
    templateUrl: "./overview.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class ControllerEssTimeOfUseTariffOverviewComponent extends AbstractHistoryChartOverview {}
