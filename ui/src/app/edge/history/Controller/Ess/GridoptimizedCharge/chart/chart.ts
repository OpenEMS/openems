// @ts-strict-ignore
import { Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartAxis, HistoryUtils, Utils, YAxisType } from "src/app/shared/service/utils";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";

@Component({
  selector: "gridOptimizedChargeChart",
  templateUrl: "../../../../../../shared/components/chart/abstracthistorychart.html",
})
export class GridOptimizedChargeChartComponent extends AbstractHistoryChart {
  public static getChartData(component: EdgeConfig.Component, translate: TranslateService): HistoryUtils.ChartData {
    return {
      input: [
        {
          name: "DelayChargeMaximumChargeLimit",
          powerChannel: new ChannelAddress(component.id, "DelayChargeMaximumChargeLimit"),
        },
        {
          name: "SellToGridLimitMinimumChargeLimit",
          powerChannel: new ChannelAddress(component.id, "SellToGridLimitMinimumChargeLimit"),
          converter: HistoryUtils.ValueConverter.NEGATIVE_AS_ZERO,
        },
        {
          name: "ProductionDcActualPower",
          powerChannel: new ChannelAddress("_sum", "ProductionDcActualPower"),
        },
        {
          name: "EssActivePower",
          powerChannel: new ChannelAddress("_sum", "EssActivePower"),
        },
        {
          name: "EssSoc",
          powerChannel: new ChannelAddress("_sum", "EssSoc"),
        },
      ],
      output: (data: HistoryUtils.ChannelData) => ([
        {
          name: translate.instant("Edge.Index.Widgets.GridOptimizedCharge.maximumCharge"),
          converter: () => data["DelayChargeMaximumChargeLimit"],
          color: "rgb(253,197,7)",
          borderDash: [3, 3],
        },
        {
          name: translate.instant("Edge.Index.Widgets.GridOptimizedCharge.minimumCharge"),
          converter: () => data["SellToGridLimitMinimumChargeLimit"],
          color: "rgb(200,0,0)",
          borderDash: [3, 3],
        },
        {
          name: translate.instant("General.chargePower"),
          converter: () =>
            (data["ProductionDcActualPower"]
              ?
              data["ProductionDcActualPower"].map((value, index) => {
                return Utils.subtractSafely(data["EssActivePower"][index], value);
              })
              :
              data["EssActivePower"])?.map(val => HistoryUtils.ValueConverter.POSITIVE_AS_ZERO_AND_INVERT_NEGATIVE(val)) ?? null,
          color: "rgb(0,223,0)",
        },
        {
          name: translate.instant("General.soc"),
          converter: () => data["EssSoc"].map(el => Utils.multiplySafely(el, 1000)),
          color: "rgb(189, 195, 199)",
          borderDash: [10, 10],
          yAxisId: ChartAxis.RIGHT,
          custom: {
            unit: YAxisType.PERCENTAGE,
          },
        },
      ]),
      tooltip: {
        formatNumber: "1.0-2",
      },
      yAxes: [{
        unit: YAxisType.ENERGY,
        position: "left",
        yAxisId: ChartAxis.LEFT,
      }, {
        unit: YAxisType.PERCENTAGE,
        position: "right",
        yAxisId: ChartAxis.RIGHT,
        displayGrid: false,
      }],
    };
  }
  protected getChartData(): HistoryUtils.ChartData {
    return GridOptimizedChargeChartComponent.getChartData(this.component, this.translate);
  }

}
