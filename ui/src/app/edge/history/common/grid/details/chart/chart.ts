// @ts-strict-ignore
import { Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/CHART.CONSTANTS";
import { Phase } from "src/app/shared/components/shared/phase";
import { ChannelAddress } from "src/app/shared/shared";
import { DefaultTypes } from "src/app/shared/type/defaulttypes";
import { ChartAxis, HistoryUtils, YAxisType } from "src/app/shared/utils/utils";

@Component({
  selector: "gridDetailsChart",
  templateUrl: "../../../../../../shared/components/chart/ABSTRACTHISTORYCHART.HTML",
  standalone: false,
})
export class ChartComponent extends AbstractHistoryChart {

  public static getChartData(translate: TranslateService): HISTORY_UTILS.CHART_DATA {
    return {
      input: [
        {
          name: "GridActivePower",
          powerChannel: CHANNEL_ADDRESS.FROM_STRING("_sum/GridActivePower"),
        },
        ...Phase.THREE_PHASE.map((phase, index) => ({
          name: "Phase" + phase,
          powerChannel: CHANNEL_ADDRESS.FROM_STRING("_sum/GridActivePower" + phase),
        })),
      ],
      output: (data: DEFAULT_TYPES.HISTORY.CHANNEL_DATA) => {

        const datasets: DEFAULT_TYPES.HISTORY.DISPLAY_VALUES[] =
          [
            {
              name: TRANSLATE.INSTANT("GENERAL.TOTAL"),
              converter: () => {
                return data["GridActivePower"];
              },
              color: CHART_CONSTANTS.COLORS.BLUE,
              stack: 1,
            },
            ...Phase.THREE_PHASE.map((phase, index) => ({
              name: "Phase " + phase,
              converter: () => {
                return data["Phase" + phase];
              },
              color: ABSTRACT_HISTORY_CHART.PHASE_COLORS[index],
            })),
          ];

        return datasets;
      },
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

  public override getChartData() {
    return CHART_COMPONENT.GET_CHART_DATA(THIS.TRANSLATE);
  }
}
