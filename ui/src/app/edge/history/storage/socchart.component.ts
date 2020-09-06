import { AbstractHistoryChart } from '../abstracthistorychart';
import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, EdgeConfig, Service, Utils } from '../../../shared/shared';
import { ChartOptions, Data, DEFAULT_TIME_CHART_OPTIONS, TooltipItem, Dataset } from './../shared';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { formatNumber } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'socStorageChart',
    templateUrl: '../abstracthistorychart.html'
})
export class SocStorageChartComponent extends AbstractHistoryChart implements OnInit, OnChanges {

    @Input() private period: DefaultTypes.HistoryPeriod | null = null;

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
        this.spinnerId = "storage-single-chart";
        this.service.startSpinner(this.spinnerId);
        this.service.setCurrentComponent('', this.route);
        this.subscribeChartRefresh();
    }

    ngOnDestroy() {
        this.unsubscribeChartRefresh();
    }

    protected updateChart() {
        this.loading = true;
        this.service.startSpinner(this.spinnerId);
        this.colors = [];
        if (this.period != null) {
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
                        let datasets: Dataset[] = [];

                        let moreThanOneESS = Object.keys(result.data).length > 1 ? true : false;

                        this.getChannelAddresses(edge, config).then(channelAddresses => {
                            channelAddresses.forEach(channelAddress => {
                                let component = config.getComponent(channelAddress.componentId);
                                let data = result.data[channelAddress.toString()].map(value => {
                                    if (value == null) {
                                        return null
                                    } else if (value > 100 || value < 0) {
                                        return null;
                                    } else {
                                        return value;
                                    }
                                });
                                if (!data) {
                                    return;
                                } else {
                                    if (channelAddress.channelId == 'EssSoc') {
                                        datasets.push({
                                            label: (moreThanOneESS ? this.translate.instant('General.total') : this.translate.instant('General.soc')),
                                            data: data,
                                            hidden: false
                                        })
                                        this.colors.push({
                                            backgroundColor: 'rgba(0,223,0,0.05)',
                                            borderColor: 'rgba(0,223,0,1)',
                                        })
                                    }
                                    if (channelAddress.channelId == 'Soc' && moreThanOneESS) {
                                        datasets.push({
                                            label: (channelAddress.componentId == component.alias ? component.id : component.alias),
                                            data: data,
                                            hidden: false
                                        })
                                        this.colors.push({
                                            backgroundColor: 'rgba(128,128,128,0.05)',
                                            borderColor: 'rgba(128,128,128,1)',
                                        })
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
    }

    protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {
            let channeladdresses: ChannelAddress[] = [];
            channeladdresses.push(new ChannelAddress('_sum', 'EssSoc'))
            if (config.getComponentIdsImplementingNature('io.openems.edge.ess.api.SymmetricEss').length > 1) {
                config.getComponentsImplementingNature("io.openems.edge.ess.api.SymmetricEss").filter(component => !(component.factoryId == 'Ess.Cluster')).forEach(component => {
                    channeladdresses.push(new ChannelAddress(component.id, 'Soc'))
                })
            }
            resolve(channeladdresses);
        })
    }

    protected setLabel() {
        let options = <ChartOptions>Utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS);
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
        return window.innerHeight / 21 * 9;
    }
}