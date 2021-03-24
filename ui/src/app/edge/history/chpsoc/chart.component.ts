import { AbstractHistoryChart } from '../abstracthistorychart';
import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Service, Edge, EdgeConfig } from '../../../shared/shared';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { Data, TooltipItem } from './../shared';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { formatNumber } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'chpsocchart',
    templateUrl: '../abstracthistorychart.html'
})
export class ChpSocChartComponent extends AbstractHistoryChart implements OnInit, OnChanges {

    @Input() public period: DefaultTypes.HistoryPeriod;
    @Input() public componentId: string;

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
        this.spinnerId = "chpsoc-chart";
        this.service.startSpinner(this.spinnerId);
        this.service.setCurrentComponent('', this.route);
    }

    ngOnDestroy() {
        this.unsubscribeChartRefresh()
    }

    protected updateChart() {
        this.autoSubscribeChartRefresh();
        this.service.startSpinner(this.spinnerId);
        this.loading = true;
        this.queryHistoricTimeseriesData(this.period.from, this.period.to).then(response => {
            this.service.getCurrentEdge().then(() => {
                this.service.getConfig().then(config => {
                    let outputChannel = config.getComponentProperties(this.componentId)['outputChannelAddress'];
                    let inputChannel = config.getComponentProperties(this.componentId)['inputChannelAddress'];
                    let lowThreshold = this.componentId + '/_PropertyLowThreshold';
                    let highThreshold = this.componentId + '/_PropertyHighThreshold';
                    let result = response.result;
                    // convert labels
                    let labels: Date[] = [];
                    for (let timestamp of result.timestamps) {
                        labels.push(new Date(timestamp));
                    }
                    this.labels = labels;

                    // convert datasets
                    let datasets = [];

                    // convert datasets
                    for (let channel in result.data) {
                        if (channel == outputChannel) {
                            let address = ChannelAddress.fromString(channel);
                            let data = result.data[channel].map(value => {
                                if (value == null) {
                                    return null
                                } else {
                                    return value * 100; // convert to % [0,100]
                                }
                            });
                            datasets.push({
                                label: address.channelId,
                                data: data,
                            });
                            this.colors.push({
                                backgroundColor: 'rgba(0,191,255,0.05)',
                                borderColor: 'rgba(0,191,255,1)',
                            })
                        } else {
                            let data = result.data[channel].map(value => {
                                if (value == null) {
                                    return null
                                } else if (value > 100 || value < 0) {
                                    return null;
                                } else {
                                    return value;
                                }
                            })
                            if (channel == inputChannel) {
                                datasets.push({
                                    label: this.translate.instant('General.soc'),
                                    data: data,
                                });
                                this.colors.push({
                                    backgroundColor: 'rgba(0,0,0,0)',
                                    borderColor: 'rgba(0,223,0,1)',
                                })
                            }
                            if (channel == lowThreshold) {
                                datasets.push({
                                    label: this.translate.instant('Edge.Index.Widgets.CHP.lowThreshold'),
                                    data: data,
                                    borderDash: [3, 3]
                                });
                                this.colors.push({
                                    backgroundColor: 'rgba(0,0,0,0)',
                                    borderColor: 'rgba(0,191,255,1)',
                                })
                            }
                            if (channel == highThreshold) {
                                datasets.push({
                                    label: this.translate.instant('Edge.Index.Widgets.CHP.highThreshold'),
                                    data: data,
                                    borderDash: [3, 3]
                                });
                                this.colors.push({
                                    backgroundColor: 'rgba(0,0,0,0)',
                                    borderColor: 'rgba(0,191,255,1)',
                                })
                            }
                        }
                    }
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
            const outputChannel = ChannelAddress.fromString(config.getComponentProperties(this.componentId)['outputChannelAddress']);
            const inputChannel = ChannelAddress.fromString(config.getComponentProperties(this.componentId)['inputChannelAddress']);
            let result: ChannelAddress[] = [
                outputChannel,
                inputChannel,
                new ChannelAddress(this.componentId, '_PropertyHighThreshold'),
                new ChannelAddress(this.componentId, '_PropertyLowThreshold'),
            ];
            resolve(result);
        })
    }

    protected setLabel() {
        let options = this.createDefaultChartOptions();
        options.scales.yAxes[0].scaleLabel.labelString = this.translate.instant('General.percentage');
        options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
            let label = data.datasets[tooltipItem.datasetIndex].label;
            let value = tooltipItem.yLabel;
            return label + ": " + formatNumber(value, 'de', '1.0-0') + " %"; // TODO get locale dynamically
        }
        options.scales.yAxes[0].ticks.max = 100;
        this.options = options;
    }

    public getChartHeight(): number {
        return window.innerHeight / 1.3;
    }
}