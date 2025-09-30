import { Component } from "@angular/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChannelAddress } from "src/app/shared/shared";
import { ChartAxis, HistoryUtils, YAxisType } from "src/app/shared/utils/utils";

@Component({
  selector: "currentVoltageChart",
  templateUrl: "../../../../../components/chart/ABSTRACTHISTORYCHART.HTML",
  standalone: false,
})
export class CurrentVoltageSymmetricChartComponent extends AbstractHistoryChart {

  protected override getChartData(): HISTORY_UTILS.CHART_DATA {

    const component = THIS.CONFIG.GET_COMPONENT(THIS.ROUTE.SNAPSHOT.PARAMS.COMPONENT_ID);
    const chartObject: HISTORY_UTILS.CHART_DATA = {
      input: [
        {
          name: COMPONENT.ID + "Current",
          powerChannel: CHANNEL_ADDRESS.FROM_STRING(COMPONENT.ID + "/Current"),

        },
        {
          name: COMPONENT.ID + "Voltage",
          powerChannel: CHANNEL_ADDRESS.FROM_STRING(COMPONENT.ID + "/Voltage"),
        },
      ],
      output: (data: HISTORY_UTILS.CHANNEL_DATA) => [

        {
          name: THIS.TRANSLATE.INSTANT("EDGE.HISTORY.CURRENT"),
          converter: () => {
            return data[COMPONENT.ID + "Current"];
          },
          color: "rgb(253,197,7)",
          hiddenOnInit: false,
          stack: 1,

          yAxisId: CHART_AXIS.LEFT,
        },
        {
          name: THIS.TRANSLATE.INSTANT("EDGE.HISTORY.VOLTAGE"),
          converter: () => {
            return data[COMPONENT.ID + "Voltage"];
          },
          color: "rgb(255,0,0)",
          hiddenOnInit: false,
          stack: 1,
          yAxisId: CHART_AXIS.RIGHT,
        },
      ],
      tooltip: {
        formatNumber: "1.1-2",
        afterTitle: THIS.TRANSLATE.INSTANT("GENERAL.TOTAL"),
      },
      yAxes: [{
        unit: YAXIS_TYPE.VOLTAGE,
        position: "right",
        yAxisId: CHART_AXIS.RIGHT,
        displayGrid: false,
        scale: {
          dynamicScale: true,
        },
      },
      {
        unit: YAXIS_TYPE.CURRENT,
        position: "left",
        yAxisId: CHART_AXIS.LEFT,
      },
      ],
    };

    return chartObject;
  }
}
