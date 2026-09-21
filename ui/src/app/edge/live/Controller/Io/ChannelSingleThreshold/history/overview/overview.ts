import { Component } from "@angular/core";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";
import { ChartComponentsModule } from "src/app/shared/components/chart/chart.module";
import { PickdateComponentModule } from "src/app/shared/components/pickdate/pickdate.module";

@Component({
    selector: "oe-controller-io-single-threshold-overview",
    templateUrl: "./overview.html",
    standalone: true,
    imports: [CommonUiModule, ChartComponentsModule, PickdateComponentModule],
})
export class ControllerIoChannelSingleThresholdOverviewComponent extends AbstractHistoryChartOverview {}
