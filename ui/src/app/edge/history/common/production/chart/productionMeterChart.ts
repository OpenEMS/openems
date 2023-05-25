import { Component } from '@angular/core';
import { AbstractHistoryChart } from 'src/app/shared/genericComponents/chart/abstracthistorychart';
import { QueryHistoricTimeseriesEnergyResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChannelAddress } from '../../../../../shared/shared';

/** Will be used in the Future again */
@Component({
  selector: 'productionMeterchart',
  templateUrl: '../../../../../shared/genericComponents/chart/abstracthistorychart.html',
})
export class ProductionMeterChartComponent extends AbstractHistoryChart {

  protected override getChartData(): DefaultTypes.History.ChartData {
    let channels: DefaultTypes.History.InputChannel[] = [{
      name: 'ActivePower',
      powerChannel: ChannelAddress.fromString(this.component.id + '/ActivePower'),
      energyChannel: ChannelAddress.fromString(this.component.id + '/ActiveProductionEnergy'),
      converter: (data) => data != null ? data : null,
    },
    ];

    // Phase 1 to 3
    for (let i = 1; i < 4; i++) {
      channels.push({
        name: 'ActivePowerL' + i,
        powerChannel: ChannelAddress.fromString(this.component.id + '/ActivePowerL' + i),
        energyChannel: ChannelAddress.fromString(this.component.id + '/ActiveProductionEnergyL' + i),
      });
    }
    return {
      input: channels,
      output: (data: DefaultTypes.History.ChannelData) => {
        let datasets: DefaultTypes.History.DisplayValues[] = [];
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
      unit: DefaultTypes.History.YAxisTitle.ENERGY,
    };
  }
}