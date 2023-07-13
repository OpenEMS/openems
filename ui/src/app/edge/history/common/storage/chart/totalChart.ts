import { Component } from '@angular/core';
import { AbstractHistoryChart } from 'src/app/shared/genericComponents/chart/abstracthistorychart';
import { HistoryUtils, Utils } from 'src/app/shared/service/utils';
import { ChannelAddress } from 'src/app/shared/shared';

@Component({
    selector: 'storageTotalChart',
    templateUrl: '../../../../../shared/genericComponents/chart/abstracthistorychart.html'
})
export class TotalChartComponent extends AbstractHistoryChart {

    protected getChartData(): HistoryUtils.ChartData {

        const channels: HistoryUtils.InputChannel[] = [{
            name: 'EssActivePower',
            powerChannel: ChannelAddress.fromString('_sum' + '/EssActivePower'),
            converter: (data) => data != null ? data : null
        }, {
            name: 'EssActivePowerL1',
            powerChannel: ChannelAddress.fromString('_sum' + '/EssActivePowerL1'),
            converter: (data) => data != null ? data : null
        }, {
            name: 'EssActivePowerL2',
            powerChannel: ChannelAddress.fromString('_sum' + '/EssActivePowerL2'),
            converter: (data) => data != null ? data : null
        }, {
            name: 'EssActivePowerL3',
            powerChannel: ChannelAddress.fromString('_sum' + '/EssActivePowerL3'),
            converter: (data) => data != null ? data : null
        }, {
            name: 'ProductionDcActualPower',
            powerChannel: ChannelAddress.fromString('_sum' + '/ProductionDcActualPower'),
            // energyChannel: ChannelAddress.fromString('_sum/ProductionDcActiveEnergy'),
            converter: (data) => data != null ? data : null
        }];

        const ess = this.config.getComponentsImplementingNature("io.openems.edge.ess.api.SymmetricEss")
            .filter(component => !(component.factoryId === 'Ess.Cluster'));

        if (ess.length > 1) {
            ess.forEach(component => {
                let factoryID = component.factoryId;
                let factory = this.config.factories[factoryID];
                channels.push({
                    name: component.id + 'ActivePower',
                    powerChannel: ChannelAddress.fromString(component.id + '/ActivePower'),
                    converter: (data) => data != null ? data : null
                });

                if ((factory.natureIds.includes("io.openems.edge.ess.api.AsymmetricEss"))) {
                    for (let i = 1; i <= 3; i++) {
                        channels.push({
                            name: component.id + 'ActivePowerL' + i,
                            powerChannel: ChannelAddress.fromString(component.id + '/ActivePowerL' + i),
                            converter: (data) => data != null ? data : null
                        });
                    }
                }
            });
        }

        let charger = this.config.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger");
        if (ess.length != 1 && charger.length > 0) {
            charger.forEach(component => {
                channels.push(
                    {
                        name: component.id + 'ActualPower',
                        powerChannel: ChannelAddress.fromString(component.id + '/ActualPower'),
                        converter: (data) => data != null ? data : null
                    });
            });
        }

        return {
            input: channels,
            output: (data: HistoryUtils.ChannelData) => {
                let datasets: HistoryUtils.DisplayValues[] = [];
                let effectivePower;

                if (charger.length > 0) {
                    effectivePower = data['ProductionDcActualPower'].map((value, index) => {
                        return Utils.subtractSafely(data['EssActivePower'][index], value);
                    });
                } else {
                    effectivePower = data['EssActivePower'];
                }

                datasets.push({
                    name: this.translate.instant('General.TOTAL'),
                    converter: () => { return effectivePower; },
                    color: 'rgb(0,223,0)'
                });

                console.log(' new data', data);

                if (this.showPhases) {
                    datasets.push({
                        name: this.translate.instant('General.phase') + ' ' + 'L1',
                        converter: () => { return data['EssActivePowerL1']; },
                        color: 'rgb(255,127,80)'
                    }, {
                        name: this.translate.instant('General.phase') + ' ' + 'L2',
                        converter: () => { return data['EssActivePowerL2']; },
                        color: 'rgb(0,0,255)'
                    }, {
                        name: this.translate.instant('General.phase') + ' ' + 'L3',
                        converter: () => { return data['EssActivePowerL3']; },
                        color: 'rgb(128,128,0)'
                    });
                }

                console.log('datasets', datasets);

                if (ess.length > 1) {
                    ess.forEach(component => {
                        let factoryID = component.factoryId;
                        let factory = this.config.factories[factoryID];
                        datasets.push({
                            name: component.id,
                            converter: () => { return data[component.id + 'ActivePower']?.map(element => element != null ? element * 1000 : element); },
                            color: 'rgb(45,143,171)'
                        });

                        if ((factory.natureIds.includes("io.openems.edge.ess.api.AsymmetricEss")) && this.showPhases) {
                            datasets.push({
                                name: (' + component.alias + ') + ' ' + this.translate.instant('General.phase') + ' ' + 'L1',
                                converter: () => { return data[component.id + 'ActivePowerL1']; },
                                color: 'rgb(255,127,80)'
                            }, {
                                name: (' + component.alias + ') + ' ' + this.translate.instant('General.phase') + ' ' + 'L2',
                                converter: () => { return data[component.id + 'ActivePowerL2']; },
                                color: 'rgb(0,0,255)'
                            }, {
                                name: (' + component.alias + ') + ' ' + this.translate.instant('General.phase') + ' ' + 'L3',
                                converter: () => { return data[component.id + 'ActivePowerL3']; },
                                color: 'rgb(128,128,0)'
                            });
                        }
                    });
                }

                if (ess.length != 1 && charger.length > 0) {
                    charger.forEach(component => {
                        datasets.push({
                            name: component.id + 'ActualPower',
                            converter: () => { return data[component.id + 'ActualPower']?.map(element => element != null ? element * -1 : element); },
                            color: 'rgb(0,223,0)'
                        });
                    });
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