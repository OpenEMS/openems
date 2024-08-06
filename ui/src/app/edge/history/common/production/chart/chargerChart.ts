// @ts-strict-ignore
import { Component } from '@angular/core';
import { AbstractHistoryChart } from 'src/app/shared/components/chart/abstracthistorychart';
import { QueryHistoricTimeseriesEnergyResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { ChartAxis, HistoryUtils, YAxisTitle } from 'src/app/shared/service/utils';

import { ChannelAddress } from '../../../../../shared/shared';

@Component({
  selector: 'productionChargerChart',
  templateUrl: '../../../../../shared/components/chart/abstracthistorychart.html',
})
export class ChargerChartComponent extends AbstractHistoryChart {

  protected override getChartData(): HistoryUtils.ChartData {
    return {
      input: [{
        name: 'ActualPower',
        powerChannel: ChannelAddress.fromString(this.component.id + '/ActualPower'),
        energyChannel: ChannelAddress.fromString(this.component.id + '/ActualEnergy'),
        converter: (data) => data != null ? data : null,
      }],
      output: (data: HistoryUtils.ChannelData) => {
        return [
          {
            name: this.translate.instant('General.production'),
            converter: () => { return data['ActualPower']; },
            nameSuffix: (energyResponse: QueryHistoricTimeseriesEnergyResponse) => {
              return energyResponse.result.data[this.component.id + '/ActualEnergy'];
            },
            color: 'rgb(0,152,204)',
          },
        ];
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
