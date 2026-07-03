import { Component } from "@angular/core";

import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { BaseChartDirective } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";
import { ChartComponentsModule } from "src/app/shared/components/chart/chart.module";
import { ScheduleChartComponent } from "src/app/shared/components/chart/schedule-chart/schedule-chart";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-error.module";
import { ChartConstants } from "src/app/shared/shared";

@Component({
    selector: "oe-production-chart",
    templateUrl: "../../../../../history/abstracthistorychart.html",
    standalone: true,
    imports: [
        BaseChartDirective,
        ReactiveFormsModule,
        IonicModule,
        TranslateModule,
        ChartComponentsModule,
        HistoryDataErrorModule,
        NgxSpinnerModule,
    ],
})
export class ProductionChartComponent extends ScheduleChartComponent {

    protected override buildDatasets(): ScheduleChartComponent.Dataset[] {
        const data = this.data.summarizeDataForChannel("ProductionActivePower");

        return [{
            color: ChartConstants.Colors.BLUE, data: data.history,
        }, {
            color: ChartConstants.Colors.BLUE, data: data.prediction, borderDash: [5, 5],
        }];
    }
}
