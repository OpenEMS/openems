import { Component } from "@angular/core";
import { AbstractHistoryChart } from "src/app/shared/genericComponents/chart/abstracthistorychart";
import { HistoryUtils } from "src/app/shared/service/utils";
import { ChannelAddress } from "src/app/shared/shared";

@Component({
    selector: 'storageChargerChart',
    templateUrl: '../../../../../shared/genericComponents/chart/abstracthistorychart.html'
})
export class ChargerChartComponent extends AbstractHistoryChart {
    protected componentId: string;

    protected getChartData(): HistoryUtils.ChartData {
        return {
            input: [
                {
                    name: 'ActualPower',
                    powerChannel: ChannelAddress.fromString(this.componentId + '/ActualPower'),
                    converter: (data) => data != null ? data : null
                }
            ],
            output: (data: HistoryUtils.ChannelData) => {
                return [
                    {
                        name: this.translate.instant('General.chargePower'),
                        converter: () => { return data['ActualPower']; },
                        color: 'rgba(0,223,0)'
                    }
                ];
            },
            tooltip: {
                formatNumber: '1.1-2'
            },
            unit: HistoryUtils.YAxisTitle.ENERGY
        };
    }
}