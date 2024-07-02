// @ts-strict-ignore
import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';

import { ChannelAddress, Edge, EdgeConfig, Service } from '../../../shared/shared';
import { AbstractHistoryChart } from '../abstracthistorychart';
import * as Chart from 'chart.js';

@Component({
    selector: 'gridChart',
    templateUrl: '../abstracthistorychart.html',
})
export class GridChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

    @Input({ required: true }) public period!: DefaultTypes.HistoryPeriod;
    @Input({ required: true }) public showPhases!: boolean;

    ngOnChanges() {
        this.updateChart();
    }

    constructor(
        protected override service: Service,
        protected override translate: TranslateService,
        private route: ActivatedRoute,
    ) {
        super("grid-chart", service, translate);
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
        this.loading = true;
        this.startSpinner();
        this.colors = [];

        this.queryHistoricTimeseriesData(this.period.from, this.period.to).then(response => {

            const result = response.result;
            // convert labels
            const labels: Date[] = [];
            for (const timestamp of result.timestamps) {
                labels.push(new Date(timestamp));
            }
            this.labels = labels;

            // convert datasets
            const datasets = [];

            if ('_sum/GridActivePower' in result.data) {
                const gridData = result.data['_sum/GridActivePower'].map(value => {
                    if (value == null) {
                        return null;
                    } else if (value == 0) {
                        return 0;
                    } else {
                        return value / 1000;
                    }
                });
                datasets.push({
                    label: this.translate.instant('General.grid'),
                    data: gridData,
                    hidden: false,
                });
                this.colors.push({
                    backgroundColor: 'rgba(0,0,0,0.05)',
                    borderColor: 'rgba(0,0,0,1)',
                });
            }

            if ('_sum/GridActivePowerL1' && '_sum/GridActivePowerL2' && '_sum/GridActivePowerL3' in result.data && this.showPhases == true) {
                if ('_sum/GridActivePowerL1' in result.data) {
                    /**
                     * Buy From Grid
                     */
                    const gridData = result.data['_sum/GridActivePowerL1'].map(value => {
                        if (value == null) {
                            return null;
                        } else if (value == 0) {
                            return 0;
                        } else {
                            return value / 1000;
                        }
                    });
                    datasets.push({
                        label: this.translate.instant('General.phase') + ' ' + 'L1',
                        data: gridData,
                        hidden: false,
                    });
                    this.colors.push(this.phase1Color);
                }
                if ('_sum/GridActivePowerL2' in result.data) {
                    /**
                     * Buy From Grid
                     */
                    const gridData = result.data['_sum/GridActivePowerL2'].map(value => {
                        if (value == null) {
                            return null;
                        } else if (value == 0) {
                            return 0;
                        } else {
                            return value / 1000;
                        }
                    });
                    datasets.push({
                        label: this.translate.instant('General.phase') + ' ' + 'L2',
                        data: gridData,
                        hidden: false,
                    });
                    this.colors.push(this.phase2Color);
                }
                if ('_sum/GridActivePowerL3' in result.data) {
                    /**
                     * Buy From Grid
                     */
                    const gridData = result.data['_sum/GridActivePowerL3'].map(value => {
                        if (value == null) {
                            return null;
                        } else if (value == 0) {
                            return 0;
                        } else {
                            return value / 1000;
                        }
                    });
                    datasets.push({
                        label: this.translate.instant('General.phase') + ' ' + 'L3',
                        data: gridData,
                        hidden: false,
                    });
                    this.colors.push(this.phase3Color);
                }
            }
            this.datasets = datasets;
            this.loading = false;
            this.stopSpinner();

        }).catch(reason => {
            console.error(reason); // TODO error message
            this.initializeChart();
            return;
        });
    }

    protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {
            const result: ChannelAddress[] = [
                new ChannelAddress('_sum', 'GridActivePower'),
                new ChannelAddress('_sum', 'GridActivePowerL1'),
                new ChannelAddress('_sum', 'GridActivePowerL2'),
                new ChannelAddress('_sum', 'GridActivePowerL3'),
            ];
            resolve(result);
        });
    }

    protected setLabel() {
        const translate = this.translate; // enables access to TranslateService
        const options = this.createDefaultChartOptions();
        options.plugins.tooltip.callbacks.label = function (tooltipItem: Chart.TooltipItem<any>) {
            let label = tooltipItem.dataset.label;
            const value = tooltipItem.dataset.data[tooltipItem.dataIndex];
            // 0.005 to prevent showing Charge or Discharge if value is e.g. 0.00232138
            if (value < -0.005) {
                if (label.includes(translate.instant('General.phase'))) {
                    label += ' ' + translate.instant('General.gridSell');
                } else {
                    label = translate.instant('General.gridSell');
                }
            } else if (value > 0.005) {
                if (label.includes(translate.instant('General.phase'))) {
                    label += ' ' + translate.instant('General.gridBuy');
                } else {
                    label = translate.instant('General.gridBuy');
                }
            }
            return label + ": " + formatNumber(value, 'de', '1.0-2') + " kW";
        };
        this.options = options;
    }

    public getChartHeight(): number {
        return window.innerHeight / 1.3;
    }
}
