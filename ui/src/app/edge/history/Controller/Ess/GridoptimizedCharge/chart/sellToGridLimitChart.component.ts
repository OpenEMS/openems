// @ts-strict-ignore
import { Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/CHART.CONSTANTS";
import { ChannelAddress } from "src/app/shared/shared";
import { ChartAxis, HistoryUtils, Utils, YAxisType } from "src/app/shared/utils/utils";

@Component({
  selector: "sellToGridLimitChart",
  templateUrl: "../../../../../../shared/components/chart/ABSTRACTHISTORYCHART.HTML",
  standalone: false,
})
export class SellToGridLimitChartComponent extends AbstractHistoryChart {

  public static getChartData(gridmeterId: string, componentId: string, translate: TranslateService): HISTORY_UTILS.CHART_DATA {
    return {
      input: [
        {
          name: "ActivePower",
          powerChannel: new ChannelAddress(gridmeterId, "ActivePower"),
          converter: HISTORY_UTILS.VALUE_CONVERTER.ONLY_NEGATIVE_AND_NEGATIVE_AS_POSITIVE,
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
      output: (data: HISTORY_UTILS.CHANNEL_DATA) => ([
        {
          name: TRANSLATE.INSTANT("GENERAL.GRID_SELL"),
          converter: () => data["ActivePower"],
          color: CHART_CONSTANTS.COLORS.PURPLE,
        },
        {
          name: TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.MAXIMUM_GRID_FEED_IN"),
          converter: () => data["_PropertyMaximumSellToGridPower"],
          color: CHART_CONSTANTS.COLORS.YELLOW,
          hideShadow: true,
          borderDash: [3, 3],
        },
        {
          name: TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.MAXIMUM_GRIDSELL_WITH_CHARGE"),
          converter: () => data["_PropertyMaximumSellToGridPower"].map(el => UTILS.MULTIPLY_SAFELY(el, 0.95)),
          color: CHART_CONSTANTS.COLORS.RED,
          hideShadow: true,
          borderDash: [3, 3],
        },
        {
          name: TRANSLATE.INSTANT("GENERAL.PRODUCTION"),
          converter: () => data["ProductionActivePower"],
          color: "rgb(45,143,171)",
        },
      ]),
      tooltip: {
        formatNumber: "1.0-2",
      },
      yAxes: [{
        unit: YAXIS_TYPE.ENERGY,
        position: "left",
        yAxisId: CHART_AXIS.LEFT,
      }],
    };
  }

  protected getChartData(): HISTORY_UTILS.CHART_DATA {
    const gridMeterId = THIS.CONFIG.GET_COMPONENT_PROPERTIES(THIS.COMPONENT.ID)["METER.ID"];
    return SELL_TO_GRID_LIMIT_CHART_COMPONENT.GET_CHART_DATA(gridMeterId, THIS.COMPONENT.ID, THIS.TRANSLATE);
  }

}
