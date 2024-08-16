// @ts-strict-ignore
import { Component } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { AbstractHistoryChart } from 'src/app/shared/components/chart/abstracthistorychart';
import { Phase } from 'src/app/shared/components/shared/phase';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChartAxis, HistoryUtils, YAxisTitle } from 'src/app/shared/service/utils';
import { ChannelAddress } from 'src/app/shared/shared';

@Component({
  selector: 'gridDetailsChart',
  templateUrl: '../../../../../../shared/components/chart/abstracthistorychart.html',
})
export class ChartComponent extends AbstractHistoryChart {

  public static getChartData(translate: TranslateService): HistoryUtils.ChartData {
    return {
      input: [
        {
          name: 'GridActivePower',
          powerChannel: ChannelAddress.fromString('_sum/GridActivePower'),
        },
        ...Phase.THREE_PHASE.map((phase, index) => ({
          name: 'Phase' + phase,
          powerChannel: ChannelAddress.fromString('_sum/GridActivePower' + phase),
        })),
      ],
      output: (data: DefaultTypes.History.ChannelData) => {

        const datasets: DefaultTypes.History.DisplayValues[] =
          [
            {
              name: translate.instant('General.TOTAL'),
              converter: () => {
                return data['GridActivePower'];
              },
              color: 'rgba(0,0,200)',
              stack: 1,
            },
            ...Phase.THREE_PHASE.map((phase, index) => ({
              name: 'Phase ' + phase,
              converter: () => {
                return data['Phase' + phase];
              },
              color: AbstractHistoryChart.phaseColors[index],
            })),
          ];

        return datasets;
      },
      tooltip: {
        formatNumber: '1.0-2',
      },
      yAxes: [{
        unit: YAxisTitle.ENERGY,
        position: 'left',
        yAxisId: ChartAxis.LEFT,
      }],
    };
  }

  public override getChartData() {
    return ChartComponent.getChartData(this.translate);
  }
}
