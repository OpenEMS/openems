import { Component } from '@angular/core';
import { AbstractHistoryChart } from 'src/app/shared/genericComponents/chart/abstracthistorychart';
import { ChartAxis, HistoryUtils, YAxisTitle } from 'src/app/shared/service/utils';
import { ChannelAddress } from 'src/app/shared/shared';

@Component({
  selector: 'cellVoltageChart',
  templateUrl: '../../../../shared/genericComponents/chart/abstracthistorychart.html',
})
export class ChartComponent extends AbstractHistoryChart {

  protected override getChartData(): HistoryUtils.ChartData {
    this.spinnerId = 'autarchy-chart';
    return {
      input:
        [{
          name: 'MinCellVoltage',
          powerChannel: ChannelAddress.fromString('battery0/MinCellVoltage'),
        },
        {
          name: 'MaxCellVoltage',
          powerChannel: ChannelAddress.fromString('battery0/MaxCellVoltage'),
        }],
      output: (data: HistoryUtils.ChannelData) => {
        return [{
          name: 'Minimale Zellspannung',
          converter: () => {
            return data['MinCellVoltage'];
          },
          color: 'rgb(127,255,0)',
        },
        {
          name: 'Maximale Zellspannung',
          converter: () => {
            return data['MaxCellVoltage'];
          },
          color: 'rgb(0,0,255)',
        },
        {
          name: 'Obergrenze',
          converter: () => {
            return Array(288).fill(3.65);
          },
          color: 'rgb(237, 28, 35)',
          borderDash: [10, 10],
          hideShadow: true,
        },
        {
          name: 'Untergrenze 1',
          converter: () => {
            return Array(288).fill(3);
          },
          color: 'rgb(178, 27, 69)',
          borderDash: [10, 10],
          hideShadow: true,
        },
        {
          name: 'Untergrenze 2',
          converter: () => {
            return Array(288).fill(2.7);
          },
          color: 'rgb(119, 26, 102)',
          borderDash: [10, 10],
          hideShadow: true,
        },
        {
          name: 'Untergrenze 3',
          converter: () => {
            return Array(288).fill(2.5);
          },
          color: 'rgb(59, 25, 136)',
          borderDash: [10, 10],
          hideShadow: true,

        },
        {
          name: 'Untergrenze 4',
          converter: () => {
            return Array(288).fill(2.3);
          },
          color: 'rgb(0, 24, 169)',
          borderDash: [10, 10],
          hideShadow: true,
        },
        ];
      },
      tooltip: {
        formatNumber: '1.0-2',
      },
      yAxes: [{
        unit: YAxisTitle.VOLTAGE,
        position: 'left',
        yAxisId: ChartAxis.LEFT,
      }],
    };
  }
}
