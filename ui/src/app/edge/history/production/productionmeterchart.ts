import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChannelAddress, Edge, EdgeConfig, Service } from '../../../shared/shared';
import { AbstractHistoryChart } from '../abstracthistorychart';
import { Data, TooltipItem } from '../shared';

@Component({
    selector: 'productionMeterChart',
    templateUrl: '../abstracthistorychart.html'
})
export class ProductionMeterChartComponent extends AbstractHistoryChart implements OnInit, OnChanges {

    @Input() public period: DefaultTypes.HistoryPeriod;
    @Input() public componentId: string;
    @Input() public isOnlyChart: boolean;
    @Input() public showPhases: boolean;

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
        this.spinnerId = 'production-meter-chart';
        this.service.startSpinner(this.spinnerId);
        this.service.setCurrentComponent('', this.route);
    }

    ngOnDestroy() {
        this.unsubscribeChartRefresh()
    }

    protected updateChart() {
        this.autoSubscribeChartRefresh();
        this.loading = true;
        this.service.startSpinner(this.spinnerId);
        this.colors = [];
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

                    this.getChannelAddresses(edge, config).then(channelAddresses => {
                        channelAddresses.forEach(channelAddress => {
                            let data = result.data[channelAddress.toString()].map(value => {
                                if (value == null) {
                                    return null
                                } else {
                                    return value / 1000; // convert to kW
                                }
                            });
                            if (!data) {
                                return;
                            } else {
                                if (channelAddress.channelId == 'ActivePower') {
                                    datasets.push({
                                        label: this.translate.instant('General.production'),
                                        data: data
                                    });
                                    this.colors.push({
                                        backgroundColor: 'rgba(45,143,171,0.05)',
                                        borderColor: 'rgba(45,143,171,1)'
                                    });
                                }
                                if ('_sum/ProductionAcActivePowerL1' && '_sum/ProductionAcActivePowerL2' && '_sum/ProductionAcActivePowerL3' in result.data && this.showPhases == true) {
                                    if (channelAddress.channelId == 'ProductionAcActivePowerL1') {
                                        datasets.push({
                                            label: this.translate.instant('General.production') + ' ' + this.translate.instant('General.phase') + ' ' + 'L1',
                                            data: data
                                        });
                                        this.colors.push(this.phase1Color);
                                    }
                                    if (channelAddress.channelId == 'ProductionAcActivePowerL2') {
                                        datasets.push({
                                            label: this.translate.instant('General.production') + ' ' + this.translate.instant('General.phase') + ' ' + 'L2',
                                            data: data
                                        });
                                        this.colors.push(this.phase2Color);
                                    }
                                    if (channelAddress.channelId == 'ProductionAcActivePowerL3') {
                                        datasets.push({
                                            label: this.translate.instant('General.production') + ' ' + this.translate.instant('General.phase') + ' ' + 'L3',
                                            data: data
                                        });
                                        this.colors.push(this.phase3Color);
                                    }
                                }
                            }
                        });
                    });
                    this.datasets = datasets;
                    this.loading = false;
                    this.service.stopSpinner(this.spinnerId);
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
                new ChannelAddress(this.componentId, 'ActivePower'),
                new ChannelAddress('_sum', 'ProductionAcActivePowerL1'),
                new ChannelAddress('_sum', 'ProductionAcActivePowerL2'),
                new ChannelAddress('_sum', 'ProductionAcActivePowerL3'),
            ];
            resolve(result);
        })
    }

    protected setLabel() {
        let options = this.createDefaultChartOptions();
        options.scales.yAxes[0].scaleLabel.labelString = "kW";
        options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
            let label = data.datasets[tooltipItem.datasetIndex].label;
            let value = tooltipItem.yLabel;
            return label + ": " + formatNumber(value, 'de', '1.0-2') + " kW";
        }
        this.options = options;
    }

    public getChartHeight(): number {
        if (this.isOnlyChart == true) {
            return window.innerHeight / 1.3;
        } else {
            return window.innerHeight / 21 * 9;
        }
    }
}