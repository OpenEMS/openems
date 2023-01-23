import { Component } from '@angular/core';
import { AbstractHistoryChart } from 'src/app/shared/genericComponents/chart/abstracthistorychart';
import { QueryHistoricTimeseriesEnergyResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse';

import { ChannelAddress, Utils } from '../../../../../shared/shared';
import { ChannelFilter, ChartData, YAxisTitle } from '../../../shared';

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
      displayValues: (data: { [name: string]: number[] }) => {
        return [{
          name: this.translate.instant('General.autarchy'),
          nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
            return Utils.calculateAutarchy(energyValues?.result.data['_sum/GridBuyActiveEnergy'] ?? null, energyValues?.result.data['_sum/ConsumptionActiveEnergy'] ?? null)
          },
          setValue: () => {
            return data['Consumption']
              ?.map((value, index) =>
                Utils.calculateAutarchy(data['GridBuy'][index], value)
              )
          },
          color: 'rgb(0,152,204)'
        }]
      },
      tooltip: {
        formatNumber: '1.0-0'
      },
      unit: YAxisTitle.PERCENTAGE,
    }
  }
}