import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';

import { ChannelAddress, Edge, EdgeConfig, Service } from '../../../shared/shared';
import { AbstractHistoryChart } from '../abstracthistorychart';
import { Data, TooltipItem } from './../shared';

@Component({
    selector: 'gridChart',
    templateUrl: '../abstracthistorychart.html',
})
export class GridChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

    @Input() public period: DefaultTypes.HistoryPeriod;
    @Input() public showPhases: boolean;

    ngOnChanges() {
        this.updateChart();
    };

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

            let result = response.result;
            // convert labels
            let labels: Date[] = [];
            for (let timestamp of result.timestamps) {
                labels.push(new Date(timestamp));
            }
            this.labels = labels;

            // convert datasets
            let datasets = [];

            if ('_sum/GridActivePower' in result.data) {
                let gridData = result.data['_sum/GridActivePower'].map(value => {
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
                    let gridData = result.data['_sum/GridActivePowerL1'].map(value => {
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
                    let gridData = result.data['_sum/GridActivePowerL2'].map(value => {
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
                    let gridData = result.data['_sum/GridActivePowerL3'].map(value => {
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
            let result: ChannelAddress[] = [
                new ChannelAddress('_sum', 'GridActivePower'),
                new ChannelAddress('_sum', 'GridActivePowerL1'),
                new ChannelAddress('_sum', 'GridActivePowerL2'),
                new ChannelAddress('_sum', 'GridActivePowerL3'),
            ];
            resolve(result);
        });
    }

    protected setLabel() {
        let translate = this.translate; // enables access to TranslateService
        let options = this.createDefaultChartOptions();
        options.scales.yAxes[0].scaleLabel.labelString = "kW";
        options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
            let label = data.datasets[tooltipItem.datasetIndex].label;
            let value = tooltipItem.yLabel;
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
