// @ts-strict-ignore
import { Component } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/CHART.CONSTANTS";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { ChannelAddress, EdgeConfig, Utils } from "src/app/shared/shared";
import { ChartAxis, HistoryUtils, YAxisType } from "src/app/shared/utils/utils";

@Component({
  selector: "energychart",
  templateUrl: "../../../../../shared/components/chart/ABSTRACTHISTORYCHART.HTML",
  standalone: false,
})
export class ChartComponent extends AbstractHistoryChart {

  public static getChartData(config: EdgeConfig | null, chartType: "line" | "bar", translate: TranslateService): HISTORY_UTILS.CHART_DATA {
    const input: HISTORY_UTILS.INPUT_CHANNEL[] =
      config?.WIDGETS.CLASSES.REDUCE((arr: HISTORY_UTILS.INPUT_CHANNEL[], key) => {
        const newObj = [];
        switch (key) {
          case "Energymonitor":
          case "Consumption":
            NEW_OBJ.PUSH({
              name: "Consumption",
              powerChannel: new ChannelAddress("_sum", "ConsumptionActivePower"),
              energyChannel: new ChannelAddress("_sum", "ConsumptionActiveEnergy"),
            });
            break;
          case "Common_Autarchy":
          case "Grid":
            NEW_OBJ.PUSH({
              name: "GridBuy",
              powerChannel: new ChannelAddress("_sum", "GridActivePower"),
              energyChannel: new ChannelAddress("_sum", "GridBuyActiveEnergy"),
              ...(chartType === "line" && { converter: HISTORY_UTILS.VALUE_CONVERTER.NEGATIVE_AS_ZERO }),
            }, {
              name: "GridSell",
              powerChannel: new ChannelAddress("_sum", "GridActivePower"),
              energyChannel: new ChannelAddress("_sum", "GridSellActiveEnergy"),
              ...(chartType === "line" && { converter: HISTORY_UTILS.VALUE_CONVERTER.POSITIVE_AS_ZERO_AND_INVERT_NEGATIVE }),
            });
            break;
          case "Storage":
            NEW_OBJ.PUSH({
              name: "EssSoc",
              powerChannel: new ChannelAddress("_sum", "EssSoc"),
            }, {
              name: "EssCharge",
              powerChannel: new ChannelAddress("_sum", "EssActivePower"),
              energyChannel: new ChannelAddress("_sum", "EssDcChargeEnergy"),
            }, {
              name: "EssDischarge",
              powerChannel: new ChannelAddress("_sum", "EssActivePower"),
              energyChannel: new ChannelAddress("_sum", "EssDcDischargeEnergy"),
            });
            break;
          case "Common_Selfconsumption":
          case "Common_Production":
            NEW_OBJ.PUSH({
              name: "ProductionActivePower",
              powerChannel: new ChannelAddress("_sum", "ProductionActivePower"),
              energyChannel: new ChannelAddress("_sum", "ProductionActiveEnergy"),
            }, {
              name: "ProductionDcActual",
              powerChannel: new ChannelAddress("_sum", "ProductionDcActualPower"),
              energyChannel: new ChannelAddress("_sum", "ProductionActiveEnergy"),
            });
            break;
        }

        ARR.PUSH(...newObj);
        return arr;
      }, []);

    return {
      input: input,
      output: (data: HISTORY_UTILS.CHANNEL_DATA): HISTORY_UTILS.DISPLAY_VALUE[] => {
        return [
          {
            name: TRANSLATE.INSTANT("GENERAL.PRODUCTION"),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => ENERGY_VALUES.RESULT.DATA["_sum/ProductionActiveEnergy"],
            converter: () => data["ProductionActivePower"],
            color: CHART_CONSTANTS.COLORS.BLUE,
            stack: 0,
            hiddenOnInit: chartType == "line" ? false : true,
            order: 1,
          },

          // DirectConsumption, displayed in stack 1 & 2, only one legenItem
          ...[chartType === "bar" && {
            name: TRANSLATE.INSTANT("GENERAL.DIRECT_CONSUMPTION"),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
              return UTILS.SUBTRACT_SAFELY(ENERGY_VALUES.RESULT.DATA["_sum/ProductionActiveEnergy"], ENERGY_VALUES.RESULT.DATA["_sum/GridSellActiveEnergy"], ENERGY_VALUES.RESULT.DATA["_sum/EssDcChargeEnergy"]);
            },
            converter: () =>
              data["ProductionActivePower"]?.map((value, index) => UTILS.SUBTRACT_SAFELY(value, data["GridSell"][index], data["EssCharge"][index]))
                ?.map(value => HISTORY_UTILS.VALUE_CONVERTER.NEGATIVE_AS_ZERO(value)),
            color: CHART_CONSTANTS.COLORS.ORANGE,
            stack: [1, 2],
            order: 2,
          }],

          // Charge Power
          {
            name: TRANSLATE.INSTANT("GENERAL.CHARGE"),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => ENERGY_VALUES.RESULT.DATA["_sum/EssDcChargeEnergy"],
            converter: () => chartType === "line" //
              ? data["EssCharge"]?.map((value, index) => {
                return HISTORY_UTILS.VALUE_CONVERTER.POSITIVE_AS_ZERO_AND_INVERT_NEGATIVE(UTILS.SUBTRACT_SAFELY(value, data["ProductionDcActual"]?.[index]));
              }) : data["EssCharge"],
            color: CHART_CONSTANTS.COLORS.GREEN,
            stack: 1,
            ...(chartType === "line" && { order: 6 }),
          },

          // Discharge Power
          {
            name: TRANSLATE.INSTANT("GENERAL.DISCHARGE"),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => ENERGY_VALUES.RESULT.DATA["_sum/EssDcDischargeEnergy"],
            converter: () => {
              return chartType === "line" ?
                data["EssDischarge"]?.map((value, index) => {
                  return HISTORY_UTILS.VALUE_CONVERTER.NEGATIVE_AS_ZERO(UTILS.SUBTRACT_SAFELY(value, data["ProductionDcActual"]?.[index]));
                }) : data["EssDischarge"];
            },
            color: CHART_CONSTANTS.COLORS.RED,
            stack: 2,
            ...(chartType === "line" && { order: 5 }),
          },

          // Sell to grid
          {
            name: TRANSLATE.INSTANT("GENERAL.GRID_SELL_ADVANCED"),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => ENERGY_VALUES.RESULT.DATA["_sum/GridSellActiveEnergy"],
            converter: () => data["GridSell"],
            color: CHART_CONSTANTS.COLORS.PURPLE,
            stack: 1,
            ...(chartType === "line" && { order: 4 }),
          },

          // Buy from Grid
          {
            name: TRANSLATE.INSTANT("GENERAL.GRID_BUY_ADVANCED"),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => ENERGY_VALUES.RESULT.DATA["_sum/GridBuyActiveEnergy"],
            converter: () => data["GridBuy"],
            color: CHART_CONSTANTS.COLORS.BLUE_GREY,
            stack: 2,
            ...(chartType === "line" && { order: 2 }),
          },

          // Consumption
          {
            name: TRANSLATE.INSTANT("GENERAL.CONSUMPTION"),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => ENERGY_VALUES.RESULT.DATA["_sum/ConsumptionActiveEnergy"],
            converter: () => data["Consumption"],
            color: CHART_CONSTANTS.COLORS.YELLOW,
            stack: 3,
            hiddenOnInit: chartType == "line" ? false : true,
            ...(chartType === "line" && { order: 0 }),
          },
          ...(chartType === "line" ?
            [{
              name: TRANSLATE.INSTANT("GENERAL.SOC"),
              converter: () => data["EssSoc"]?.map(value => UTILS.MULTIPLY_SAFELY(value, 1000)),
              color: "rgb(189, 195, 199)",
              borderDash: [10, 10],
              yAxisId: CHART_AXIS.RIGHT,
              stack: 1,
            } as HISTORY_UTILS.DISPLAY_VALUE] : []),
        ];
      },
      tooltip: {
        formatNumber: "1.0-2",
        afterTitle: (stack: string) => {

          if (chartType === "bar") {
            if (stack === "1") {
              return TRANSLATE.INSTANT("GENERAL.PRODUCTION");
            } else if (stack === "2") {
              return TRANSLATE.INSTANT("GENERAL.CONSUMPTION");
            }
          }
          return null;
        },
      },
      yAxes: [

        // Left YAxis
        {
          unit: YAXIS_TYPE.ENERGY,
          position: "left",
          yAxisId: CHART_AXIS.LEFT,
        },

        // Right Yaxis, only shown for line-chart
        ...(chartType === "line" ? [{
          unit: YAXIS_TYPE.PERCENTAGE,
          customTitle: "%",
          position: "right" as const,
          yAxisId: CHART_AXIS.RIGHT,
          displayGrid: false,
        }] : []),
      ],
      normalizeOutputData: true,
    };
  }

  public override getChartData() {
    return CHART_COMPONENT.GET_CHART_DATA(THIS.CONFIG, THIS.CHART_TYPE, THIS.TRANSLATE);
  }

  protected override getChartHeight(): number {
    return THIS.SERVICE.DEVICE_HEIGHT / 2;
  }

}
