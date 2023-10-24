import { Component } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { AbstractHistoryChart } from 'src/app/shared/genericComponents/chart/abstracthistorychart';
import { QueryHistoricTimeseriesEnergyResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { ChartAxis, HistoryUtils, YAxisTitle } from 'src/app/shared/service/utils';
import { ChannelAddress, EdgeConfig, Utils } from 'src/app/shared/shared';

@Component({
  selector: 'energychart',
  templateUrl: '../../../../../shared/genericComponents/chart/abstracthistorychart.html'
})
export class ChartComponent extends AbstractHistoryChart {

  public override getChartData() {
    return ChartComponent.getChartData(this.config, this.chartType, this.translate);
  }

  public static getChartData(config: EdgeConfig, chartType: 'line' | 'bar', translate: TranslateService): HistoryUtils.ChartData {
    let input: HistoryUtils.InputChannel[] =
      config.widgets.classes.reduce((arr: HistoryUtils.InputChannel[], key) => {
        let newObj = [];
        switch (key) {

          case 'Energymonitor':
          case 'Consumption':
            newObj.push({
              name: 'Consumption',
              powerChannel: new ChannelAddress('_sum', 'ConsumptionActivePower'),
              energyChannel: new ChannelAddress('_sum', 'ConsumptionActiveEnergy')
            });
            break;
          case 'Common_Autarchy':
          case 'Grid':
            newObj.push({
              name: 'GridBuy',
              powerChannel: new ChannelAddress('_sum', 'GridActivePower'),
              energyChannel: new ChannelAddress('_sum', 'GridBuyActiveEnergy'),
              ...(chartType === 'line' && { converter: HistoryUtils.ValueConverter.NEGATIVE_AS_ZERO })
            }, {
              name: 'GridSell',
              powerChannel: new ChannelAddress('_sum', 'GridActivePower'),
              energyChannel: new ChannelAddress('_sum', 'GridSellActiveEnergy'),
              ...(chartType === 'line' && { converter: HistoryUtils.ValueConverter.POSITIVE_AS_ZERO_AND_INVERT_NEGATIVE })
            });
            break;
          case 'Storage':
            newObj.push({
              name: 'EssSoc',
              powerChannel: new ChannelAddress('_sum', 'EssSoc')
            }, {
              name: 'EssCharge',
              powerChannel: new ChannelAddress('_sum', 'EssActivePower'),
              energyChannel: new ChannelAddress('_sum', 'EssDcChargeEnergy')
            }, {
              name: 'EssDischarge',
              powerChannel: new ChannelAddress('_sum', 'EssActivePower'),
              energyChannel: new ChannelAddress('_sum', 'EssDcDischargeEnergy')
            });
            break;
          case 'Common_Selfconsumption':
          case 'Common_Production':
            newObj.push({
              name: 'ProductionActivePower',
              powerChannel: new ChannelAddress('_sum', 'ProductionActivePower'),
              energyChannel: new ChannelAddress('_sum', 'ProductionActiveEnergy')
            }, {
              name: 'ProductionDcActual',
              powerChannel: new ChannelAddress('_sum', 'ProductionDcActualPower'),
              energyChannel: new ChannelAddress('_sum', 'ProductionActiveEnergy')
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
            name: translate.instant('General.production'),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
              return energyValues.result.data['_sum/ProductionActiveEnergy'];
            },
            converter: () => {
              return data['ProductionActivePower'];
            },
            color: 'rgb(45,143,171)',
            stack: 0,
            hiddenOnInit: chartType == 'line' ? false : true,
            order: 1
          },

          // DirectConsumption, displayed in stack 1 & 2, only one legenItem
          ...[chartType === 'bar' && {
            name: translate.instant('General.directConsumption'),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
              return Utils.subtractSafely(energyValues.result.data['_sum/ProductionActiveEnergy'], energyValues.result.data['_sum/GridSellActiveEnergy'], energyValues.result.data['_sum/EssDcChargeEnergy']);
            },
            converter: () =>
              data['ProductionActivePower']?.map((value, index) => Utils.subtractSafely(value, data['GridSell'][index], data['EssCharge'][index]))
                ?.map(value => HistoryUtils.ValueConverter.NEGATIVE_AS_ZERO(value)),
            color: 'rgb(244,164,96)',
            stack: [1, 2],
            order: 2
          }],

          // Charge Power
          {
            name: translate.instant('General.chargePower'),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
              return energyValues.result.data['_sum/EssDcChargeEnergy'];
            },
            converter: () => {
              return chartType === 'line' ?
                data['EssCharge']?.map((value, index) => {
                  return HistoryUtils.ValueConverter.POSITIVE_AS_ZERO_AND_INVERT_NEGATIVE(Utils.subtractSafely(value, data['ProductionDcActual']?.[index]));
                }) : data['EssCharge'];
            },
            color: 'rgb(0,223,0)',
            stack: 1,
            ...(chartType === 'line' && { order: 6 })
          },

          // Discharge Power
          {
            name: translate.instant('General.dischargePower'),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
              return energyValues.result.data['_sum/EssDcDischargeEnergy'];
            },
            converter: () => {
              return chartType === 'line' ?
                data['EssDischarge']?.map((value, index) => {
                  return HistoryUtils.ValueConverter.NEGATIVE_AS_ZERO(Utils.subtractSafely(value, data['ProductionDcActual']?.[index]));
                }) : data['EssDischarge'];
            },
            color: 'rgb(200,0,0)',
            stack: 2,
            ...(chartType === 'line' && { order: 5 })
          },

          // Sell to grid
          {
            name: translate.instant('General.gridSellAdvanced'),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
              return energyValues.result.data['_sum/GridSellActiveEnergy'];
            },
            converter: () => {
              return data['GridSell'];
            },
            color: 'rgb(0,0,200)',
            stack: 1,
            ...(chartType === 'line' && { order: 4 })
          },

          // Buy from Grid
          {
            name: translate.instant('General.gridBuyAdvanced'),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
              return energyValues.result.data['_sum/GridBuyActiveEnergy'];
            },
            converter: () => {
              return data['GridBuy'];
            },
            color: 'rgb(0,0,0)',
            stack: 2,
            ...(chartType === 'line' && { order: 2 })
          },

          // Consumption
          {
            name: translate.instant('General.consumption'),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
              return energyValues.result.data['_sum/ConsumptionActiveEnergy'];
            },
            converter: () => {
              return data['Consumption'];
            },
            color: 'rgb(253,197,7)',
            stack: 3,
            hiddenOnInit: chartType == 'line' ? false : true,
            ...(chartType === 'line' && { order: 0 })
          },
          ...[chartType === 'line' &&
          {
            name: translate.instant('General.soc'),
            converter: () => {
              return data['EssSoc']?.map(value => Utils.multiplySafely(value, 1000));
            },
            color: 'rgb(189, 195, 199)',
            borderDash: [10, 10],
            yAxisId: ChartAxis.RIGHT,
            stack: 1,
            customUnit: YAxisTitle.PERCENTAGE
          }]
        ];
      },
      tooltip: {
        formatNumber: '1.0-2',
        afterTitle: (stack: string) => {
          if (stack === "1") {
            return translate.instant('General.production');
          } else if (stack === "2") {
            return translate.instant('General.consumption');
          }
          return null;
        }
      },
      yAxes: [

        // Left YAxis
        {
          unit: YAxisTitle.ENERGY,
          position: 'left',
          yAxisId: ChartAxis.LEFT
        },

        // Right Yaxis, only shown for line-chart
        (chartType === 'line' && {
          unit: YAxisTitle.PERCENTAGE,
          customTitle: '%',
          position: 'right',
          yAxisId: ChartAxis.RIGHT,
          displayGrid: false
        })
      ]
    };
  }

  protected override getChartHeight(): number {
    return this.service.deviceHeight / 2;
  }
}