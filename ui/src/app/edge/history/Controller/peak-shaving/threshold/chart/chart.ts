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
import { ChannelAddress, ChartConstants, EdgeConfig, Utils } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { ChartAxis, HistoryUtils, YAxisType } from "src/app/shared/utils/utils";


@Component({
  selector: "oe-controller-peakshaving-threshold-chart",
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
    //console.log("[ThresholdPeakshavingChartComponent] non-empty data", this);
    const meterId = config.getPropertyFromComponent<string>(component, "meter.id");
    const componentId = component.id;
    const input: HistoryUtils.InputChannel[] = [
      {
        name: "GridConsumption",
        powerChannel: new ChannelAddress(meterId, "ActivePower"),
        // ggf. ValueConverter, z.B. NEGATIVE_AS_ZERO (je nach Usecase)
      },
      {
        name: "PeakShavingTarget",
        powerChannel: new ChannelAddress(componentId, "PeakShavingTargetPower"),
      },
      {
        name: "PeakShavedPower",
        powerChannel: new ChannelAddress(componentId, "PeakShavingPower"),
      },
      {
        name: "PeakShavingThreshold",
        powerChannel: new ChannelAddress(componentId, "_PropertyPeakShavingThresholdPower"),
      },
      {
        name: "RechargePower",
        powerChannel: new ChannelAddress(componentId, "_PropertyRechargePower"),
      },
      {
        name: "PeakshavingPower",
        powerChannel: new ChannelAddress(componentId, "_PropertyPeakShavingPower"),
      },
      {
        name: "EssSoc",
        powerChannel: new ChannelAddress(componentId, "EssSoc"),
      },
      {
        name: "EssPower",
        powerChannel: new ChannelAddress(componentId, "EssPower"),
      },
    ];

    return {
      input: input,
      output: (data: HistoryUtils.ChannelData) => {

        return [
          // main dataset
          {
            name: translate.instant("Edge.Index.Widgets.Peakshaving.gridConsumption"),
            color: ChartConstants.Colors.BLUE_GREY,
            converter: () => data["GridConsumption"],
            hideShadow: false,
          },
          {
            name: translate.instant("Edge.Index.Widgets.Peakshaving.peakshavingTarget"),
            color: ChartConstants.Colors.YELLOW,
            borderDash: [3, 3],
            converter: () => data["PeakShavingTarget"],
            hideShadow: true,
          },
          {
            name: translate.instant("Edge.Index.Widgets.Peakshaving.peakshavingActive"),
            color: ChartConstants.Colors.ORANGE,
            converter: () => data["PeakShavedPower"],
            hideShadow: true,
          },
          {
            name: translate.instant("Edge.Index.Widgets.Peakshaving.rechargePower"),
            color: ChartConstants.Colors.GREEN,
            borderDash: [3, 3],
            converter: () => data["RechargePower"],
            hideShadow: true,
          },
          {
            name: translate.instant("Edge.Index.Widgets.Peakshaving.peakshavingPower"),
            color: ChartConstants.Colors.RED,
            borderDash: [3, 3],
            converter: () => data["PeakshavingPower"],
            hideShadow: true,
          },
          {
            name: translate.instant("Edge.Index.Widgets.Peakshaving.peakShavingThresholdPower"),
            color: ChartConstants.Colors.PURPLE,
            borderDash: [8, 8],
            converter: () => data["PeakShavingThreshold"],
            hideShadow: true,
          },
          {
            name: translate.instant("General.soc"),
            color: ChartConstants.Colors.GREY,
            converter: () => data["EssSoc"]?.map(value => Utils.multiplySafely(value, 1000)),
            yAxisId: ChartAxis.RIGHT,
            borderDash: [10, 10],
            hideShadow: true,
          },
          {
            name: translate.instant("General.CHARGE"),
            color: ChartConstants.Colors.GREEN,
            converter: () => data["EssPower"]?.map(v => v != null && v < 0 ? -v : 0),
            hideShadow: true,
          },
          {
            name: translate.instant("General.DISCHARGE"),
            color: ChartConstants.Colors.RED,
            converter: () => data["EssPower"]?.map(v => v != null && v > 0 ? v : 0),
            hideShadow: false,
          },
        ];
      },
      tooltip: {
        formatNumber: ChartConstants.NumberFormat.ZERO_TO_TWO,
      },
      yAxes: [
        {
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
