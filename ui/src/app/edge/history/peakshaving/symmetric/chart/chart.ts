import { Component } from '@angular/core';
import { AbstractHistoryChart } from 'src/app/shared/genericComponents/chart/abstracthistorychart';
import { QueryHistoricTimeseriesEnergyResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { ChartAxis, HistoryUtils, YAxisTitle } from 'src/app/shared/service/utils';
import { ChannelAddress, Utils, EdgeConfig } from 'src/app/shared/shared';
import { TranslateService } from '@ngx-translate/core';


@Component({
    selector: 'ChartComponent',
    templateUrl: '../../../../../shared/genericComponents/chart/abstracthistorychart.html'
})
export class ChartComponent extends AbstractHistoryChart {

    public override getChartData() {
        return ChartComponent.getChartData(this.config, this.translate, this.component.id);
    }
    public static getChartData(config: EdgeConfig, translate: TranslateService, componentId: string): HistoryUtils.ChartData {
        let meterId = config.getComponent(componentId).properties['meter.id'];
        let channels: HistoryUtils.InputChannel[] = [
            {
                name: 'ActivePower',
                powerChannel: new ChannelAddress(meterId, 'ActivePower'),
                energyChannel: new ChannelAddress(meterId, 'ActiveEnergy')
            },
            {
                name: 'ProductionDcActual',
                powerChannel: ChannelAddress.fromString('_sum/ProductionDcActualPower'),
                energyChannel: ChannelAddress.fromString('_sum/ProductionDcActiveEnergy')
            }, {
                name: 'EssCharge',
                powerChannel: ChannelAddress.fromString('_sum/EssActivePower'),
                energyChannel: ChannelAddress.fromString('_sum/EssActiveChargeEnergy')
            }, {
                name: 'EssDischarge',
                powerChannel: ChannelAddress.fromString('_sum/EssActivePower'),
                energyChannel: ChannelAddress.fromString('_sum/EssActiveDischargeEnergy')
            }, {
                name: 'peakshavingPower',
                powerChannel: ChannelAddress.fromString(componentId + '/_PropertyPeakShavingPower')
            }, {
                name: 'rechargePower',
                powerChannel: ChannelAddress.fromString(componentId + '/_PropertyRechargePower')
            }];
        let chartObject: HistoryUtils.ChartData = {
            input: channels,
            output: (data: HistoryUtils.ChannelData) => {
                let datasets: HistoryUtils.DisplayValues[] = [];
                datasets.push({
                    name: translate.instant('General.measuredValue'),
                    nameSuffix: (query: QueryHistoricTimeseriesEnergyResponse) => {
                        return query.result.data[meterId + '/ActiveEnergy'];
                    },
                    converter: () => {
                        return data['ActivePower'];
                    },
                    color: 'rgba(0,0,0)'
                });
                datasets.push({
                    name: translate.instant('Edge.Index.Widgets.Peakshaving.peakshavingPower'),
                    converter: () => {
                        return data['peakshavingPower'];
                    },
                    color: 'rgba(200,0,0)',
                    showBackgroundColor: false,
                    borderDash: [3, 3]
                });
                datasets.push({
                    name: translate.instant('Edge.Index.Widgets.Peakshaving.rechargePower'),
                    converter: () => {
                        return data['rechargePower'];
                    },
                    color: 'rgba(0,223,0)',
                    showBackgroundColor: false,
                    borderDash: [3, 3]
                });

                // Charge Power
                datasets.push({
                    name: translate.instant('General.chargePower'),
                    nameSuffix: (energyResponse: QueryHistoricTimeseriesEnergyResponse) => {
                        return energyResponse.result.data['_sum/EssActiveChargeEnergy'];
                    },
                    converter: () => {
                        return data['EssCharge']
                            ?.map((value, index) =>
                                Utils.subtractSafely(data['ProductionDcActual'][index], value))?.map(value => Utils.multiplySafely(-1, value))?.map(value => HistoryUtils.ValueConverter.NEGATIVE_AS_ZERO(value));
                    },
                    color: 'rgba(0,223,0)',
                    borderDash: [10, 10]
                });
                datasets.push({
                    name: translate.instant('General.dischargePower'),
                    nameSuffix: (energyResponse: QueryHistoricTimeseriesEnergyResponse) => {
                        return energyResponse.result.data['_sum/EssActiveDischargeEnergy'];
                    },
                    converter: () => {
                        return data['EssCharge']?.map(value => HistoryUtils.ValueConverter.NEGATIVE_AS_ZERO(value));
                    },
                    color: 'rgba(200,0,0)',
                    borderDash: [10, 10]
                });
                return datasets;
            },
            tooltip: {
                formatNumber: '1.0-2'
            },
            yAxes: [{
                unit: YAxisTitle.ENERGY,
                position: 'left',
                yAxisId: ChartAxis.LEFT
            }]
        };
        return chartObject;
    }
}


