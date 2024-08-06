// @ts-strict-ignore
import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { YAxisTitle } from 'src/app/shared/service/utils';

import { ChannelAddress, Edge, EdgeConfig, Service, Utils } from '../../../../shared/shared';
import { AbstractHistoryChart } from '../../abstracthistorychart';

@Component({
    selector: 'asymmetricpeakshavingchart',
    templateUrl: '../../abstracthistorychart.html',
})
export class AsymmetricPeakshavingChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

    @Input({ required: true }) public period!: DefaultTypes.HistoryPeriod;
    @Input({ required: true }) public component!: EdgeConfig.Component;

    constructor(
        protected override service: Service,
        protected override translate: TranslateService,
        private route: ActivatedRoute,
    ) {
        super("asymmetricpeakshaving-chart", service, translate);
    }

    ngOnChanges() {
        this.updateChart();
    }

    ngOnInit() {
        this.startSpinner();
        this.service.setCurrentComponent('', this.route);
    }

    ngOnDestroy() {
        this.unsubscribeChartRefresh();
    }

    public getChartHeight(): number {
        return window.innerHeight / 1.3;
    }

    protected updateChart() {
        this.autoSubscribeChartRefresh();
        this.startSpinner();
        this.loading = true;
        this.colors = [];
        this.queryHistoricTimeseriesData(this.period.from, this.period.to).then(response => {
            const meterIdActivePowerL1 = this.component.properties['meter.id'] + '/ActivePowerL1';
            const meterIdActivePowerL2 = this.component.properties['meter.id'] + '/ActivePowerL2';
            const meterIdActivePowerL3 = this.component.properties['meter.id'] + '/ActivePowerL3';
            const peakshavingPower = this.component.id + '/_PropertyPeakShavingPower';
            const rechargePower = this.component.id + '/_PropertyRechargePower';
            const result = response.result;
            // convert labels
            const labels: Date[] = [];
            for (const timestamp of result.timestamps) {
                labels.push(new Date(timestamp));
            }
            this.labels = labels;

            // convert datasets
            const datasets = [];

            if (meterIdActivePowerL1 in result.data) {
                const data = result.data[meterIdActivePowerL1].map(value => {
                    if (value == null) {
                        return null;
                    } else if (value == 0) {
                        return 0;
                    } else {
                        return value / 1000; // convert to kW
                    }
                });
                datasets.push({
                    label: this.translate.instant('General.phase') + ' ' + 'L1',
                    data: data,
                    hidden: false,
                });
                this.colors.push(this.phase1Color);
            }
            if (meterIdActivePowerL2 in result.data) {
                const data = result.data[meterIdActivePowerL2].map(value => {
                    if (value == null) {
                        return null;
                    } else if (value == 0) {
                        return 0;
                    } else {
                        return value / 1000; // convert to kW
                    }
                });
                datasets.push({
                    label: this.translate.instant('General.phase') + ' ' + 'L2',
                    data: data,
                    hidden: false,
                });
                this.colors.push(this.phase2Color);
            }
            if (meterIdActivePowerL3 in result.data) {
                const data = result.data[meterIdActivePowerL3].map(value => {
                    if (value == null) {
                        return null;
                    } else if (value == 0) {
                        return 0;
                    } else {
                        return value / 1000; // convert to kW
                    }
                });
                datasets.push({
                    label: this.translate.instant('General.phase') + ' ' + 'L3',
                    data: data,
                    hidden: false,
                });
                this.colors.push(this.phase3Color);
            }
            if (rechargePower in result.data) {
                const data = result.data[rechargePower].map(value => {
                    if (value == null) {
                        return null;
                    } else if (value == 0) {
                        return 0;
                    } else {
                        return value / 1000; // convert to kW
                    }
                });
                datasets.push({
                    label: this.translate.instant('Edge.Index.Widgets.Peakshaving.rechargePower'),
                    data: data,
                    hidden: false,
                    borderDash: [3, 3],
                });
                this.colors.push({
                    backgroundColor: 'rgba(0,0,0,0)',
                    borderColor: 'rgba(0,223,0,1)',
                });
            }
            if (peakshavingPower in result.data) {
                const data = result.data[peakshavingPower].map(value => {
                    if (value == null) {
                        return null;
                    } else if (value == 0) {
                        return 0;
                    } else {
                        return value / 1000; // convert to kW
                    }
                });
                datasets.push({
                    label: this.translate.instant('Edge.Index.Widgets.Peakshaving.peakshavingPower'),
                    data: data,
                    hidden: false,
                    borderDash: [3, 3],
                });
                this.colors.push({
                    backgroundColor: 'rgba(0,0,0,0)',
                    borderColor: 'rgba(200,0,0,1)',
                });
            }
            if ('_sum/EssActivePower' in result.data) {
                /*
                 * Storage Charge
                 */
                let effectivePower;
                if ('_sum/ProductionDcActualPower' in result.data && result.data['_sum/ProductionDcActualPower'].length > 0) {
                    effectivePower = result.data['_sum/ProductionDcActualPower'].map((value, index) => {
                        return Utils.subtractSafely(result.data['_sum/EssActivePower'][index], value);
                    });
                } else {
                    effectivePower = result.data['_sum/EssActivePower'];
                }
                const chargeData = effectivePower.map(value => {
                    if (value == null) {
                        return null;
                    } else if (value < 0) {
                        return value / -1000; // convert to kW;
                    } else {
                        return 0;
                    }
                });
                datasets.push({
                    label: this.translate.instant('General.chargePower'),
                    data: chargeData,
                });
                this.colors.push({
                    backgroundColor: 'rgba(0,223,0,0.05)',
                    borderColor: 'rgba(0,223,0,1)',
                });
                /*
                 * Storage Discharge
                 */
                const dischargeData = effectivePower.map(value => {
                    if (value == null) {
                        return null;
                    } else if (value > 0) {
                        return value / 1000; // convert to kW
                    } else {
                        return 0;
                    }
                });
                datasets.push({
                    label: this.translate.instant('General.dischargePower'),
                    data: dischargeData,
                });
                this.colors.push({
                    backgroundColor: 'rgba(200,0,0,0.05)',
                    borderColor: 'rgba(200,0,0,1)',
                });
            }
            this.datasets = datasets;
            this.loading = false;
            this.stopSpinner();

        }).catch(reason => {
            console.error(reason); // TODO error message
            this.initializeChart();
            return;
        }).finally(async () => {
            this.unit = YAxisTitle.ENERGY;
            await this.setOptions(this.options);
            this.stopSpinner();
        });
    }

    protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {
            const result: ChannelAddress[] = [
                new ChannelAddress(this.component.id, '_PropertyPeakShavingPower'),
                new ChannelAddress(this.component.id, '_PropertyRechargePower'),
                new ChannelAddress(this.component.properties['meter.id'], 'ActivePowerL1'),
                new ChannelAddress(this.component.properties['meter.id'], 'ActivePowerL2'),
                new ChannelAddress(this.component.properties['meter.id'], 'ActivePowerL3'),
                new ChannelAddress('_sum', 'ProductionDcActualPower'),
                new ChannelAddress('_sum', 'EssActivePower'),
            ];
            resolve(result);
        });
    }

    protected setLabel() {
        this.options = this.createDefaultChartOptions();
    }

}
