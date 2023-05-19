import { ChannelAddress, Utils } from '../../../../../shared/shared';
import { Component } from '@angular/core';
import { AbstractHistoryChart } from 'src/app/shared/genericComponents/chart/abstracthistorychart';
import { ChannelFilter, ChartData } from '../../../shared';

@Component({
    selector: 'autarchychart',
    templateUrl: '../../../../../shared/genericComponents/chart/abstracthistorychart.html',
})
export class ChartComponent extends AbstractHistoryChart {

    protected override getChartData(): ChartData {
        this.spinnerId = 'autarchy-chart';
        return {
            channel:
                [{
                    name: 'Consumption',
                    powerChannel: ChannelAddress.fromString('_sum/ConsumptionActivePower'),
                    energyChannel: ChannelAddress.fromString('_sum/ConsumptionActiveEnergy'),
                    filter: ChannelFilter.NOT_NULL,
                },
                {
                    name: 'GridBuy',
                    powerChannel: ChannelAddress.fromString('_sum/GridActivePower'),
                    energyChannel: ChannelAddress.fromString('_sum/GridBuyActiveEnergy'),
                    filter: ChannelFilter.NOT_NULL_OR_NEGATIVE,
                }],
            displayValue: [{
                name: this.translate.instant('General.autarchy'),
                getValue: function (data: number[]) {
                    return data['Consumption'].map((value, index) => Utils.calculateAutarchy(data['GridBuy'][index], value));
                },
                color: 'rgb(0,152,204)'
            }],
            tooltip: {
                unit: '%',
                formatNumber: '1.0-0'
            },
            yAxisTitle: this.translate.instant("General.percentage"),
        };
    }
    protected override getChannelAddresses() {
        return {
            powerChannels: [
                new ChannelAddress('_sum', 'GridActivePower'),
                new ChannelAddress('_sum', 'ConsumptionActivePower')]
            ,
            energyChannels: [
                new ChannelAddress('_sum', 'GridBuyActiveEnergy'),
                new ChannelAddress('_sum', 'ConsumptionActiveEnergy')
            ]
        };
    }
}