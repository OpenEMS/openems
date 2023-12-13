import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';

import { ChannelAddress, Edge, EdgeConfig, Service, Utils } from '../../../../shared/shared';
import { AbstractHistoryChart } from '../../abstracthistorychart';
import { YAxisTitle } from 'src/app/shared/service/utils';


@Component({
    selector: 'asymmetricpeakshavingchart',
    templateUrl: '../../abstracthistorychart.html',
})
export class AsymmetricPeakshavingChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

    @Input() public period: DefaultTypes.HistoryPeriod;
    @Input() public component: EdgeConfig.Component;

    ngOnChanges() {
        this.updateChart();
    }

    constructor(
        protected override service: Service,
        protected override translate: TranslateService,
        private route: ActivatedRoute,
    ) {
        super("asymmetricpeakshaving-chart", service, translate);
    }

    ngOnInit() {
        this.startSpinner();
        this.service.setCurrentComponent('', this.route);
    }

    ngOnDestroy() {
        this.unsubscribeChartRefresh();
    }

    protected updateChart() {
        this.autoSubscribeChartRefresh();
        this.startSpinner();
        this.loading = true;
        this.colors = [];
        this.queryHistoricTimeseriesData(this.period.from, this.period.to).then(response => {
            let meterIdActivePowerL1 = this.component.properties['meter.id'] + '/ActivePowerL1';
            let meterIdActivePowerL2 = this.component.properties['meter.id'] + '/ActivePowerL2';
            let meterIdActivePowerL3 = this.component.properties['meter.id'] + '/ActivePowerL3';
            let peakshavingPower = this.component.id + '/_PropertyPeakShavingPower';
            let rechargePower = this.component.id + '/_PropertyRechargePower';
            let result = response.result;
            // convert labels
            let labels: Date[] = [];
            for (let timestamp of result.timestamps) {
                labels.push(new Date(timestamp));
            }
            this.labels = labels;

            // convert datasets
            let datasets = [];

            if (meterIdActivePowerL1 in result.data) {
                let data = result.data[meterIdActivePowerL1].map(value => {
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
                let data = result.data[meterIdActivePowerL2].map(value => {
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
                let data = result.data[meterIdActivePowerL3].map(value => {
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
                let data = result.data[rechargePower].map(value => {
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
                let data = result.data[peakshavingPower].map(value => {
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
                let chargeData = effectivePower.map(value => {
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
                let dischargeData = effectivePower.map(value => {
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
        }).finally(() => {
            this.unit = YAxisTitle.ENERGY;
            this.setOptions(this.options);
            this.stopSpinner();
        });;
    }

    protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {
            let result: ChannelAddress[] = [
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

    public getChartHeight(): number {
        return window.innerHeight / 1.3;
    }
}
