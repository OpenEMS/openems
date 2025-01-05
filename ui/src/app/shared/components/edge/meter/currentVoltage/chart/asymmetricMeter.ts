import { Component } from "@angular/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { Phase } from "src/app/shared/components/shared/phase";
import { ChartAxis, HistoryUtils, YAxisType } from "src/app/shared/service/utils";
import { ChannelAddress } from "src/app/shared/shared";

@Component({
  selector: "currentVoltageAsymmetricChart",
  templateUrl: "../../../../../components/chart/abstracthistorychart.html",
  standalone: false,
})
export class CurrentVoltageAsymmetricChartComponent extends AbstractHistoryChart {

  protected override getChartData(): HistoryUtils.ChartData {

    const component = this.config.getComponent(this.route.snapshot.params.componentId);
    const currentPhasesColors: string[] = ["rgb(246, 180, 137)", "rgb(238, 120, 42)", "rgb(118, 52, 9)"];
    const voltagePhasesColors: string[] = ["rgb(255, 0, 0)", "rgb(133, 0, 0)", "rgb(71, 0, 0)"];
    const chartObject: HistoryUtils.ChartData = {
      input: [
        ...Phase.THREE_PHASE.map((phase) => ({
          name: "Current" + phase,
          powerChannel: ChannelAddress.fromString(component.id + "/Current" + phase),
        })),
        ...Phase.THREE_PHASE.map((phase) => ({
          name: "Voltage" + phase,
          powerChannel: ChannelAddress.fromString(component.id + "/Voltage" + phase),
        })),
      ],
      output: (data: HistoryUtils.ChannelData) => [
        ...Phase.THREE_PHASE.map((phase, index) => ({
          name: this.translate.instant("Edge.History.CURRENT") + " " + phase,
          converter: () => {
            return data["Current" + phase];
          },
          hideShadow: true,
          color: currentPhasesColors[index],
          yAxisId: ChartAxis.LEFT,
        })),
        ...Phase.THREE_PHASE.map((phase, index) => ({
          name: this.translate.instant("Edge.History.VOLTAGE") + " " + phase,
          converter: () => {
            return data["Voltage" + phase];
          },
          hideShadow: true,
          color: voltagePhasesColors[index],
          yAxisId: ChartAxis.RIGHT,
        })),
      ],
      tooltip: {
        formatNumber: "1.1-2",
        afterTitle: this.translate.instant("General.TOTAL"),
      },
      yAxes: [{
        unit: YAxisType.VOLTAGE,
        position: "right",
        yAxisId: ChartAxis.RIGHT,
        displayGrid: false,
      },
      {
        unit: YAxisType.CURRENT,
        position: "left",
        yAxisId: ChartAxis.LEFT,
      },
      ],
    };

    return chartObject;
  }
}
