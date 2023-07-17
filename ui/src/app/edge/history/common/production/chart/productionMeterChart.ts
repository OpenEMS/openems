import { Component } from '@angular/core';
import { AbstractHistoryChart } from 'src/app/shared/genericComponents/chart/abstracthistorychart';
import { QueryHistoricTimeseriesEnergyResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { ChartAxis, HistoryUtils, YAxisTitle } from 'src/app/shared/service/utils';

import { ChannelAddress } from '../../../../../shared/shared';

/** Will be used in the Future again */
@Component({
  selector: 'productionMeterchart',
  templateUrl: '../../../../../shared/genericComponents/chart/abstracthistorychart.html'
})
export class ProductionMeterChartComponent extends AbstractHistoryChart {

  protected override getChartData(): HistoryUtils.ChartData {
    let channels: HistoryUtils.InputChannel[] = [{
      name: 'ActivePower',
      powerChannel: ChannelAddress.fromString(this.component.id + '/ActivePower'),
      energyChannel: ChannelAddress.fromString(this.component.id + '/ActiveProductionEnergy'),
      converter: (data) => data != null ? data : null
    }
    ];

    // Phase 1 to 3
    for (let i = 1; i < 4; i++) {
      channels.push({
        name: 'ActivePowerL' + i,
        powerChannel: ChannelAddress.fromString(this.component.id + '/ActivePowerL' + i),
        energyChannel: ChannelAddress.fromString(this.component.id + '/ActiveProductionEnergyL' + i)
      });
    }
    return {
      input: channels,
      output: (data: HistoryUtils.ChannelData) => {
        let datasets: HistoryUtils.DisplayValues[] = [];
        datasets.push({
          name: this.translate.instant('General.production'),
          nameSuffix: (energyPeriodResponse: QueryHistoricTimeseriesEnergyResponse) => {
            return energyPeriodResponse?.result.data[this.component.id + '/ActiveProductionEnergy'] ?? null;
          },
          converter: () => {
            return data['ActivePower'];
          },
          color: 'rgb(0,152,204)'
        });
        if (this.showPhases) {

          // Phase 1 to 3
          for (let i = 1; i < 4; i++) {
            datasets.push({
              name: "Erzeugung Phase L" + i,
              converter: () => {
                return data['ActivePowerL' + i] ?? null;
              },
              color: this.phaseColors[i - 1]
            });
          }
        }
        return datasets;
      },
      tooltip: {
        formatNumber: '1.1-2'
      },
      yAxes: [{
        unit: YAxisTitle.ENERGY,
        position: 'left',
        yAxisId: ChartAxis.LEFT
      }]
    };
  }
}