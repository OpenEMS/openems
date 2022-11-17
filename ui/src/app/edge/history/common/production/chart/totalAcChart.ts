import { Component } from '@angular/core';
import { AbstractHistoryChart } from 'src/app/shared/genericComponents/chart/abstracthistorychart';
import { HistoryUtils } from 'src/app/shared/service/utils';
import { ChannelAddress } from '../../../../../shared/shared';
import { ChannelFilter, ChartData } from '../../../shared';

@Component({
  selector: 'productionTotalAcChart',
  templateUrl: '../../../../../shared/genericComponents/chart/abstracthistorychart.html',
})
export class TotalAcChartComponent extends AbstractHistoryChart {

  protected override getChartData(): ChartData {
    this.spinnerId = 'productionTotalAcChart2-chart';
    return {
      channel:
        [
          {
            name: 'ProductionAcActivePower',
            powerChannel: ChannelAddress.fromString('_sum/ProductionAcActivePower'),
            energyChannel: ChannelAddress.fromString('_sum/ProductionAcActivePower'),
            filter: ChannelFilter.NOT_NULL_OR_NEGATIVE,
          },
          {
            name: 'ProductionAcActivePowerL1',
            powerChannel: ChannelAddress.fromString('_sum/ProductionAcActivePowerL1'),
            energyChannel: ChannelAddress.fromString('_sum/ProductionAcActivePowerL1'),
            filter: ChannelFilter.NOT_NULL_OR_NEGATIVE,
          },
          {
            name: 'ProductionAcActivePowerL2',
            powerChannel: ChannelAddress.fromString('_sum/ProductionAcActivePowerL2'),
            energyChannel: ChannelAddress.fromString('_sum/ProductionAcActivePowerL2'),
            filter: ChannelFilter.NOT_NULL_OR_NEGATIVE,
          },
          {
            name: 'ProductionAcActivePowerL3',
            powerChannel: ChannelAddress.fromString('_sum/ProductionAcActivePowerL3'),
            energyChannel: ChannelAddress.fromString('_sum/ProductionAcActivePowerL3'),
            filter: ChannelFilter.NOT_NULL_OR_NEGATIVE,
          }
        ],
      displayValues: (channel: { name: string, data: number[] }[]) => {
        return [{
          name: this.translate.instant("General.total"),
          setValue: () => {
            return HistoryUtils.CONVERT_WATT_TO_KILOWATT(channel.find(element => element.name == 'ProductionAcActivePower')?.data) ?? null
          },
          color: "rgb(0,152,204)",
        }]
      },
      tooltip: {
        unit: 'kW',
        formatNumber: '1.1-2'
      },
      yAxisTitle: this.translate.instant("General.percentage"),
    }
  }
}