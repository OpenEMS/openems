import { Component } from '@angular/core';
import { AbstractHistoryChart } from 'src/app/shared/genericComponents/chart/abstracthistorychart';
import { QueryHistoricTimeseriesEnergyResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { HistoryUtils } from 'src/app/shared/service/utils';
import { ChannelAddress, Utils } from 'src/app/shared/shared';

@Component({
  selector: 'singleThresholdChart',
  templateUrl: '../../../../../shared/genericComponents/chart/abstracthistorychart.html',
})
export class ChartComponent extends AbstractHistoryChart {

  public outputChannelAddress: string | string[]
  protected override getChartData(): HistoryUtils.ChartData {
    this.outputChannelAddress = this.config.getComponentProperties(this.component.id)['outputChannelAddress'];
    this.spinnerId = 'autarchy-chart';

    let inputChannel = this.config.getComponentProperties(this.component.id)['inputChannelAddress'];
    let inputChannels: HistoryUtils.InputChannel[] = [];

    [inputChannel, this.outputChannelAddress].forEach(channel => {
      let address = ChannelAddress.fromString(channel);
      inputChannels.push({
        name: address.toString(),
        powerChannel: ChannelAddress.fromString(channel),
        // TODO if cumulated channels merged, change channel
        energyChannel: ChannelAddress.fromString(channel)
      })
    }
    );

    return {
      input: inputChannels,
      output: (data: HistoryUtils.ChannelData) => {
        let output: HistoryUtils.DisplayValues[] = [];

        for (var channelAddress in data) {
          output.push(this.getDataset(ChannelAddress.fromString(channelAddress), data));
        }

        return output;
      },
      tooltip: {
        formatNumber: '1.0-0',
      },
      unit: HistoryUtils.YAxisTitle.PERCENTAGE,
    };
  }

  private getDataset(channelAddress: ChannelAddress, data: HistoryUtils.ChannelData): HistoryUtils.DisplayValues {

    let channel = channelAddress.toString()

    // Redo conversion from kW to kWh
    let channelData = data[channel].map(value => Utils.multiplySafely(value, 1000))

    let displayValue: HistoryUtils.DisplayValues = {
      name: this.translate.instant('Edge.Index.Widgets.Singlethreshold.other'),
      nameSuffix: (energyValues: QueryHistoricTimeseriesEnergyResponse) => null,
      converter: () => channelData,
      color: 'rgb(0,0,0)'
    }

    switch (channelAddress.channelId) {
      case 'GridActivePower':
        displayValue.name = this.translate.instant('General.grid');
        displayValue.color = 'rgb(0,0,0)m';
        break;
      case 'ProductionActivePower':
        displayValue.name = this.translate.instant('General.production');
        displayValue.color = 'rgb(0,152,204)';
        displayValue.converter = () => {
          return channelData.map(value => Utils.divideSafely(value, 1000));
        };
        break;
      case 'EssSoc':
        displayValue.name = this.translate.instant('General.soc');
        displayValue.color = 'rgb(189, 195, 199)';
        displayValue.converter = () => channelData.map(value => (value > 100 || value < 0) ? null : value)
        break;
      default:

        // TODO redo
        if ((this.outputChannelAddress?.includes(channel))) {
          displayValue.name = channelAddress.channelId;
          displayValue.color = 'rgb(0,191,255)';
          displayValue.converter = () => channelData.map(value => Utils.multiplySafely(value, 100))
        }
        break;
    }
    return displayValue
  }
}