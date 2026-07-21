import { CommonModule } from "@angular/common";
import { Component, ChangeDetectionStrategy } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { BaseChartDirective } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";
import { calculateResolution, ChronoUnit } from "src/app/edge/history/shared";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartComponentsModule } from "src/app/shared/components/chart/chart.module";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-error.module";
import { EdgeConfig } from "src/app/shared/shared";
import { HistoryUtils } from "src/app/shared/utils/utils";
import { PeakShavingChartDataBuilder } from "./peak-shaving-chart-data";

@Component({
    selector: "oe-controller-peakshaving-timeslot-chart",
    templateUrl: "../../../../../shared/components/chart/abstracthistorychart.html",
    standalone: true,
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [
        BaseChartDirective,
        ReactiveFormsModule,
        CommonModule,
        IonicModule,
        TranslateModule,
        ChartComponentsModule,
        HistoryDataErrorModule,
        NgxSpinnerModule,
    ],
})
export abstract class SharedPeakShavingChartComponent extends AbstractHistoryChart {
    public static getChartData(
        config: EdgeConfig,
        component: EdgeConfig.Component,
        translate: TranslateService,
    ): HistoryUtils.ChartData {
        return PeakShavingChartDataBuilder.build(config, component, translate, {
            activePowerMode: "single",
        });
    }

    protected override getChartData(): HistoryUtils.ChartData {
        return SharedPeakShavingChartComponent.getChartData(this.config, this.component, this.translate);
    }

    protected override loadChart(): Promise<void> {
        const unit: ChronoUnit.Type = calculateResolution(
            this.service,
            this.service.historyPeriod.value.from,
            this.service.historyPeriod.value.to,
        ).resolution.unit;
        return this.loadLineChart(unit);
    }
}
