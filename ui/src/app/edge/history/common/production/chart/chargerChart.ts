import { Component } from '@angular/core';
import { AbstractHistoryChart } from 'src/app/shared/genericComponents/chart/abstracthistorychart';
import { QueryHistoricTimeseriesEnergyResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { HistoryUtils } from 'src/app/shared/service/utils';

import { ChannelAddress } from '../../../../../shared/shared';
import { ChannelData, ChartData, YAxisTitle } from '../../../shared';

@Component({
  selector: 'productionChargerChart',
  templateUrl: '../../../../../shared/genericComponents/chart/abstracthistorychart.html',
})
export class ChargerChartComponent extends AbstractHistoryChart {

  protected override getChartData(): ChartData {
    return {
      input: [{
        name: 'ActualPower',
        powerChannel: ChannelAddress.fromString(this.component.id + '/ActualPower'),
        energyChannel: ChannelAddress.fromString(this.component.id + '/ActualEnergy'),
        converter: (data) => data != null ? data : null,
      }],
      // name: string,
      // /** suffix to the name */
      // nameSuffix?: (energyValues: QueryHistoricTimeseriesEnergyResponse) => number | string,
      // /** Convert the values to be displayed in Chart */
      // converter: () => number[],
      // /** If dataset should be hidden on Init */
      // hiddenOnInit?: boolean,
      // /** default: true, stroke through label for hidden dataset */
      // noStrokeThroughLegendIfHidden?: boolean,
      // /** color in rgb-Format */
      // color: string,
      // /** the stack for barChart */
      // stack?: number,
      output: (data: ChannelData) => {
        return [
          {
            name: this.translate.instant('General.production'),
            converter: () => { return data['ActualPower'] },
            nameSuffix: (energyResponse: QueryHistoricTimeseriesEnergyResponse) => {
              return energyResponse.result.data[this.component.id + '/ActualEnergy']
            },
            // setValue: () => data['ActualPower'],
            color: 'rgb(0,152,204)',

          }
        ]
      },
      tooltip: {
        formatNumber: '1.1-2'
      },
      unit: YAxisTitle.ENERGY,
    }
  }
}