import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { CurrentData } from 'src/app/shared/edge/currentdata';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChannelAddress, Edge, EdgeConfig, Service, Utils } from '../../../shared/shared';
import { ChartOptions, Data, DEFAULT_TIME_CHART_OPTIONS, TooltipItem } from './../shared';
import { AbstractHistoryChart } from '../abstracthistorychart';

@Component({
    selector: 'productionChart',
    templateUrl: '../abstracthistorychart.html'
})
export class ProductionChartComponent extends AbstractHistoryChart implements OnInit, OnChanges {

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

                    // show Component-ID if there is more than one Channel
                    let showComponentId = Object.keys(result.data).length > 1 ? true : false;

                    Object.keys(result.data).forEach((channel, index) => {
                        let address = ChannelAddress.fromString(channel);
                        let data = result.data[channel].map(value => {
                            if (value == null) {
                                return null
                            } else {
                                return value / 1000; // convert to kW
                            }
                        });
                        //more than one Production Unit
                        if (showComponentId) {
                            if (address.channelId == 'ActivePower' || address.channelId == 'ActualPower') {
                                switch (index % 2) {
                                    case 0:
                                        datasets.push({
                                            label: this.translate.instant('General.Production') + (showComponentId ? ' (' + address.componentId + ')' : ''),
                                            data: data
                                        });
                                        this.colors.push({
                                            backgroundColor: 'rgba(255,165,0,0.1)',
                                            borderColor: 'rgba(255,165,0,1)',
                                        });
                                        break;
                                    case 1:
                                        datasets.push({
                                            label: this.translate.instant('General.Production') + (showComponentId ? ' (' + address.componentId + ')' : ''),
                                            data: data
                                        });
                                        this.colors.push({
                                            backgroundColor: 'rgba(255,255,0,0.1)',
                                            borderColor: 'rgba(255,255,0,1)',
                                        });
                                        break;
                                }
                                // different color + label for total production data
                            } else if (address.channelId == 'ProductionActivePower') {
                                datasets.push({
                                    label: this.translate.instant('General.Production') + (showComponentId ? ' (Gesamt)' : ''),
                                    data: data
                                });
                                this.colors.push({
                                    backgroundColor: 'rgba(0,0,0,0.1)',
                                    borderColor: 'rgba(0,0,0,1)',
                                });
                            }
                            // only one production unit
                        } else {
                            datasets.push({
                                label: this.translate.instant('General.Production'),
                                data: data,
                                hidden: false
                            });
                            this.colors.push({
                                backgroundColor: 'rgba(45,143,171,0.05)',
                                borderColor: 'rgba(45,143,171,1)'
                            })
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
        // gathering production meters
        let channeladdresses: ChannelAddress[] = []
        config.getComponentsImplementingNature("io.openems.edge.meter.api.SymmetricMeter").forEach(meter => {
            if (config.isProducer(meter)) {
                channeladdresses.push(new ChannelAddress(meter.id, 'ActivePower'))
            }
        });
        config.getComponentsImplementingNature('io.openems.edge.ess.dccharger.api.EssDcCharger').forEach(charger => {
            channeladdresses.push(new ChannelAddress(charger.id, 'ActualPower'))
        })

        return new Promise((resolve) => {
            if (channeladdresses.length > 1) {
                channeladdresses.push(new ChannelAddress('_sum', 'ProductionActivePower'));
                resolve(channeladdresses);
            } else {
                let result: ChannelAddress[] = [];
                if (config.getComponentsImplementingNature('io.openems.edge.ess.dccharger.api.EssDcCharger').length > 0) {
                    result.push(new ChannelAddress('_sum', 'ProductionDcActualPower'))
                };
                config.getComponentsImplementingNature("io.openems.edge.meter.api.SymmetricMeter").forEach(meter => {
                    if (config.isProducer(meter)) {
                        result.push(new ChannelAddress('_sum', 'ProductionActivePower'));
                    }
                });
                resolve(result);
            }
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
}