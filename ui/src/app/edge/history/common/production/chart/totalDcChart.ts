import { Component } from '@angular/core';
import { AbstractHistoryChart } from 'src/app/shared/genericComponents/chart/abstracthistorychart';
import { QueryHistoricTimeseriesEnergyResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { HistoryUtils } from 'src/app/shared/service/utils';

import { ChannelAddress } from '../../../../../shared/shared';
import { ChannelData, ChartData, YAxisTitle } from '../../../shared';

@Component({
  selector: 'totalDcChart',
  templateUrl: '../../../../../shared/genericComponents/chart/abstracthistorychart.html',
})
export class TotalDcChartComponent extends AbstractHistoryChart {

  protected override getChartData(): ChartData {
    return {
      input:
        [
          {
            name: 'ProductionDcActual',
            powerChannel: ChannelAddress.fromString('_sum/ProductionDcActualPower'),
            energyChannel: ChannelAddress.fromString('_sum/ProductionDcActiveEnergy'),
            converter: (data) => data != null ? data : null,
          },
        ],
      output: (data: ChannelData) => {
        return [{
          name: this.translate.instant('General.production'),
          nameSuffix: (energyResponse: QueryHistoricTimeseriesEnergyResponse) => {
            return energyResponse.result.data['_sum/ProductionDcActiveEnergy'];
          },
          converter: () => {
            return data['ProductionDcActual'] ?? null;
          },
          strokeThroughHiddingStyle: false,
          color: 'rgb(0,152,204)'
        }];
      },
      tooltip: {
        // unit: 'kW',
        formatNumber: '1.1-2'
      },
      unit: YAxisTitle.ENERGY,
    };
  }
}