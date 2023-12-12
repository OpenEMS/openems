import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';

import { ChannelAddress, Edge, EdgeConfig, Service, Utils } from '../../../../shared/shared';
import { AbstractHistoryChart } from '../../abstracthistorychart';
import * as Chart from 'chart.js';
import { ChartAxis, YAxisTitle } from 'src/app/shared/service/utils';
import { _DeepPartialObject } from 'chart.js/dist/types/utils';

@Component({
    selector: 'symmetricpeakshavingchart',
    templateUrl: '../../abstracthistorychart.html',
})
export class SymmetricPeakshavingChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

    @Input() public period: DefaultTypes.HistoryPeriod;
    @Input() public componentId: string;

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
                let meterIdActivePower = config.getComponent(this.componentId).properties['meter.id'] + '/ActivePower';
                let peakshavingPower = this.componentId + '/_PropertyPeakShavingPower';
                let rechargePower = this.componentId + '/_PropertyRechargePower';
                let result = response.result;
                // convert labels
                let labels: Date[] = [];
                for (let timestamp of result.timestamps) {
                    labels.push(new Date(timestamp));
                }
                this.labels = labels;

                // convert datasets
                let datasets = [];

                if (meterIdActivePower in result.data) {
                    let data = result.data[meterIdActivePower].map(value => {
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
                        borderDash: [10, 10],
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
        }).finally(() => {
            this.setOptions(this.options);
            this.applyControllerSpecificOptions(this.options);
        });
    }
    private applyControllerSpecificOptions(options: Chart.ChartOptions): void {
        this.options = options;
    }

    protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {
            let result: ChannelAddress[] = [
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
        let options = this.createDefaultChartOptions();
        this.options = options;
    }

    public getChartHeight(): number {
        return window.innerHeight / 1.3;
    }
}
