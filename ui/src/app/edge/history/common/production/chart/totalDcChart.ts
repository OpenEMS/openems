import { Component } from '@angular/core';
import { AbstractHistoryChart } from 'src/app/shared/genericComponents/chart/abstracthistorychart';
import { QueryHistoricTimeseriesEnergyResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { ChartAxis, HistoryUtils, YAxisTitle } from 'src/app/shared/service/utils';

import { ChannelAddress } from '../../../../../shared/shared';

@Component({
  selector: 'totalDcChart',
  templateUrl: '../../../../../shared/genericComponents/chart/abstracthistorychart.html',
})
export class TotalDcChartComponent extends AbstractHistoryChart {

  protected override getChartData(): HistoryUtils.ChartData {
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
      output: (data: HistoryUtils.ChannelData) => {
        return [{
          name: this.translate.instant('General.production'),
          nameSuffix: (energyResponse: QueryHistoricTimeseriesEnergyResponse) => {
            return energyResponse.result.data['_sum/ProductionDcActiveEnergy'];
          },
          converter: () => {
            return data['ProductionDcActual'] ?? null;
          },
          strokeThroughHiddingStyle: false,
          color: 'rgb(0,152,204)',
        }];
      },
      tooltip: {
        formatNumber: '1.1-2',
      },
      yAxes: [{
        unit: YAxisTitle.ENERGY,
        position: 'left',
        yAxisId: ChartAxis.LEFT,
      }],
    };
  }
}
