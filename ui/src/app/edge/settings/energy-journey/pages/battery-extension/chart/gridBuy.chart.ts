// @ts-strict-ignore
import { Component, Input, OnChanges } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartAxis, HistoryUtils, Utils, YAxisType } from "src/app/shared/service/utils";
import { ChannelAddress, ChartConstants } from "src/app/shared/shared";
import { TPropType } from "src/app/shared/type/utility";
import { SimulationResult } from "../battery-extension.module";
import { PossibleBatteryExtensionResponse } from "../battery-extension.response";

@Component({
  selector: "gridBuyChart",
  templateUrl: "../../../../../../shared/components/chart/abstracthistorychart.html",
  standalone: false,
})
export class GridBuyChartComponent extends AbstractHistoryChart implements OnChanges {

  @Input({ required: true }) public simulatedData: SimulationResult = {
    simulationData: [],
    charge: 0,
    discharge: 0,
  };
  @Input({ required: true }) public fromDate: Date;
  @Input({ required: true }) public toDate: Date;

  public static getChartData(simulatedData: PossibleBatteryExtensionResponse["result"]["simulationData"], translate: TranslateService): HistoryUtils.ChartData {
    return {
      input: [
        {
          name: "GridBuyActiveEnergy",
          powerChannel: new ChannelAddress("_sum", "GridActivePower"),
          energyChannel: new ChannelAddress("_sum", "GridBuyActiveEnergy"),
        },
      ],
      output: () => {
        const displayData = [
          ...(simulatedData?.length > 0
            ? Object.entries(simulatedData)
              .reduce((arr: number[], [_name, _simulatedData], i) => {
                arr.push(_simulatedData.gridBuy);
                return arr;
              }, [])
            : []),
        ];
        return [
          <HistoryUtils.DisplayValue<HistoryUtils.DataLabelsCustomOptions>>{
            name: translate.instant("SETTINGS.ENERGY_JOURNEY.BATTERY_MODULE_EXTENSION"),
            converter: () => {
              return displayData
                .map((el) => Utils.subtractSafely(displayData[0], Utils.divideSafely(el, 1)))
                .filter((_el, i) => i !== 0);
            },
            color: ChartConstants.Colors.BLUE,
            custom: {
              pluginType: "datalabels",
              datalabels: {
                displayUnit: "kWh",
              },
            },
          },
        ];
      },
      tooltip: {
        formatNumber: "1.0-0",
        enabled: false,
      },
      yAxes: [{
        unit: YAxisType.ENERGY,
        position: "left",
        yAxisId: ChartAxis.LEFT,
      }],
    };
  }

  private static getLabels(simulationData: TPropType<SimulationResult, "simulationData">, translate: TranslateService): string[] {
    return simulationData.reduce((arr: string[], el, index) => {
      if (index === 0) {
        return arr;
      }
      const diff = Utils.subtractSafely(el.numberOfModules, simulationData[0].numberOfModules);
      if (!diff) {
        return arr;
      }

      arr.push("+ " + diff + " " + (diff === 1 ? translate.instant("SETTINGS.ENERGY_JOURNEY.MODULE") : translate.instant("SETTINGS.ENERGY_JOURNEY.MODULES")));
      return arr;
    }, []);
  }

  ngOnChanges() {
    this.updateChart();
  }

  public override getChartData() {
    return GridBuyChartComponent.getChartData(this.simulatedData?.simulationData, this.translate);
  }

  protected override beforeSetChartLabel(): void {
    this.labels = GridBuyChartComponent.getLabels(this.simulatedData.simulationData, this.translate);
  }


  protected override async loadChart() {
    this.labels = [];
    this.errorResponse = null;

    this.queryHistoricTimeseriesEnergyPerPeriod(this.fromDate, this.toDate)
      .then((energyPeriodResponse) => {
        this.chartType = "bar";
        this.chartObject = this.getChartData();
        const displayValues = AbstractHistoryChart.fillChart(this.chartType, this.chartObject, energyPeriodResponse);
        this.datasets = displayValues.datasets;
        this.legendOptions = displayValues.legendOptions;
        this.labels = displayValues.labels;
        this.beforeSetChartLabel();
        this.setChartLabel();
      });
  }
}
