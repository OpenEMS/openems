import { Component } from "@angular/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { Phase } from "src/app/shared/components/shared/phase";
import { ChannelAddress } from "src/app/shared/shared";
import { ChartAxis, HistoryUtils, YAxisType } from "src/app/shared/utils/utils";

@Component({
  selector: "currentVoltageAsymmetricChart",
  templateUrl: "../../../../../components/chart/ABSTRACTHISTORYCHART.HTML",
  standalone: false,
})
export class CurrentVoltageAsymmetricChartComponent extends AbstractHistoryChart {

  protected override getChartData(): HISTORY_UTILS.CHART_DATA {

    const component = THIS.CONFIG.GET_COMPONENT(THIS.ROUTE.SNAPSHOT.PARAMS.COMPONENT_ID);
    const currentPhasesColors: string[] = ["rgb(246, 180, 137)", "rgb(238, 120, 42)", "rgb(118, 52, 9)"];
    const voltagePhasesColors: string[] = ["rgb(255, 0, 0)", "rgb(133, 0, 0)", "rgb(71, 0, 0)"];
    const chartObject: HISTORY_UTILS.CHART_DATA = {
      input: [
        ...Phase.THREE_PHASE.map((phase) => ({
          name: "Current" + phase,
          powerChannel: CHANNEL_ADDRESS.FROM_STRING(COMPONENT.ID + "/Current" + phase),
        })),
        ...Phase.THREE_PHASE.map((phase) => ({
          name: "Voltage" + phase,
          powerChannel: CHANNEL_ADDRESS.FROM_STRING(COMPONENT.ID + "/Voltage" + phase),
        })),
      ],
      output: (data: HISTORY_UTILS.CHANNEL_DATA) => [
        ...Phase.THREE_PHASE.map((phase, index) => ({
          name: THIS.TRANSLATE.INSTANT("EDGE.HISTORY.CURRENT") + " " + phase,
          converter: () => {
            return data["Current" + phase];
          },
          hideShadow: true,
          color: currentPhasesColors[index],
          yAxisId: CHART_AXIS.LEFT,
        })),
        ...Phase.THREE_PHASE.map((phase, index) => ({
          name: THIS.TRANSLATE.INSTANT("EDGE.HISTORY.VOLTAGE") + " " + phase,
          converter: () => {
            return data["Voltage" + phase];
          },
          hideShadow: true,
          color: voltagePhasesColors[index],
          yAxisId: CHART_AXIS.RIGHT,
        })),
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
