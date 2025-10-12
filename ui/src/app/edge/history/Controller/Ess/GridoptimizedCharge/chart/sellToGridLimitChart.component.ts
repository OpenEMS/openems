// @ts-strict-ignore
import { Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/chart.constants";
import { hasMaximumGridFeedInLimitInMeta } from "src/app/shared/permissions/edgePermissions";
import { ChannelAddress, Edge } from "src/app/shared/shared";
import { ChartAxis, HistoryUtils, Utils, YAxisType } from "src/app/shared/utils/utils";

@Component({
  selector: "sellToGridLimitChart",
  templateUrl: "../../../../../../shared/components/chart/abstracthistorychart.html",
  standalone: false,
})
export class SellToGridLimitChartComponent extends AbstractHistoryChart {

  public static getChartData(gridmeterId: string, componentId: string, translate: TranslateService, edge: Edge): HistoryUtils.ChartData {
    return {
      input: [
        {
          name: "ActivePower",
          powerChannel: new ChannelAddress(gridmeterId, "ActivePower"),
          converter: HistoryUtils.ValueConverter.ONLY_NEGATIVE_AND_NEGATIVE_AS_POSITIVE,
        },
        ...(hasMaximumGridFeedInLimitInMeta(edge) ? [
          {
            name: "_PropertyMaximumGridFeedInLimit",
            powerChannel: new ChannelAddress("_meta", "_PropertyMaximumGridFeedInLimit"),
          },
        ] :
          [
            {
              name: "_PropertyMaximumSellToGridPower",
              powerChannel: new ChannelAddress(componentId, "_PropertyMaximumSellToGridPower"),
            },
          ]),
        {
          name: "ProductionActivePower",
          powerChannel: new ChannelAddress("_sum", "ProductionActivePower"),
        },
      ],
      output: (data: HistoryUtils.ChannelData) => ([
        {
          name: translate.instant("General.gridSell"),
          converter: () => data["ActivePower"],
          color: ChartConstants.Colors.PURPLE,
        },
        {
          name: translate.instant("Edge.Index.Widgets.GridOptimizedCharge.maximumGridFeedIn"),
          converter: hasMaximumGridFeedInLimitInMeta(edge) ? () => data["_PropertyMaximumGridFeedInLimit"] : () => data["_PropertyMaximumSellToGridPower"],
          color: ChartConstants.Colors.YELLOW,
          hideShadow: true,
          borderDash: [3, 3],
        },
        {
          name: translate.instant("Edge.Index.Widgets.GridOptimizedCharge.MAXIMUM_GRIDSELL_WITH_CHARGE"),
          converter: hasMaximumGridFeedInLimitInMeta(edge) ? () => data["_PropertyMaximumGridFeedInLimit"].map(el => Utils.multiplySafely(el, 0.95))
            : () => data["_PropertyMaximumSellToGridPower"].map(el => Utils.multiplySafely(el, 0.95)),
          color: ChartConstants.Colors.RED,
          hideShadow: true,
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
    return SellToGridLimitChartComponent.getChartData(gridMeterId, this.component.id, this.translate, this.edge);
  }

}
