import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChannelAddress, Edge, EdgeConfig, Service, Utils } from '../../../shared/shared';
import { AbstractHistoryChart } from '../abstracthistorychart';
import { ChartOptions, Data, DEFAULT_TIME_CHART_OPTIONS, TooltipItem } from './../shared';

@Component({
    selector: 'gridChart',
    templateUrl: '../abstracthistorychart.html'
})
export class GridChartComponent extends AbstractHistoryChart implements OnInit, OnChanges {

    @Input() private period: DefaultTypes.HistoryPeriod;
    @Input() private showPhases: boolean;

    ngOnChanges() {
        this.updateChart();
    };

    constructor(
        protected service: Service,
        protected translate: TranslateService,
        private route: ActivatedRoute,
    ) {
        super(service, translate);
    }


    ngOnInit() {
        this.service.setCurrentComponent('', this.route);
        this.setLabel();
    }

    protected updateChart() {
        this.loading = true;
        this.queryHistoricTimeseriesData(this.period.from, this.period.to).then(response => {
            this.service.getCurrentEdge().then(() => {
                this.service.getConfig().then(() => {
                    let result = response.result;
                    this.colors = [];
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
                                return null
                            } else if (value == 0) {
                                return 0;
                            } else {
                                return value / 1000;
                            }
                        });
                        datasets.push({
                            label: this.translate.instant('General.Grid'),
                            data: gridData,
                            hidden: false
                        });
                        this.colors.push({
                            backgroundColor: 'rgba(0,0,0,0.05)',
                            borderColor: 'rgba(0,0,0,1)'
                        })
                    }

                    if ('_sum/GridActivePowerL1' && '_sum/GridActivePowerL2' && '_sum/GridActivePowerL3' in result.data && this.showPhases == true) {
                        if ('_sum/GridActivePowerL1' in result.data) {
                            /**
                             * Buy From Grid
                             */
                            let gridData = result.data['_sum/GridActivePowerL1'].map(value => {
                                if (value == null) {
                                    return null
                                } else if (value == 0) {
                                    return 0;
                                } else {
                                    return value / 1000;
                                }
                            });
                            datasets.push({
                                label: this.translate.instant('General.Phase') + ' ' + 'L1',
                                data: gridData,
                                hidden: false
                            });
                            this.colors.push(this.phase1Color);
                        }
                        if ('_sum/GridActivePowerL2' in result.data) {
                            /**
                             * Buy From Grid
                             */
                            let gridData = result.data['_sum/GridActivePowerL2'].map(value => {
                                if (value == null) {
                                    return null
                                } else if (value == 0) {
                                    return 0;
                                } else {
                                    return value / 1000;
                                }
                            });
                            datasets.push({
                                label: this.translate.instant('General.Phase') + ' ' + 'L2',
                                data: gridData,
                                hidden: false
                            });
                            this.colors.push(this.phase2Color);
                        }
                        if ('_sum/GridActivePowerL3' in result.data) {
                            /**
                             * Buy From Grid
                             */
                            let gridData = result.data['_sum/GridActivePowerL3'].map(value => {
                                if (value == null) {
                                    return null
                                } else if (value == 0) {
                                    return 0;
                                } else {
                                    return value / 1000;
                                }
                            });
                            datasets.push({
                                label: this.translate.instant('General.Phase') + ' ' + 'L3',
                                data: gridData,
                                hidden: false
                            });
                            this.colors.push(this.phase3Color);
                        }
                    }
                    this.datasets = datasets;
                    this.loading = false;

                }).catch(reason => {
                    console.error(reason); // TODO error message
                    this.initializeChart();
                    return;
                });
            }).catch(reason => {
                console.error(reason); // TODO error message
                this.initializeChart();
                return;
            });
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
                new ChannelAddress('_sum', 'GridActivePowerL3')
            ];
            resolve(result);
        })
    }

    protected setLabel() {
        let translate = this.translate; // enables access to TranslateService
        let options = <ChartOptions>Utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS);
        options.scales.yAxes[0].scaleLabel.labelString = "kW";
        options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
            let label = data.datasets[tooltipItem.datasetIndex].label;
            let value = tooltipItem.yLabel;
            // 0.005 to prevent showing Charge or Discharge if value is e.g. 0.00232138
            if (value < -0.005) {
                if (label.includes(translate.instant('General.Phase'))) {
                    label += ' ' + translate.instant('General.GridSell');
                } else {
                    label = translate.instant('General.GridSell');
                }
            } else if (value > 0.005) {
                if (label.includes(translate.instant('General.Phase'))) {
                    label += ' ' + translate.instant('General.GridBuy');
                } else {
                    label = translate.instant('General.GridBuy');
                }
            }
            return label + ": " + formatNumber(value, 'de', '1.0-2') + " kW";
        }
        this.options = options;
    }

    public getChartHeight(): number {
        return window.innerHeight / 1.3;
    }
}