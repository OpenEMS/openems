import { Component } from '@angular/core';
import { AbstractHistoryChart } from 'src/app/shared/genericComponents/chart/abstracthistorychart';
import { QueryHistoricTimeseriesEnergyResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { ChartAxis, HistoryUtils, YAxisTitle } from 'src/app/shared/service/utils';
import { ChannelAddress, Utils } from 'src/app/shared/shared';

@Component({
  selector: 'autarchychart',
  templateUrl: '../../../../../shared/genericComponents/chart/abstracthistorychart.html'
})
export class ChartComponent extends AbstractHistoryChart {

  protected override getChartData(): HistoryUtils.ChartData {
    this.spinnerId = 'autarchy-chart';
    return {
      input:
        [{
          name: 'Consumption',
          powerChannel: ChannelAddress.fromString('_sum/ConsumptionActivePower'),
          energyChannel: ChannelAddress.fromString('_sum/ConsumptionActiveEnergy')
        },
        {
          name: 'GridBuy',
          powerChannel: ChannelAddress.fromString('_sum/GridActivePower'),
          energyChannel: ChannelAddress.fromString('_sum/GridBuyActiveEnergy'),
          converter: HistoryUtils.ValueConverter.NON_NULL_OR_NEGATIVE
        }],
      output: (data: HistoryUtils.ChannelData) => {
        return [{
          name: this.translate.instant('General.autarchy'),
          nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => {
            return Utils.calculateAutarchy(energyValues?.result.data['_sum/GridBuyActiveEnergy'] ?? null, energyValues?.result.data['_sum/ConsumptionActiveEnergy'] ?? null);
          },
          converter: () => {
            return data['Consumption']
              ?.map((value, index) =>
                Utils.calculateAutarchy(data['GridBuy'][index], value)
              );
          },
          color: 'rgb(0,152,204)'
        }];
      },
      tooltip: {
        formatNumber: '1.0-0'
      },
      yAxes: [{
        unit: YAxisTitle.PERCENTAGE,
        position: 'left',
        yAxisId: ChartAxis.LEFT
      }]
    };
  }
}