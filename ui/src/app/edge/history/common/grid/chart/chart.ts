import { Component } from '@angular/core';
import { AbstractHistoryChart } from 'src/app/shared/genericComponents/chart/abstracthistorychart';
import { QueryHistoricTimeseriesEnergyResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChartAxis, HistoryUtils, YAxisTitle } from 'src/app/shared/service/utils';
import { ChannelAddress } from 'src/app/shared/shared';

@Component({
  selector: 'gridchart',
  templateUrl: '../../../../../shared/genericComponents/chart/abstracthistorychart.html'
})
export class ChartComponent extends AbstractHistoryChart {

  protected override getChartData(): HistoryUtils.ChartData {
    this.spinnerId = 'grid-chart';

    let input: DefaultTypes.History.InputChannel[] = [
      {
        name: 'GridSell',
        powerChannel: ChannelAddress.fromString('_sum/GridActivePower'),
        energyChannel: ChannelAddress.fromString('_sum/GridSellActiveEnergy'),
        ...(this.chartType === 'line' && { converter: HistoryUtils.ValueConverter.ONLY_NEGATIVE_AND_NEGATIVE_AS_POSITIVE })
      },
      {
        name: 'GridBuy',
        powerChannel: ChannelAddress.fromString('_sum/GridActivePower'),
        energyChannel: ChannelAddress.fromString('_sum/GridBuyActiveEnergy'),
        converter: HistoryUtils.ValueConverter.NEGATIVE_AS_ZERO
      }
    ];

    ['L1', 'L2', 'L3'].forEach(phase => {
      input.push({
        name: 'GridActivePower' + phase,
        powerChannel: ChannelAddress.fromString('_sum/GridActivePower' + phase)
      });
    });

    return {
      input: input,
      output: (data: DefaultTypes.History.ChannelData) => {

        let datasets: DefaultTypes.History.DisplayValues[] = [];
        datasets.push(
          {
            name: this.translate.instant('General.gridBuy'),
            nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
              return energyValues?.result.data['_sum/GridBuyActiveEnergy'] ?? null;
            },
            converter: () => {
              return data['GridBuy'];
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
              return data['GridSell'];
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
            stack: 3
          });
        });

        return datasets;
      },
      tooltip: {
        formatNumber: '1.0-0'
      },
      yAxes: [{
        unit: YAxisTitle.ENERGY,
        position: 'left',
        yAxisId: ChartAxis.LEFT
      }]
    };
  }
}