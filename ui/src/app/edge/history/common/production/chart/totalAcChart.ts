import { Component } from '@angular/core';
import { AbstractHistoryChart } from 'src/app/shared/genericComponents/chart/abstracthistorychart';
import { HistoryUtils, Utils } from 'src/app/shared/service/utils';
import { ChannelAddress } from '../../../../../shared/shared';
import { ChannelFilter, ChartData, DisplayValues } from '../../../shared';

@Component({
  selector: 'productionTotalAcChart',
  templateUrl: '../../../../../shared/genericComponents/chart/abstracthistorychart.html',
})
export class TotalAcChartComponent extends AbstractHistoryChart {

  protected override getChartData(): ChartData {
    return {
      channel:
        [
          {
            name: 'ProductionAcActivePower',
            powerChannel: ChannelAddress.fromString('_sum/ProductionAcActivePower'),
            energyChannel: ChannelAddress.fromString('_sum/ProductionAcActiveEnergy'),
            filter: ChannelFilter.NOT_NULL_OR_NEGATIVE,
          },
          {
            name: 'ProductionAcActivePowerL1',
            powerChannel: ChannelAddress.fromString('_sum/ProductionAcActivePowerL1'),
            filter: ChannelFilter.NOT_NULL_OR_NEGATIVE,
          },
          {
            name: 'ProductionAcActivePowerL2',
            powerChannel: ChannelAddress.fromString('_sum/ProductionAcActivePowerL2'),
            filter: ChannelFilter.NOT_NULL_OR_NEGATIVE,
          },
          {
            name: 'ProductionAcActivePowerL3',
            powerChannel: ChannelAddress.fromString('_sum/ProductionAcActivePowerL3'),
            filter: ChannelFilter.NOT_NULL_OR_NEGATIVE,
          }
        ],
      displayValues: (channels: { name: string, data: number[] }[]) => {
        let datasets: DisplayValues[] = []

        console.log("channel", channels);
        datasets.push({
          name: this.translate.instant("General.total"),
          setValue: () => {
            return HistoryUtils.CONVERT_WATT_TO_KILOWATT(channels.find(element => element.name == 'ProductionAcActivePower')?.data)
          },
          color: "rgb(0,152,204)",
          stack: 0
        })

        for (let i = 1; i < 4; i++) {
          datasets.push({
            name: "ProductionAcActivePowerL" + i,
            setValue: () => {
              if (!this.showPhases) {
                return null;
              }
              return HistoryUtils.CONVERT_WATT_TO_KILOWATT(channels.find(element => element.name == ('ProductionAcActivePowerL' + i))?.data) ?? null
            },
            color: 'rgb(' + this.phaseColors[i - 1] + ')',
          })
        }

        return datasets
      },
      tooltip: {
        unit: 'kW',
        formatNumber: '1.1-2'
      },
      yAxisTitle: "kW",
    }
  }
}