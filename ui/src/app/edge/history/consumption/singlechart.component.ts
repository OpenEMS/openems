import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { QueryHistoricTimeseriesDataResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChannelAddress, Edge, EdgeConfig, Service } from '../../../shared/shared';
import { AbstractHistoryChart } from '../abstracthistorychart';
import { Data, TooltipItem } from '../shared';

@Component({
    selector: 'consumptionSingleChart',
    templateUrl: '../abstracthistorychart.html'
})
export class ConsumptionSingleChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

    @Input() public period: DefaultTypes.HistoryPeriod;
    @Input() public showPhases: boolean;
    @Input() public isOnlyChart: boolean;

    ngOnChanges() {
        this.updateChart();
    };

    constructor(
        protected service: Service,
        protected translate: TranslateService,
        private route: ActivatedRoute
    ) {
        super("consumption-single-chart", service, translate);
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
        this.queryHistoricTimeseriesData(this.period.from, this.period.to).then(response => {
            this.service.getCurrentEdge().then(edge => {
                this.service.getConfig().then(config => {
                    this.colors = [];
                    let result = (response as QueryHistoricTimeseriesDataResponse).result;

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
                            if (!Object.keys(result.data).includes(channelAddress.toString())) {
                                result.data[channelAddress.toString()] = [].fill(null);
                            }
                            let component = config.getComponent(channelAddress.componentId);
                            let data = result.data[channelAddress.toString()].map(value => {
                                if (value == null) {
                                    return null;
                                } else {
                                    return value / 1000;
                                }
                            });
                            if (!data) {
                                return;
                            } else {
                                if (channelAddress.channelId == 'ConsumptionActivePower') {
                                    datasets.push({
                                        label: this.translate.instant('General.consumption'),
                                        data: data,
                                        hidden: false
                                    });
                                    this.colors.push({
                                        backgroundColor: 'rgba(253,197,7,0.05)',
                                        borderColor: 'rgba(253,197,7,1)'
                                    });
                                }

                                // ConsumptionMeter
                                if (channelAddress.channelId == 'ActivePower') {
                                    datasets.push({
                                        label: component.alias,
                                        data: data
                                    });
                                    this.colors.push({
                                        backgroundColor: 'rgba(255,64,64,0.1)',
                                        borderColor: 'rgba(255,64,64,1)'
                                    });
                                }

                                if (this.showPhases == true) {
                                    if (channelAddress.channelId == 'ConsumptionActivePowerL1') {
                                        datasets.push({
                                            label: this.translate.instant('General.phase') + ' ' + 'L1',
                                            data: data
                                        });
                                        this.colors.push(this.phase1Color);
                                    }
                                    if (channelAddress.channelId == 'ConsumptionActivePowerL2') {
                                        datasets.push({
                                            label: this.translate.instant('General.phase') + ' ' + 'L2',
                                            data: data
                                        });
                                        this.colors.push(this.phase2Color);
                                    }
                                    if (channelAddress.channelId == 'ConsumptionActivePowerL3') {
                                        datasets.push({
                                            label: this.translate.instant('General.phase') + ' ' + 'L3',
                                            data: data
                                        });
                                        this.colors.push(this.phase3Color);
                                    }

                                    // Meter Phases
                                    if (channelAddress.channelId == 'ActivePowerL1') {
                                        datasets.push({
                                            label: component.alias + ' Phase ' + 'L1',
                                            data: data
                                        });
                                        this.colors.push({
                                            backgroundColor: 'rgba(255,193,193,0.1)',
                                            borderColor: 'rgba(139,35,35,1)'
                                        });
                                    }
                                    if (channelAddress.channelId == 'ActivePowerL2') {
                                        datasets.push({
                                            label: component.alias + ' Phase ' + 'L2',
                                            data: data
                                        });
                                        this.colors.push({
                                            backgroundColor: 'rgba(198,226,255,0.1)',
                                            borderColor: 'rgba(198,226,255,1)'
                                        });
                                    }
                                    if (channelAddress.channelId == 'ActivePowerL3') {
                                        datasets.push({
                                            label: component.alias + ' Phase ' + 'L3',
                                            data: data
                                        });
                                        this.colors.push({
                                            backgroundColor: 'rgba(121,205,205,0.1)',
                                            borderColor: 'rgba(121,205,205,1)'
                                        });
                                    }
                                }
                            }
                        });
                    });
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
                new ChannelAddress('_sum', 'ConsumptionActivePower'),
                new ChannelAddress('_sum', 'ConsumptionActivePowerL1'),
                new ChannelAddress('_sum', 'ConsumptionActivePowerL2'),
                new ChannelAddress('_sum', 'ConsumptionActivePowerL3')
            ];

            let consumptionMeters = config.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
                .filter(component => component.isEnabled && config.isTypeConsumptionMetered(component));
            for (let meter of consumptionMeters) {
                result.push(new ChannelAddress(meter.id, 'ActivePower'));
                result.push(new ChannelAddress(meter.id, 'ActivePowerL1'));
                result.push(new ChannelAddress(meter.id, 'ActivePowerL2'));
                result.push(new ChannelAddress(meter.id, 'ActivePowerL3'));
            }
            resolve(result);
        });
    }

    protected setLabel() {
        let options = this.createDefaultChartOptions();
        options.scales.yAxes[0].scaleLabel.labelString = "kW";
        options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
            let label = data.datasets[tooltipItem.datasetIndex].label;
            let value = tooltipItem.yLabel;
            return label + ": " + formatNumber(value, 'de', '1.0-2') + " kW";
        };
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