import { Component } from '@angular/core';
import { AbstractHistoryChart } from 'src/app/shared/genericComponents/chart/abstracthistorychart';
import { HistoryUtils } from "src/app/shared/service/utils";
import { ChannelAddress } from 'src/app/shared/shared';

@Component({
    selector: 'storageEssChart',
    templateUrl: '../../../../../shared/genericComponents/chart/abstracthistorychart.html'
})
export class EssChartComponent extends AbstractHistoryChart {
    protected componentId: string;

    protected getChartData(): HistoryUtils.ChartData {
        console.log(this.component);

        let component = this.config.getComponent(this.componentId);
        let factoryID = component.factoryId;
        let factory = this.config.factories[factoryID];

        const channels: HistoryUtils.InputChannel[] = [{
            name: 'ActivePower',
            powerChannel: ChannelAddress.fromString(this.componentId + '/ActivePower'),
            converter: (data) => data != null ? data : null
        }];

        if ((factory.natureIds.includes("io.openems.edge.ess.api.AsymmetricEss"))) {
            for (let i = 1; i <= 3; i++) {
                channels.push({
                    name: 'ActivePowerL' + i,
                    powerChannel: ChannelAddress.fromString(component.id + '/ActivePowerL' + i),
                    converter: (data) => data != null ? data : null
                });
            }
        }

        return {
            input: channels,
            output: (data: HistoryUtils.ChannelData) => {
                let datasets: HistoryUtils.DisplayValues[] = [];
                datasets.push(
                    {
                        name: this.translate.instant('General.chargeDischarge'),
                        converter: () => { return data['ActivePower']; },
                        color: 'rgb(0,223,0)'
                    }
                );

                for (let i = 1; i <= 3; i++) {
                    if (data['ActivePowerL' + i]) {
                        datasets.push(
                            {
                                name: this.translate.instant('General.phase') + ' ' + 'L' + i,
                                converter: () => { return data['ActivePowerL' + i]; },
                                color: 'rgb(' + this.phaseColors[i - 1] + ')'
                            }
                        );
                    }
                }

                return datasets;
            },
            tooltip: {
                formatNumber: '1.1-2'
            },
            unit: HistoryUtils.YAxisTitle.ENERGY
        };
    }
}