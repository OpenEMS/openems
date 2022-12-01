import { Component } from '@angular/core';
import { AbstractHistoryChart } from 'src/app/shared/genericComponents/chart/abstracthistorychart';
import { HistoryUtils } from 'src/app/shared/service/utils';
import { ChannelAddress } from '../../../../../shared/shared';
import { ChannelFilter, ChartData } from '../../../shared';

@Component({
  selector: 'totalDcChart',
  templateUrl: '../../../../../shared/genericComponents/chart/abstracthistorychart.html',
})
export class TotalDcChartComponent extends AbstractHistoryChart {

  protected override getChartData(): ChartData {
    return {
      channel:
        [
          {
            name: 'ProductionDcActual',
            powerChannel: ChannelAddress.fromString('_sum/ProductionDcActualPower'),
            energyChannel: ChannelAddress.fromString('_sum/ProductionDcActiveEnergy'),
            filter: ChannelFilter.NOT_NULL,
          },
        ],
      displayValues: (channel: { name: string, data: number[] }[]) => {
        return [{
          name: this.translate.instant('General.production'),
          setValue: () => {
            return HistoryUtils.CONVERT_WATT_TO_KILOWATT(channel.find(element => element.name == 'ProductionDcActual')?.data) ?? null
          },
          color: 'rgb(0,152,204)'
        }]
      },
      tooltip: {
        unit: 'kW',
        formatNumber: '1.1-2'
      },
      yAxisTitle: 'kW',
    }
  }
}