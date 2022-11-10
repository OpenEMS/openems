import { Component } from '@angular/core';
import { AbstractHistoryChart } from 'src/app/shared/genericComponents/chart/abstracthistorychart';
import { ChannelAddress, Utils } from '../../../../../shared/shared';
import { ChannelFilter, ChartData } from '../../../shared';

@Component({
  selector: 'autarchychart',
  templateUrl: '../../../../../shared/genericComponents/chart/abstracthistorychart.html',
})
export class ChartComponent extends AbstractHistoryChart {

  protected override getChartData(): ChartData {
    this.spinnerId = 'autarchy-chart';
    return {
      channel:
        [{
          name: 'Consumption',
          powerChannel: ChannelAddress.fromString('_sum/ConsumptionActivePower'),
          energyChannel: ChannelAddress.fromString('_sum/ConsumptionActiveEnergy'),
          filter: ChannelFilter.NOT_NULL,
        },
        {
          name: 'GridBuy',
          powerChannel: ChannelAddress.fromString('_sum/GridActivePower'),
          energyChannel: ChannelAddress.fromString('_sum/GridBuyActiveEnergy'),
          filter: ChannelFilter.NOT_NULL_OR_NEGATIVE,
        }],
      displayValues: (channel: { name: string, data: number[] }[]) => {
        return [{
          name: this.translate.instant('General.autarchy'),
          setValue: () => {
            return channel.find(element => element.name == 'Consumption')?.data
              ?.map((value, index) =>
                Utils.calculateAutarchy(channel.find(element => element.name == 'GridBuy')?.data[index], value)
              )
          },
          color: 'rgb(0,152,204)'
        }]
      },
      tooltip: {
        unit: '%',
        formatNumber: '1.0-0'
      },
      yAxisTitle: this.translate.instant("General.percentage"),
    }
  }
}