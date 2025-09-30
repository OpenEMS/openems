// @ts-strict-ignore
import { Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/CHART.CONSTANTS";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";
import { ChartAxis, HistoryUtils, Utils, YAxisType } from "src/app/shared/utils/utils";

@Component({
  selector: "gridOptimizedChargeChart",
  templateUrl: "../../../../../../shared/components/chart/ABSTRACTHISTORYCHART.HTML",
  standalone: false,
})
export class GridOptimizedChargeChartComponent extends AbstractHistoryChart {
  public static getChartData(component: EDGE_CONFIG.COMPONENT, translate: TranslateService): HISTORY_UTILS.CHART_DATA {
    return {
      input: [
        {
          name: "DelayChargeMaximumChargeLimit",
          powerChannel: new ChannelAddress(COMPONENT.ID, "DelayChargeMaximumChargeLimit"),
        },
        {
          name: "SellToGridLimitMinimumChargeLimit",
          powerChannel: new ChannelAddress(COMPONENT.ID, "SellToGridLimitMinimumChargeLimit"),
          converter: HISTORY_UTILS.VALUE_CONVERTER.NEGATIVE_AS_ZERO,
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
      output: (data: HISTORY_UTILS.CHANNEL_DATA) => ([
        {
          name: TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.MAXIMUM_CHARGE"),
          converter: () => data["DelayChargeMaximumChargeLimit"],
          color: CHART_CONSTANTS.COLORS.YELLOW,
          borderDash: [3, 3],
        },
        {
          name: TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.MINIMUM_CHARGE"),
          converter: () => data["SellToGridLimitMinimumChargeLimit"],
          color: CHART_CONSTANTS.COLORS.RED,
          borderDash: [3, 3],
        },
        {
          name: TRANSLATE.INSTANT("GENERAL.CHARGE"),
          converter: () =>
            (data["ProductionDcActualPower"]
              ?
              data["ProductionDcActualPower"].map((value, index) => {
                return UTILS.SUBTRACT_SAFELY(data["EssActivePower"][index], value);
              })
              :
              data["EssActivePower"])?.map(val => HISTORY_UTILS.VALUE_CONVERTER.POSITIVE_AS_ZERO_AND_INVERT_NEGATIVE(val)) ?? null,
          color: CHART_CONSTANTS.COLORS.GREEN,
        },
        {
          name: TRANSLATE.INSTANT("GENERAL.SOC"),
          converter: () => data["EssSoc"].map(el => UTILS.MULTIPLY_SAFELY(el, 1000)),
          color: "rgb(189, 195, 199)",
          borderDash: [10, 10],
          yAxisId: CHART_AXIS.RIGHT,
          custom: {
            unit: YAXIS_TYPE.PERCENTAGE,
          },
        },
      ]),
      tooltip: {
        formatNumber: "1.0-2",
      },
      yAxes: [{
        unit: YAXIS_TYPE.ENERGY,
        position: "left",
        yAxisId: CHART_AXIS.LEFT,
      }, {
        unit: YAXIS_TYPE.PERCENTAGE,
        position: "right",
        yAxisId: CHART_AXIS.RIGHT,
        displayGrid: false,
      }],
    };
  }
  protected getChartData(): HISTORY_UTILS.CHART_DATA {
    return GRID_OPTIMIZED_CHARGE_CHART_COMPONENT.GET_CHART_DATA(THIS.COMPONENT, THIS.TRANSLATE);
  }

}
