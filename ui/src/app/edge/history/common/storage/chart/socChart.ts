import { Component } from '@angular/core';
import { AbstractHistoryChart } from 'src/app/shared/genericComponents/chart/abstracthistorychart';
import { HistoryUtils } from 'src/app/shared/service/utils';
import { ChannelAddress } from 'src/app/shared/shared';

@Component({
    selector: 'storageSocChart',
    templateUrl: '../../../../../shared/genericComponents/chart/abstracthistorychart.html'
})
export class SocChartComponent extends AbstractHistoryChart {

    protected getChartData(): HistoryUtils.ChartData {

        const channels: HistoryUtils.InputChannel[] = [{
            name: 'Soc',
            powerChannel: ChannelAddress.fromString('_sum' + '/EssSoc'),
            converter: (data) => data != null ? data : null
        }];

        const emergencyCapacityReserveComponents = this.config.getComponentsByFactory('Controller.Ess.EmergencyCapacityReserve')
            .filter(component => component.isEnabled);

        const ess = this.config.getComponentsImplementingNature("io.openems.edge.ess.api.SymmetricEss")
            .filter(component => !(component.factoryId === 'Ess.Cluster'));

        if (ess.length > 1) {
            ess.forEach(component => {
                channels.push({
                    name: component.id,
                    powerChannel: ChannelAddress.fromString(component.id + '/Soc'),
                    converter: (data) => data != null ? data : null
                });
            });
        }

        emergencyCapacityReserveComponents.forEach(
            component =>
                channels.push({
                    name: component.id,
                    powerChannel: ChannelAddress.fromString(component.id + '/ActualReserveSoc'),
                    converter: (data) => data != null ? data : null
                })
        );

        return {
            input: channels,
            output: (data: HistoryUtils.ChannelData) => {
                let datasets: HistoryUtils.DisplayValues[] = [];

                datasets.push({
                    name: ess.length > 1 ? this.translate.instant('General.TOTAL') : this.translate.instant('General.soc'),
                    converter: () => { return data['Soc']?.map(element => element != null ? element * 1000 : element); },
                    color: 'rgb(0,223,0)'
                });

                if (ess.length > 1) {
                    ess.forEach(component => {
                        datasets.push({
                            name: component.id,
                            converter: () => { return data[component.id]?.map(element => element != null ? element * 1000 : element); },
                            color: 'rgb(128,128,128)'
                        });
                    });
                }

                emergencyCapacityReserveComponents.forEach(
                    component =>
                        datasets.push({
                            name: emergencyCapacityReserveComponents.length > 1 ? component.alias : this.translate.instant("Edge.Index.EmergencyReserve.emergencyReserve"),
                            converter: () => { return data[component.id]?.map(element => element != null ? element * 1000 : element); },
                            color: 'rgb(1,1,1)'
                        })
                );

                return datasets;
            },
            tooltip: {
                formatNumber: '1.1-2'
            },
            unit: HistoryUtils.YAxisTitle.PERCENTAGE
        };
    }
}