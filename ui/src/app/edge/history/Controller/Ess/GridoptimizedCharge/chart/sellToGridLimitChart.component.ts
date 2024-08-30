// @ts-strict-ignore
import { Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartAxis, HistoryUtils, Utils, YAxisType } from "src/app/shared/service/utils";
import { ChannelAddress } from "src/app/shared/shared";

@Component({
  selector: "sellToGridLimitChart",
  templateUrl: "../../../../../../shared/components/chart/abstracthistorychart.html",
})
export class SellToGridLimitChartComponent extends AbstractHistoryChart {

  public static getChartData(gridmeterId: string, componentId: string, translate: TranslateService): HistoryUtils.ChartData {
    return {
      input: [
        {
          name: "ActivePower",
          powerChannel: new ChannelAddress(gridmeterId, "ActivePower"),
          converter: HistoryUtils.ValueConverter.ONLY_NEGATIVE_AND_NEGATIVE_AS_POSITIVE,
        },
        {
          name: "_PropertyMaximumSellToGridPower",
          powerChannel: new ChannelAddress(componentId, "_PropertyMaximumSellToGridPower"),
        },
        {
          name: "ProductionActivePower",
          powerChannel: new ChannelAddress("_sum", "ProductionActivePower"),
        },
      ],
      output: (data: HistoryUtils.ChannelData) => ([
        {
          name: translate.instant("General.gridSell"),
          converter: () => data["ActivePower"],
          color: "rgb(0,0,200)",
        },
        {
          name: translate.instant("Edge.Index.Widgets.GridOptimizedCharge.maximumGridFeedIn"),
          converter: () => data["_PropertyMaximumSellToGridPower"],
          color: "rgb(0,0,0)",
          borderDash: [3, 3],
        },
        {
          name: translate.instant("Edge.Index.Widgets.GridOptimizedCharge.MAXIMUM_GRIDSELL_WITH_CHARGE"),
          converter: () => data["_PropertyMaximumSellToGridPower"].map(el => Utils.multiplySafely(el, 0.95)),
          color: "rgb(200,0,0)",
          borderDash: [3, 3],
        },
        {
          name: translate.instant("General.production"),
          converter: () => data["ProductionActivePower"],
          color: "rgb(45,143,171)",
        },
      ]),
      tooltip: {
        formatNumber: "1.0-2",
      },
      yAxes: [{
        unit: YAxisType.ENERGY,
        position: "left",
        yAxisId: ChartAxis.LEFT,
      }],
    };
  }

  protected getChartData(): HistoryUtils.ChartData {
    const gridMeterId = this.config.getComponentProperties(this.component.id)["meter.id"];
    return SellToGridLimitChartComponent.getChartData(gridMeterId, this.component.id, this.translate);
  }

}
