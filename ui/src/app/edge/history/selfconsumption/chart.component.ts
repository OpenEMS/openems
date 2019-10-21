import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { CurrentData } from 'src/app/shared/edge/currentdata';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChannelAddress, Edge, EdgeConfig, Service, Utils } from '../../../shared/shared';
import { ChartOptions, Data, Dataset, DEFAULT_TIME_CHART_OPTIONS, EMPTY_DATASET, TooltipItem } from './../shared';
import { AbstractHistoryChart } from '../abstracthistorychart';

@Component({
    selector: 'selfconsumptionChart',
    templateUrl: '../abstracthistorychart.html'
})
export class SelfconsumptionChartComponent extends AbstractHistoryChart implements OnInit, OnChanges {

    @Input() private period: DefaultTypes.HistoryPeriod;

    ngOnChanges() {
        this.updateChart();
    };

    constructor(
        protected service: Service,
        private route: ActivatedRoute,
        private translate: TranslateService
    ) {
        super(service);
    }


    ngOnInit() {
        this.service.setCurrentComponent('', this.route);
        this.setLabel();
    }

    protected updateChart() {
        this.loading = true;
        this.queryHistoricTimeseriesData(this.period.from, this.period.to).then(response => {
            this.service.getCurrentEdge().then(edge => {
                this.service.getConfig().then(config => {
                    let result = response.result;
                    // convert labels
                    let labels: Date[] = [];
                    for (let timestamp of result.timestamps) {
                        labels.push(new Date(timestamp));
                    }
                    this.labels = labels;

                    // convert datasets
                    let datasets = [];

                    // required data for autarchy and self consumption
                    let sellToGridData: number[] = [];
                    let dischargeData: number[] = [];
                    let productionData: number[] = [];

                    if ('_sum/EssActivePower' in result.data) {
                        /*
                         * Storage Discharge
                         */
                        let effectivePower;
                        if ('_sum/ProductionDcActualPower' in result.data && result.data['_sum/ProductionDcActualPower'].length > 0) {
                            effectivePower = result.data['_sum/ProductionDcActualPower'].map((value, index) => {
                                return Utils.subtractSafely(result.data['_sum/EssActivePower'][index], value);
                            });
                        } else {
                            effectivePower = result.data['_sum/EssActivePower'];
                        }
                        dischargeData = effectivePower.map(value => {
                            if (value == null) {
                                return null
                            } else if (value > 0) {
                                return value;
                            } else {
                                return 0;
                            }
                        });
                    };

                    if ('_sum/GridActivePower' in result.data) {
                        /*
                         * Sell To Grid
                         */
                        sellToGridData = result.data['_sum/GridActivePower'].map(value => {
                            if (value == null) {
                                return null
                            } else if (value < 0) {
                                return value * -1; // invert value
                            } else {
                                return 0;
                            }
                        });
                    };

                    if ('_sum/ProductionActivePower' in result.data) {
                        /*
                         * Production
                         */
                        productionData = result.data['_sum/ProductionActivePower'].map(value => {
                            if (value == null) {
                                return null
                            } else {
                                return value;
                            }
                        });
                    }


                    /*
                    * Self Consumption
                    */
                    let selfConsumption = productionData.map((value, index) => {
                        if (value == null) {
                            return null
                        } else {
                            return CurrentData.calculateSelfConsumption(sellToGridData[index], value, dischargeData[index]);
                        }
                    })

                    datasets.push({
                        label: this.translate.instant('General.SelfConsumption'),
                        data: selfConsumption,
                        hidden: false
                    })
                    this.colors.push({
                        backgroundColor: 'rgba(253,197,7,0.05)',
                        borderColor: 'rgba(253,197,7,1)'
                    })

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
        return new Promise((resolve, reject) => {
            let result: ChannelAddress[] = [
                new ChannelAddress('_sum', 'GridActivePower'),
                new ChannelAddress('_sum', 'EssActivePower'),
                new ChannelAddress('_sum', 'ProductionActivePower'),
                new ChannelAddress('_sum', 'ProductionDcActualPower')
            ];
            resolve(result);
        })
    }

    protected setLabel() {
        let options = <ChartOptions>Utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS);
        options.scales.yAxes[0].scaleLabel.labelString = this.translate.instant('General.Percentage');
        options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
            let label = data.datasets[tooltipItem.datasetIndex].label;
            let value = tooltipItem.yLabel;
            if (label == this.grid) {
                if (value < 0) {
                    value *= -1;
                    label = this.gridBuy;
                } else {
                    label = this.gridSell;
                }
            }
            return label + ": " + formatNumber(value, 'de', '1.0-0') + " %"; // TODO get locale dynamically
        }
        options.scales.yAxes[0].ticks.max = 100;
        this.options = options;
    }
}