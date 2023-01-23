import { Component } from '@angular/core';
import { AbstractHistoryChart } from 'src/app/shared/genericComponents/chart/abstracthistorychart';
import { QueryHistoricTimeseriesEnergyResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { HistoryUtils, Utils } from 'src/app/shared/service/utils';

import { ChannelAddress } from '../../../../../shared/shared';
import { ChannelFilter, ChartData, DisplayValues, YAxisTitle } from '../../../shared';

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
      displayValues: (data: { [name: string]: number[] }) => {
        let datasets: DisplayValues[] = []

        datasets.push({
          name: this.translate.instant("General.total"),
          nameSuffix: (energyPeriodResponse: QueryHistoricTimeseriesEnergyResponse) => {
            return energyPeriodResponse.result.data['_sum/ProductionAcActiveEnergy'] ?? null
          },
          setValue: () => {
            return data['ProductionAcActivePower']
          },
          color: "rgb(0,152,204)",
          stack: 0
        })

        for (let i = 1; i < 4; i++) {
          datasets.push({
            name: "Phase L" + i,
            setValue: () => {
              if (!this.showPhases) {
                return null;
              }
              return data['ProductionAcActivePowerL' + i] ?? null
            },
            color: 'rgb(' + this.phaseColors[i - 1] + ')',
          })
        }

        return datasets
      },
      tooltip: {
        formatNumber: '1.1-2'
      },
      unit: YAxisTitle.ENERGY,
    }
  }
}