import { Component, ChangeDetectionStrategy } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { AbstractHistoryChartOverview } from "src/app/shared/components/chart/abstractHistoryChartOverview";
import { ChartComponentsModule } from "src/app/shared/components/chart/chart.module";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-error.module";
import { PickdateComponentModule } from "src/app/shared/components/pickdate/pickdate.module";
import { ControllerModbusTcpApiChartComponent } from "../chart/chart";

@Component({
    templateUrl: "./overview.html",
    standalone: true,
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [
        ReactiveFormsModule,
        IonicModule,
        TranslateModule,
        ChartComponentsModule,
        PickdateComponentModule,
        HistoryDataErrorModule,
        ControllerModbusTcpApiChartComponent,
    ],
})
export class ControllerModbusTcpApiOverviewComponent extends AbstractHistoryChartOverview {}
