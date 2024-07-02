// @ts-strict-ignore
import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';

import { ChannelAddress, Edge, EdgeConfig, Service, Utils } from '../../../../shared/shared';
import { AbstractHistoryChart } from '../../abstracthistorychart';

@Component({
    selector: 'symmetricpeakshavingchart',
    templateUrl: '../../abstracthistorychart.html',
})
export class SymmetricPeakshavingChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

    @Input({ required: true }) public period!: DefaultTypes.HistoryPeriod;
    @Input({ required: true }) public componentId!: string;

    ngOnChanges() {
        this.updateChart();
    }

    constructor(
        protected override service: Service,
        protected override translate: TranslateService,
        private route: ActivatedRoute,
    ) {
        super("symmetricpeakshaving-chart", service, translate);
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
            this.service.getConfig().then(config => {
                const meterIdActivePower = config.getComponent(this.componentId).properties['meter.id'] + '/ActivePower';
                const peakshavingPower = this.componentId + '/_PropertyPeakShavingPower';
                const rechargePower = this.componentId + '/_PropertyRechargePower';
                const result = response.result;
                // convert labels
                const labels: Date[] = [];
                for (const timestamp of result.timestamps) {
                    labels.push(new Date(timestamp));
                }
                this.labels = labels;

                // convert datasets
                const datasets = [];

                if (meterIdActivePower in result.data) {
                    const data = result.data[meterIdActivePower].map(value => {
                        if (value == null) {
                            return null;
                        } else if (value == 0) {
                            return 0;
                        } else {
                            return value / 1000; // convert to kW
                        }
                    });
                    datasets.push({
                        label: this.translate.instant('General.measuredValue'),
                        data: data,
                        hidden: false,
                    });
                    this.colors.push({
                        backgroundColor: 'rgba(0,0,0,0.05)',
                        borderColor: 'rgba(0,0,0,1)',
                    });
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
                        borderDash: [10, 10],
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
                        borderDash: [10, 10],
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
            });

        }).catch(reason => {
            console.error(reason); // TODO error message
            this.initializeChart();
            return;
        }).finally(async () => {
            await this.setOptions(this.options);
        });
    }

    protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {
            const result: ChannelAddress[] = [
                new ChannelAddress(this.componentId, '_PropertyRechargePower'),
                new ChannelAddress(this.componentId, '_PropertyPeakShavingPower'),
                new ChannelAddress(config.getComponent(this.componentId).properties['meter.id'], 'ActivePower'),
                new ChannelAddress('_sum', 'ProductionDcActualPower'),
                new ChannelAddress('_sum', 'EssActivePower'),
            ];
            resolve(result);
        });
    }

    protected setLabel() {
        const options = this.createDefaultChartOptions();
        this.options = options;
    }

    public getChartHeight(): number {
        return window.innerHeight / 1.3;
    }
}
