import { Component, ChangeDetectionStrategy } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";
import { ChartComponentsModule } from "src/app/shared/components/chart/chart.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-error.module";
import { PickdateComponentModule } from "src/app/shared/components/pickdate/pickdate.module";
import { LocaleProvider } from "src/app/shared/provider/locale-provider";
import { PeakShavingSymmetricChartComponent } from "../chart/chart";

@Component({
    templateUrl: "./new-navigation.html",
    standalone: true,
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [
        CommonUiModule,
        LocaleProvider,
        ReactiveFormsModule,
        PeakShavingSymmetricChartComponent,
        ChartComponentsModule,
        PickdateComponentModule,
        HistoryDataErrorModule,
        ComponentsBaseModule,
    ],
})
export class ControllerPeakShavingSymmetricHistoryComponent extends AbstractHistoryChartOverview {}
