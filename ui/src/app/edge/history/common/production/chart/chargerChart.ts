import { Component } from '@angular/core';
import { AbstractHistoryChart } from 'src/app/shared/genericComponents/chart/abstracthistorychart';
import { ChannelAddress } from '../../../../../shared/shared';
import { ChannelFilter, ChartData } from '../../../shared';

@Component({
  selector: 'productionChargerChart',
  templateUrl: '../../../../../shared/genericComponents/chart/abstracthistorychart.html',
})
export class ChargerChartComponent extends AbstractHistoryChart {

  protected override getChartData(): ChartData {
    return {
      channel:
        [{
          name: 'ActualPower',
          powerChannel: ChannelAddress.fromString(this.component.id + '/ActualPower'),
          filter: ChannelFilter.NOT_NULL,
        }],
      displayValues: (channel: { name: string, data: number[] }[]) => {
        return [{
          name: this.translate.instant('General.production'),
          setValue: () => {
            return channel.find(element => element.name == 'ActualPower')?.data ?? null
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