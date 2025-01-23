import { Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/chart.constants";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChartAxis, HistoryUtils, YAxisType, DisplayValue } from "src/app/shared/service/utils";
import { ChannelAddress, EdgeConfig, Utils } from "src/app/shared/shared";
import { Service } from "src/app/shared/service/service";

@Component({
  selector: "energychart",
  templateUrl: "../../../../../shared/components/chart/abstracthistorychart.html",
  standalone: false,
})
export class ChartComponent extends AbstractHistoryChart {
  public period1: string = 'day';
  public period2: string = 'day';
  public date1: string;
  public date2: string;

  constructor(
    protected translate: TranslateService,
    private service: Service
  ) {
    super();
  }

  public static getChartData(config: EdgeConfig | null, chartType: "line" | "bar", translate: TranslateService): HistoryUtils.ChartData {
    const input: HistoryUtils.InputChannel[] =
      config?.widgets.classes.reduce((arr: HistoryUtils.InputChannel[], key) => {
        const newObj = [];
        switch (key) {
          case "Energymonitor":
          case "Consumption":
            newObj.push({
              name: "Consumption",
              powerChannel: new ChannelAddress("_sum", "ConsumptionActivePower"),
              energyChannel: new ChannelAddress("_sum", "ConsumptionActiveEnergy"),
            });
            break;
          case "Common_Autarchy":
          case "Grid":
            newObj.push({
              name: "GridBuy",
              powerChannel: new ChannelAddress("_sum", "GridActivePower"),
              energyChannel: new ChannelAddress("_sum", "GridBuyActiveEnergy"),
              ...(chartType === "line" && { converter: HistoryUtils.ValueConverter.NEGATIVE_AS_ZERO }),
            }, {
              name: "GridSell",
              powerChannel: new ChannelAddress("_sum", "GridActivePower"),
              energyChannel: new ChannelAddress("_sum", "GridSellActiveEnergy"),
              ...(chartType === "line" && { converter: HistoryUtils.ValueConverter.POSITIVE_AS_ZERO_AND_INVERT_NEGATIVE }),
            });
            break;
          case "Storage":
            newObj.push({
              name: "EssSoc",
              powerChannel: new ChannelAddress("_sum", "EssSoc"),
            });
            break;
          case "Production":
            newObj.push({
              name: "Production",
              powerChannel: new ChannelAddress("_sum", "ProductionActivePower"),
              energyChannel: new ChannelAddress("_sum", "ProductionActiveEnergy"),
            }, {
              name: "ProductionDcActual",
              powerChannel: new ChannelAddress("_sum", "ProductionDcActualPower"),
              energyChannel: new ChannelAddress("_sum", "ProductionActiveEnergy"),
            });
            break;
        }

        arr.push(...newObj);
        return arr;
      }, []);

    return {
      input: input,
      output: (data: HistoryUtils.ChannelData) => {
        return [
          {
            name: translate.instant("General.production"),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => energyValues.result.data["_sum/ProductionActiveEnergy"],
            converter: () => data["ProductionActivePower"],
            color: ChartConstants.Colors.BLUE,
            stack: 0,
            hiddenOnInit: chartType == "line" ? false : true,
            order: 1,
          },

          // DirectConsumption, displayed in stack 1 & 2, only one legenItem
          ...[chartType === "bar" && {
            name: translate.instant("General.directConsumption"),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
              return Utils.subtractSafely(energyValues.result.data["_sum/ProductionActiveEnergy"], energyValues.result.data["_sum/GridSellActiveEnergy"], energyValues.result.data["_sum/EssDcChargeEnergy"]);
            },
            converter: () =>
              data["ProductionActivePower"]?.map((value, index) => Utils.subtractSafely(value, data["GridSell"][index], data["EssCharge"][index]))
                ?.map(value => HistoryUtils.ValueConverter.NEGATIVE_AS_ZERO(value)),
            color: ChartConstants.Colors.ORANGE,
            stack: [1, 2],
            order: 2,
          }],

          // Charge Power
          {
            name: translate.instant("General.CHARGE"),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => energyValues.result.data["_sum/EssDcChargeEnergy"],
            converter: () => chartType === "line" //
              ? data["EssCharge"]?.map((value, index) => {
                return HistoryUtils.ValueConverter.POSITIVE_AS_ZERO_AND_INVERT_NEGATIVE(Utils.subtractSafely(value, data["ProductionDcActual"]?.[index]));
              }) : data["EssCharge"],
            color: ChartConstants.Colors.GREEN,
            stack: 1,
            ...(chartType === "line" && { order: 6 }),
          },

          // Discharge Power
          {
            name: translate.instant("General.DISCHARGE"),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => energyValues.result.data["_sum/EssDcDischargeEnergy"],
            converter: () => {
              return chartType === "line" ?
                data["EssDischarge"]?.map((value, index) => {
                  return HistoryUtils.ValueConverter.NEGATIVE_AS_ZERO(Utils.subtractSafely(value, data["ProductionDcActual"]?.[index]));
                }) : data["EssDischarge"];
            },
            color: ChartConstants.Colors.RED,
            stack: 2,
            ...(chartType === "line" && { order: 5 }),
          },

          // Sell to grid
          {
            name: translate.instant("General.gridSellAdvanced"),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => energyValues.result.data["_sum/GridSellActiveEnergy"],
            converter: () => data["GridSell"],
            color: ChartConstants.Colors.PURPLE,
            stack: 1,
            ...(chartType === "line" && { order: 4 }),
          },

          // Buy from Grid
          {
            name: translate.instant("General.gridBuyAdvanced"),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => energyValues.result.data["_sum/GridBuyActiveEnergy"],
            converter: () => data["GridBuy"],
            color: ChartConstants.Colors.BLUE_GREY,
            stack: 2,
            ...(chartType === "line" && { order: 2 }),
          },

          // Consumption
          {
            name: translate.instant("General.consumption"),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => energyValues.result.data["_sum/ConsumptionActiveEnergy"],
            converter: () => data["Consumption"],
            color: ChartConstants.Colors.YELLOW,
            stack: 3,
            hiddenOnInit: chartType == "line" ? false : true,
            ...(chartType === "line" && { order: 0 }),
          },
          ...[chartType === "line" &&
          {
            name: translate.instant("General.soc"),
            converter: () => data["EssSoc"]?.map(value => Utils.multiplySafely(value, 1000)),
            color: "rgb(189, 195, 199)",
            borderDash: [10, 10],
            yAxisId: ChartAxis.RIGHT,
            stack: 1,
          }],
        ];
      },
      tooltip: {
        formatNumber: "1.0-2",
        afterTitle: (stack: string) => {

          if (chartType === "bar") {
            if (stack === "1") {
              return translate.instant("General.production");
            } else if (stack === "2") {
              return translate.instant("General.consumption");
            }
          }
          return null;
        },
      },
      yAxes: [

        // Left YAxis
        {
          unit: YAxisType.ENERGY,
          position: "left",
          yAxisId: ChartAxis.LEFT,
        },

        // Right Yaxis, only shown for line-chart
        (chartType === "line" && {
          unit: YAxisType.PERCENTAGE,
          customTitle: "%",
          position: "right",
          yAxisId: ChartAxis.RIGHT,
          displayGrid: false,
        }),
      ],
      normalizeOutputData: true,
    };
  }

  public static getComparisonChartData(date1: string, date2: string, period1: string, period2: string, translate: TranslateService): HistoryUtils.ChartData {
    const comparisonInput: HistoryUtils.InputChannel[] = [
      {
        name: `Période ${period1} ${date1}`,
        powerChannel: new ChannelAddress("_sum", "ConsumptionActivePower"),
        energyChannel: new ChannelAddress("_sum", "ConsumptionActiveEnergy"),
      },
      {
        name: `Période ${period2} ${date2}`,
        powerChannel: new ChannelAddress("_sum", "ConsumptionActivePower"),
        energyChannel: new ChannelAddress("_sum", "ConsumptionActiveEnergy"),
      }
    ];

    return {
      input: comparisonInput,
      output: (data: HistoryUtils.ChannelData) => {
        const output: DisplayValue[] = [];
        comparisonInput.forEach(channel => {
          output.push({
            label: channel.name,
            data: data[channel.energyChannel.toString()],
            backgroundColor: channel.name.includes(date1) ? "rgba(75, 192, 192, 0.2)" : "rgba(153, 102, 255, 0.2)",
            borderColor: channel.name.includes(date1) ? "rgba(75, 192, 192, 1)" : "rgba(153, 102, 255, 1)",
            borderWidth: 1,
          });
        });
        return output;
      },
      tooltip: {
        formatNumber: "1.0-2",
      },
      yAxes: [
        {
          unit: YAxisType.ENERGY,
          position: "left",
          yAxisId: ChartAxis.LEFT,
        }
      ],
    };
  }

  public override getChartData() {
    return ChartComponent.getChartData(this.config, this.chartType, this.translate);
  }

  public getComparisonChartData(date1: string, date2: string, period1: string, period2: string) {
    return ChartComponent.getComparisonChartData(date1, date2, period1, period2, this.translate);
  }

  protected override getChartHeight(): number {
    return this.service.deviceHeight / 2;
  }

  public isInputValid(): boolean {
    return !!this.date1 && !!this.date2 && !!this.period1 && !!this.period2;
  }

  public onComparePeriods() {
    if (!this.isInputValid()) {
      console.error("Dates or periods are not valid.");
      return;
    }

    Promise.all([
      this.service.getDataForDate(this.date1),
      this.service.getDataForDate(this.date2),
    ]).then(([data1, data2]) => {
      const comparisonData = this.getComparisonChartData(this.date1, this.date2, this.period1, this.period2);
      this.datasets = comparisonData.output;
      this.labels = comparisonData.input.map(channel => channel.name);
    });
  }

}