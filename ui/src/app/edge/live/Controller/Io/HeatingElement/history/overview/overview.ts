import { Component, ChangeDetectionStrategy } from "@angular/core";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";

@Component({
    selector: "controller-io-heatingelement-overview",
    templateUrl: "./overview.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class ControllerIoHeatingElementOverviewComponent extends AbstractHistoryChartOverview {}
