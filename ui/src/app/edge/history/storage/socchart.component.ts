import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChannelAddress, Edge, EdgeConfig, Service, Utils } from '../../../shared/shared';
import { ChartOptions, Data, DEFAULT_TIME_CHART_OPTIONS, TooltipItem } from './../shared';
import { AbstractHistoryChart } from '../abstracthistorychart';

@Component({
    selector: 'socStorageChart',
    templateUrl: '../abstracthistorychart.html'
})
export class SocStorageChartComponent extends AbstractHistoryChart implements OnInit, OnChanges {

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
            this.service.getCurrentEdge().then(() => {
                this.service.getConfig().then((config) => {
                    let result = response.result;
                    // convert labels
                    let labels: Date[] = [];
                    for (let timestamp of result.timestamps) {
                        labels.push(new Date(timestamp));
                    }
                    this.labels = labels;

                    // convert datasets
                    let datasets = [];

                    // show Component-ID if there is more than one Channel
                    let showComponentId = Object.keys(result.data).length > 1 ? true : false;

                    Object.keys(result.data).forEach((channel, index) => {
                        let address = ChannelAddress.fromString(channel);
                        let data = result.data[channel].map(value => {
                            if (value == null) {
                                return null
                            } else if (value > 100 || value < 0) {
                                return null;
                            } else {
                                return value;
                            }
                        });
                        if (config.components[address['componentId']].factoryId == 'Ess.Cluster') {
                            datasets.push({
                                label: this.translate.instant('General.Soc') + (showComponentId ? ' (' + this.translate.instant('General.Total') + ')' : ''),
                                data: data
                            })
                            this.colors.push({
                                backgroundColor: 'rgba(0,0,0,0.1)',
                                borderColor: 'rgba(0,0,0,1)',
                            });
                        } else {
                            switch (index % 2) {
                                case 0:
                                    datasets.push({
                                        label: this.translate.instant('General.Soc') + (showComponentId ? ' (' + address.componentId + ')' : ''),
                                        data: data
                                    });
                                    this.colors.push({
                                        backgroundColor: 'rgba(255,165,0,0.1)',
                                        borderColor: 'rgba(255,165,0,1)',
                                    });
                                    break;
                                case 1:
                                    datasets.push({
                                        label: this.translate.instant('General.Soc') + (showComponentId ? ' (' + address.componentId + ')' : ''),
                                        data: data
                                    });
                                    this.colors.push({
                                        backgroundColor: 'rgba(255,255,0,0.1)',
                                        borderColor: 'rgba(255,255,0,1)',
                                    });
                                    break;
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
            let channeladdresses: ChannelAddress[] = [];
            if (config.getComponentIdsImplementingNature('io.openems.edge.ess.api.SymmetricEss').length > 1) {
                // find all Ess
                for (let componentid of config.getComponentIdsImplementingNature('io.openems.edge.ess.api.SymmetricEss')) {
                    channeladdresses.push(new ChannelAddress(componentid, 'Soc'))
                }
            } else {
                channeladdresses.push(new ChannelAddress('_sum', 'EssSoc'))
            }
            resolve(channeladdresses);
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