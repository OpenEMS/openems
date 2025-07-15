// @ts-strict-ignore
import { CommonModule } from "@angular/common";
import { Component } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { BaseChartDirective } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";
import { calculateResolution, ChronoUnit } from "src/app/edge/history/shared";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartComponentsModule } from "src/app/shared/components/chart/chart.module";
import { HistoryDataErrorModule } from "src/app/shared/components/history-data-error/history-data-error.module";
import { Phase } from "src/app/shared/components/shared/phase";
import { ChannelAddress, ChartConstants, EdgeConfig, Utils } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { ChartAxis, HistoryUtils, YAxisType } from "src/app/shared/utils/utils";

@Component({
  selector: "oe-controller-peakshaving-asymmetric-chart",
  templateUrl: "../../../../../../shared/components/chart/abstracthistorychart.html",
  standalone: true,
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
export class ChartComponent extends AbstractHistoryChart {

  public static getChartData(config: EdgeConfig, component: EdgeConfig.Component, translate: TranslateService): HistoryUtils.ChartData {

    AssertionUtils.assertIsDefined(component);
    const meterId = config.getPropertyFromComponent<string>(component, "meter.id");

    const input: HistoryUtils.InputChannel[] = [
      ...(Phase.THREE_PHASE.map(phase => ({
        name: "ActivePower" + phase,
        powerChannel: new ChannelAddress(meterId, "ActivePower" + phase),
        converter: HistoryUtils.ValueConverter.NEGATIVE_AS_ZERO,
      }))),
      {
        name: "EssSoc",
        powerChannel: new ChannelAddress("_sum", "EssSoc"),
      },
      {
        name: "Charge",
        powerChannel: new ChannelAddress("_sum", "EssActivePower"),
        converter: HistoryUtils.ValueConverter.POSITIVE_AS_ZERO_AND_INVERT_NEGATIVE,
      },
      {
        name: "Discharge",
        powerChannel: new ChannelAddress("_sum", "EssActivePower"),
        converter: HistoryUtils.ValueConverter.NON_NULL_OR_NEGATIVE,

      },
      {
        name: "CHARGE_UNDER",
        powerChannel: new ChannelAddress(component.id, "_PropertyRechargePower"),
      },
      {
        name: "DISCHARGE_OVER",
        powerChannel: new ChannelAddress(component.id, "_PropertyPeakShavingPower"),
      },
    ];

    return {
      input: input,
      output: (data: HistoryUtils.ChannelData) => {
        const output: HistoryUtils.DisplayValue[] = [
          ...(Phase.THREE_PHASE.map((phase, i) => ({
            name: "Phase " + phase,
            color: ChartConstants.Colors.DEFAULT_PHASES_COLORS[i],
            converter: () => data["ActivePower" + phase],
          }))),
          {
            name: translate.instant("Edge.Index.Widgets.Peakshaving.rechargePower"),
            color: ChartConstants.Colors.GREEN,
            converter: () => data["CHARGE_UNDER"],
            hideShadow: true,
            borderDash: [3, 3],
          },
          {
            name: translate.instant("Edge.Index.Widgets.Peakshaving.peakshavingPower"),
            color: ChartConstants.Colors.RED,
            converter: () => data["DISCHARGE_OVER"],
            hideShadow: true,
            borderDash: [3, 3],
          },
          {
            name: translate.instant("General.CHARGE"),
            color: ChartConstants.Colors.GREEN,
            converter: () => data["Charge"],
          },
          {
            name: translate.instant("General.DISCHARGE"),
            color: ChartConstants.Colors.RED,
            converter: () => data["Discharge"],
          },
          {
            name: translate.instant("General.soc"),
            color: ChartConstants.Colors.GREY,
            converter: () => data["EssSoc"]?.map(value => Utils.multiplySafely(value, 1000)),
            yAxisId: ChartAxis.RIGHT,
            hideShadow: true,
            borderDash: [10, 10],
          },
        ];
        return output;
      },
      tooltip: {
        formatNumber: ChartConstants.NumberFormat.ZERO_TO_TWO,
      },
      yAxes: [{
        unit: YAxisType.ENERGY,
        position: "left",
        yAxisId: ChartAxis.LEFT,
      },

      {
        unit: YAxisType.PERCENTAGE,
        position: "right",
        yAxisId: ChartAxis.RIGHT,
      },
      ],
    };
  }

  protected override getChartData(): HistoryUtils.ChartData {
    return ChartComponent.getChartData(this.config, this.component, this.translate);
  }

  protected override loadChart(): Promise<void> {
    const unit: ChronoUnit.Type = calculateResolution(this.service, this.service.historyPeriod.value.from, this.service.historyPeriod.value.to).resolution.unit;
    this.loadLineChart(unit);
    return;
  }
}
