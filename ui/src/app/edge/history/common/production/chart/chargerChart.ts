import { Component } from '@angular/core';
import { AbstractHistoryChart } from 'src/app/shared/genericComponents/chart/abstracthistorychart';
import { HistoryUtils } from 'src/app/shared/service/utils';
import { ChannelAddress } from '../../../../../shared/shared';
import { ChannelFilter, ChartData, YAxisTitle } from '../../../shared';

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
          energyChannel: ChannelAddress.fromString(this.component.id + '/ActualEnergy'),
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
        formatNumber: '1.1-2'
      },
      unit: YAxisTitle.ENERGY,
    }
  }
}