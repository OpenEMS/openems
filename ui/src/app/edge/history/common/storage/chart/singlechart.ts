import { Component } from '@angular/core';
import { AbstractHistoryChart } from 'src/app/shared/genericComponents/chart/abstracthistorychart';
import { HistoryUtils, Utils } from "src/app/shared/service/utils";
import { ChannelAddress } from 'src/app/shared/shared';

@Component({
    selector: 'storageSingleChart',
    templateUrl: '../../../../../shared/genericComponents/chart/abstracthistorychart.html'
})
export class SingleChartComponent extends AbstractHistoryChart {

    protected getChartData(): HistoryUtils.ChartData {
        return {
            input: [
                {
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
                }
            ],

            output: (data: HistoryUtils.ChannelData) => {
                let datasets: HistoryUtils.DisplayValues[] = [];

                // calculate total charge and discharge
                let effectivePower = [];
                let effectivePowerL1 = [];
                let effectivePowerL2 = [];
                let effectivePowerL3 = [];
                const charger = this.config.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger");

                if (charger.length > 0) {
                    data['ProductionDcActualPower'].forEach((value, index) => {
                        if (data['ProductionDcActualPower'][index] != null) {
                            effectivePower[index] = Utils.subtractSafely(data['EssActivePower'][index], value);
                            effectivePowerL1[index] = Utils.subtractSafely(data['EssActivePowerL1'][index], value / 3);
                            effectivePowerL2[index] = Utils.subtractSafely(data['EssActivePowerL2'][index], value / 3);
                            effectivePowerL3[index] = Utils.subtractSafely(data['EssActivePowerL3'][index], value / 3);
                        }
                    });
                } else {
                    effectivePower = data['EssActivePower'];
                    effectivePowerL1 = data['EssActivePowerL1'];
                    effectivePowerL2 = data['EssActivePowerL2'];
                    effectivePowerL3 = data['EssActivePowerL3'];
                }

                datasets.push({
                    name: this.translate.instant('General.chargeDischarge'),
                    converter: () => { return effectivePower; },
                    color: 'rgb(0,223,0)'
                });

                for (let i = 1; i < 4; i++) {
                    let variableName = 'effectivePowerL' + i;
                    let currentValue = eval(variableName);
                    datasets.push({
                        name: this.translate.instant('General.phase') + ' ' + 'L' + i,
                        converter: () => {
                            if (!this.showPhases) {
                                return null;
                            }
                            return currentValue ?? null;
                        },
                        color: 'rgb(' + this.phaseColors[i - 1] + ')'
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
