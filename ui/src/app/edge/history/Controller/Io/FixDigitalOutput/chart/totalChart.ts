import { Component } from '@angular/core';
import { AbstractHistoryChart } from 'src/app/shared/genericComponents/chart/abstracthistorychart';
import { HistoryUtils } from 'src/app/shared/service/utils';
import { ChannelAddress } from 'src/app/shared/shared';

@Component({
  selector: 'totalChart',
  templateUrl: '../../../../../../shared/genericComponents/chart/abstracthistorychart.html',
})
export class TotalChartComponent extends AbstractHistoryChart {

  protected override getChartData(): HistoryUtils.ChartData {

    let channels: HistoryUtils.InputChannel[] = [];
    this.config.getComponentsByFactory('Controller.Io.FixDigitalOutput').forEach(component => {

      channels.push({
        name: '',
        powerChannel: ChannelAddress.fromString(this.config.getComponentProperties(component.id)['outputChannelAddress']),
      })
      // const outputChannel = ChannelAddress.fromString(this.config.getComponentProperties(component.id)['outputChannelAddress']);
    });


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