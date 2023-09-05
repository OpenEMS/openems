import { Component } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { AbstractHistoryChart } from 'src/app/shared/genericComponents/chart/abstracthistorychart';
import { ChartAxis, HistoryUtils, YAxisTitle } from 'src/app/shared/service/utils';
import { ChannelAddress, EdgeConfig } from 'src/app/shared/shared';

@Component({
  selector: 'singleChart',
  templateUrl: '../../../../../../shared/genericComponents/chart/abstracthistorychart.html'
})
export class ChartComponent extends AbstractHistoryChart {

  public override getChartData() {
    return ChartComponent.getChartData(this.config, this.chartType, this.translate, this.component.id);
  }

  public static getChartData(config: EdgeConfig, chartType: 'line' | 'bar', translate: TranslateService, componentId: string): HistoryUtils.ChartData {
    // let channels: HistoryUtils.InputChannel[] = [];


    // config.getComponentsByFactory('Controller.Io.FixDigitalOutput').forEach(component => {
    //   channels.push({
    //     name: 'Relay',
    //     powerChannel: ChannelAddress.fromString(config.getComponentProperties(component.id)['outputChannelAddress']),
    //   })
    // });

    return {
      input:
        [{
          name: 'Relay',
          powerChannel: ChannelAddress.fromString(config.getComponentProperties(componentId)['outputChannelAddress'])
        }],
      output: (data: HistoryUtils.ChannelData) => {
        return [{
          name: componentId,
          converter: () => {
            return data['Relay'].map(datapoint => datapoint == null ? null : datapoint * 100 * 1000);
          },
          color: 'rgb(0,191,255)'
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


    return null;
  }

  public override getChartHeight(): number {
    if (this.showTotal) {
      return window.innerHeight / 1.3;
    } else {
      return window.innerHeight / 2.3;
    }
  }
}