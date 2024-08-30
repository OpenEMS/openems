import { Component } from "@angular/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartAxis, HistoryUtils, YAxisTitle } from "src/app/shared/service/utils";
import { ChannelAddress } from "src/app/shared/shared";

@Component({
  selector: "currentVoltageChart",
  templateUrl: "../../../../../components/chart/abstracthistorychart.html",
})
export class CurrentVoltageSymmetricChartComponent extends AbstractHistoryChart {

  protected override getChartData(): HistoryUtils.ChartData {

    const component = this.config.getComponent(this.route.snapshot.params.componentId);
    const chartObject: HistoryUtils.ChartData = {
      input: [
        {
          name: component.id + "Current",
          powerChannel: ChannelAddress.fromString(component.id + "/Current"),

        },
        {
          name: component.id + "Voltage",
          powerChannel: ChannelAddress.fromString(component.id + "/Voltage"),
        },
      ],
      output: (data: HistoryUtils.ChannelData) => [

        {
          name: this.translate.instant("Edge.History.CURRENT"),
          converter: () => {
            return data[component.id + "Current"];
          },
          color: "rgb(253,197,7)",
          hiddenOnInit: false,
          stack: 1,

          yAxisId: ChartAxis.RIGHT,
        },
        {
          name: this.translate.instant("Edge.History.VOLTAGE"),
          converter: () => {
            return data[component.id + "Voltage"];
          },
          color: "rgb(255,0,0)",
          hiddenOnInit: false,
          stack: 1,
          yAxisId: ChartAxis.LEFT,
        },
      ],
      tooltip: {
        formatNumber: "1.1-2",
        afterTitle: this.translate.instant("General.TOTAL"),
      },
      yAxes: [{
        unit: YAxisTitle.VOLTAGE,
        position: "left",
        yAxisId: ChartAxis.LEFT,
      },
      {
        unit: YAxisTitle.CURRENT,
        position: "right",
        yAxisId: ChartAxis.RIGHT,
      },
      ],
    };

    return chartObject;
  }
}
