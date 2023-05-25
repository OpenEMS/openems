import { Component } from '@angular/core';
import { AbstractHistoryChart } from 'src/app/shared/genericComponents/chart/abstracthistorychart';
import { QueryHistoricTimeseriesEnergyResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { HistoryUtils } from 'src/app/shared/service/utils';
import { ChannelAddress } from 'src/app/shared/shared';

@Component({
  selector: 'gridchart',
  templateUrl: '../../../../../shared/genericComponents/chart/abstracthistorychart.html',
})
export class ChartComponent extends AbstractHistoryChart {

  protected override getChartData(): HistoryUtils.ChartData {
    this.spinnerId = 'grid-chart';
    return {
      input:
        [{
          name: 'GridSell',
          powerChannel: ChannelAddress.fromString('_sum/GridActivePower'),
          energyChannel: ChannelAddress.fromString('_sum/GridSellActiveEnergy'),
          // TODO energyChannel has positive values, powerChannel needs only negative values
          ...(this.chartType === 'line' && { converter: HistoryUtils.ValueConverter.ONLY_NEGATIVE_AND_NEGATIVE_AS_POSITIVE })
        },
        {
          name: 'GridBuy',
          powerChannel: ChannelAddress.fromString('_sum/GridActivePower'),
          energyChannel: ChannelAddress.fromString('_sum/GridBuyActiveEnergy'),
          converter: HistoryUtils.ValueConverter.NEGATIVE_AS_ZERO,
        },
        {
          name: 'GridActivePowerL1',
          powerChannel: ChannelAddress.fromString('_sum/GridActivePowerL1'),
        },
        {
          name: 'GridActivePowerL2',
          powerChannel: ChannelAddress.fromString('_sum/GridActivePowerL2'),
        },
        {
          name: 'GridActivePowerL3',
          powerChannel: ChannelAddress.fromString('_sum/GridActivePowerL3'),
        },
        ],
      output: (data: HistoryUtils.ChannelData) => {

        let datasets: HistoryUtils.DisplayValues[] = [];
        datasets.push(
          {
            name: this.translate.instant('General.gridBuy'),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
              return energyValues?.result.data['_sum/GridBuyActiveEnergy'] ?? null;
            },
            converter: () => {
              return data['GridBuy']
            },
            color: 'rgb(0,0,0)',
            stack: 0
          });

        datasets.push(
          {
            name: this.translate.instant('General.gridSell'),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
              return energyValues?.result.data['_sum/GridSellActiveEnergy'] ?? null;
            },
            converter: () => {
              return data['GridSell']
            },
            // TODO create Color class
            color: 'rgba(0,0,200)',
            stack: 1
          });

        if (!this.showPhases) {
          return datasets;
        }

        ['L1', 'L2', 'L3'].forEach((phase, index) => {
          datasets.push({
            name: "Phase " + phase,
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
              return energyValues?.result.data['_sum/GridActivePower' + phase];
            },
            converter: () => {
              return data['GridActivePower' + phase] ?? null;
            },
            color: this.phaseColors[index],
            stack: 3,
          });
        });

        return datasets;
      },
      tooltip: {
        formatNumber: '1.0-0',
      },
      unit: HistoryUtils.YAxisTitle.ENERGY,
    };
  }
}