import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { CurrentData } from 'src/app/shared/edge/currentdata';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChannelAddress, Edge, EdgeConfig, Service, Utils } from '../../../shared/shared';
import { ChartOptions, Data, DEFAULT_TIME_CHART_OPTIONS, TooltipItem } from '../shared';
import { AbstractHistoryChart } from '../abstracthistorychart';

@Component({
    selector: 'productionSingleChart',
    templateUrl: '../abstracthistorychart.html'
})
export class ProductionSingleChartComponent extends AbstractHistoryChart implements OnInit, OnChanges {

    @Input() private period: DefaultTypes.HistoryPeriod;
    @Input() private showPhases: boolean;

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



                    Object.keys(result.data).forEach((channel, index) => {
                        let address = ChannelAddress.fromString(channel);
                        let component = config.getComponent(address.componentId);
                        let data = result.data[channel].map(value => {
                            if (value == null) {
                                return null
                            } else {
                                return value / 1000; // convert to kW
                            }
                        });
                        //more than one Production Unit
                        if (address.channelId == 'ProductionActivePower') {
                            datasets.push({
                                label: this.translate.instant('General.Production') + ' (' + this.translate.instant('General.Total') + ')',
                                data: data
                            });
                            this.colors.push({
                                backgroundColor: 'rgba(255,165,0,0.1)',
                                borderColor: 'rgba(255,165,0,1)',
                            });
                        }
                        if ('_sum/ActivePowerL1' && '_sum/ActivePowerL2' && '_sum/ActivePowerL3' in result.data && this.showPhases == true) {
                            // Phases
                            if (address.channelId == 'ActivePowerL1') {
                                datasets.push({
                                    label: this.translate.instant('General.Production') + ' ' + this.translate.instant('General.Phase') + ' ' + 'L1',
                                    data: data
                                });
                                this.colors.push(this.phase1Color);
                            }
                            if (address.channelId == 'ActivePowerL2') {
                                datasets.push({
                                    label: this.translate.instant('General.Production') + ' ' + this.translate.instant('General.Phase') + ' ' + 'L2',
                                    data: data
                                });
                                this.colors.push(this.phase2Color);
                            }
                            if (address.channelId == 'ActivePowerL3') {
                                datasets.push({
                                    label: this.translate.instant('General.Production') + ' ' + this.translate.instant('General.Phase') + ' ' + 'L3',
                                    data: data
                                });
                                this.colors.push(this.phase3Color);
                            }
                        }
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
        return new Promise((resolve) => {
            let result: ChannelAddress[] = [
                new ChannelAddress('_sum', 'ProductionActivePower'),
                new ChannelAddress('_sum', 'ProductionAcActivePowerL1'),
                new ChannelAddress('_sum', 'ProductionAcActivePowerL2'),
                new ChannelAddress('_sum', 'ProductionAcActivePowerL3'),
            ];
            resolve(result);
        })
    }

    protected setLabel() {
        let options = <ChartOptions>Utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS);
        options.scales.yAxes[0].scaleLabel.labelString = "kW";
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
            return label + ": " + formatNumber(value, 'de', '1.0-2') + " kW";
        }
        this.options = options;
    }

    public getChartHeight(): number {
        return window.innerHeight / 21 * 9;
    }
}