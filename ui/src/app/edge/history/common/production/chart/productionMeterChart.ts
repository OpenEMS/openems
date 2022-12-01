import { Component } from '@angular/core';
import { AbstractHistoryChart } from 'src/app/shared/genericComponents/chart/abstracthistorychart';
import { HistoryUtils } from 'src/app/shared/service/utils';
import { ChannelAddress } from '../../../../../shared/shared';
import { ChannelFilter, Channels, ChartData } from '../../../shared';

@Component({
  selector: 'productionMeterchart',
  templateUrl: '../../../../../shared/genericComponents/chart/abstracthistorychart.html',
})
export class ProductionMeterChartComponent extends AbstractHistoryChart {

  protected override getChartData(): ChartData {
    let channels: Channels[] = [{
      name: 'ActivePower',
      powerChannel: ChannelAddress.fromString(this.component.id + '/ActivePower'),
      filter: ChannelFilter.NOT_NULL,
    },
    ];

    // Phase 1 to 3
    for (let i = 1; i < 4; i++) {
      channels.push({
        name: 'ActivePowerL' + i,
        powerChannel: ChannelAddress.fromString(this.component.id + '/ActivePowerL' + i),
        filter: ChannelFilter.NOT_NULL,
      })
    }
    return {
      channel: channels,
      displayValues: (channel: { name: string, data: number[] }[]) => {
        let datasets = [];
        datasets.push({
          name: this.translate.instant('General.production'),
          setValue: () => {
            return HistoryUtils.CONVERT_WATT_TO_KILOWATT(channel.find(element => element.name == 'ActivePower')?.data)
          },
          color: 'rgb(0,152,204)'
        })
        if (this.showPhases) {

          // Phase 1 to 3
          for (let i = 1; i < 4; i++) {
            datasets.push({
              name: "Erzeugung Phase L" + i,
              setValue: () => {
                return HistoryUtils.CONVERT_WATT_TO_KILOWATT(channel.find(element => element.name == ('ActivePowerL' + i))?.data) ?? null
              },
              color: this.phaseColors[i - 1]
            })
          }
        }
        return datasets;
      },
      tooltip: {
        unit: 'kW',
        formatNumber: '1.1-2'
      },
      yAxisTitle: "kW",
    }
  }
}