// @ts-strict-ignore
import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';

import { ChannelAddress, Edge, EdgeConfig, Service } from '../../../shared/shared';
import { AbstractHistoryChart } from '../abstracthistorychart';
import { YAxisTitle } from 'src/app/shared/service/utils';

@Component({
    selector: 'chpsocchart',
    templateUrl: '../abstracthistorychart.html',
})
export class ChpSocChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

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
        super("chpsoc-chart", service, translate);
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
            this.service.getCurrentEdge().then(() => {
                this.service.getConfig().then(config => {
                    const outputChannel = config.getComponentProperties(this.componentId)['outputChannelAddress'];
                    const inputChannel = config.getComponentProperties(this.componentId)['inputChannelAddress'];
                    const lowThreshold = this.componentId + '/_PropertyLowThreshold';
                    const highThreshold = this.componentId + '/_PropertyHighThreshold';
                    const result = response.result;
                    // convert labels
                    const labels: Date[] = [];
                    for (const timestamp of result.timestamps) {
                        labels.push(new Date(timestamp));
                    }
                    this.labels = labels;

                    // convert datasets
                    const datasets = [];

                    // convert datasets
                    for (const channel in result.data) {
                        if (channel == outputChannel) {
                            const address = ChannelAddress.fromString(channel);
                            const data = result.data[channel].map(value => {
                                if (value == null) {
                                    return null;
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
                            });
                        } else {
                            const data = result.data[channel].map(value => {
                                if (value == null) {
                                    return null;
                                } else if (value > 100 || value < 0) {
                                    return null;
                                } else {
                                    return value;
                                }
                            });
                            if (channel == inputChannel) {
                                datasets.push({
                                    label: this.translate.instant('GENERAL.SOC'),
                                    data: data,
                                });
                                this.colors.push({
                                    backgroundColor: 'rgba(0,0,0,0)',
                                    borderColor: 'rgba(0,223,0,1)',
                                });
                            }
                            if (channel == lowThreshold) {
                                datasets.push({
                                    label: this.translate.instant('EDGE.INDEX.WIDGETS.CHP.LOW_THRESHOLD'),
                                    data: data,
                                    borderDash: [3, 3],
                                });
                                this.colors.push({
                                    backgroundColor: 'rgba(0,0,0,0)',
                                    borderColor: 'rgba(0,191,255,1)',
                                });
                            }
                            if (channel == highThreshold) {
                                datasets.push({
                                    label: this.translate.instant('EDGE.INDEX.WIDGETS.CHP.HIGH_THRESHOLD'),
                                    data: data,
                                    borderDash: [3, 3],
                                });
                                this.colors.push({
                                    backgroundColor: 'rgba(0,0,0,0)',
                                    borderColor: 'rgba(0,191,255,1)',
                                });
                            }
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
            this.unit = YAxisTitle.PERCENTAGE;
            this.setOptions(this.options);
        });
    }

    protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {
            const outputChannel = ChannelAddress.fromString(config.getComponentProperties(this.componentId)['outputChannelAddress']);
            const inputChannel = ChannelAddress.fromString(config.getComponentProperties(this.componentId)['inputChannelAddress']);
            const result: ChannelAddress[] = [
                outputChannel,
                inputChannel,
                new ChannelAddress(this.componentId, '_PropertyHighThreshold'),
                new ChannelAddress(this.componentId, '_PropertyLowThreshold'),
            ];
            resolve(result);
        });
    }

    protected setLabel() {
        this.options = this.createDefaultChartOptions();
    }

    public getChartHeight(): number {
        return window.innerHeight / 1.3;
    }
}
