import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChannelAddress, Edge, EdgeConfig, Service, Utils } from '../../../shared/shared';
import { AbstractHistoryChart } from '../abstracthistorychart';
import { Data, TooltipItem } from '../shared';

@Component({
    selector: 'productionTotalChart',
    templateUrl: '../abstracthistorychart.html'
})
export class ProductionTotalChartComponent extends AbstractHistoryChart implements OnInit, OnChanges {

    @Input() public period: DefaultTypes.HistoryPeriod;
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
        this.spinnerId = 'production-total-chart';
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

                    // calculate total production
                    let effectiveProductionL1 = []
                    let effectiveProductionL2 = []
                    let effectiveProductionL3 = []

                    if (config.getComponentsImplementingNature('io.openems.edge.ess.dccharger.api.EssDcCharger').length > 0) {
                        result.data['_sum/ProductionDcActualPower'].forEach((value, index) => {
                            effectiveProductionL1[index] = Utils.subtractSafely(result.data['_sum/ProductionAcActivePowerL1'][index], value / -3);
                            effectiveProductionL2[index] = Utils.subtractSafely(result.data['_sum/ProductionAcActivePowerL2'][index], value / -3);
                            effectiveProductionL3[index] = Utils.subtractSafely(result.data['_sum/ProductionAcActivePowerL3'][index], value / -3);
                        });
                    } else {
                        effectiveProductionL1 = result.data['_sum/ProductionAcActivePowerL1'];
                        effectiveProductionL2 = result.data['_sum/ProductionAcActivePowerL2'];
                        effectiveProductionL3 = result.data['_sum/ProductionAcActivePowerL3'];
                    }

                    let totalProductionDataL1 = effectiveProductionL1.map(value => {
                        if (value == null) {
                            return null
                        } else {
                            return value / 1000 // convert to kW
                        }
                    })

                    let totalProductionDataL2 = effectiveProductionL2.map(value => {
                        if (value == null) {
                            return null
                        } else {
                            return value / 1000 // convert to kW
                        }
                    })

                    let totalProductionDataL3 = effectiveProductionL3.map(value => {
                        if (value == null) {
                            return null
                        } else {
                            return value / 1000 // convert to kW
                        }
                    })

                    this.getChannelAddresses(edge, config).then(channelAddresses => {
                        channelAddresses.forEach(channelAddress => {
                            let component = config.getComponent(channelAddress.componentId);
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
                                if (channelAddress.channelId == 'ProductionActivePower') {
                                    datasets.push({
                                        label: this.translate.instant('General.total'),
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
                                            label: this.translate.instant('General.phase') + ' ' + 'L1',
                                            data: totalProductionDataL1
                                        });
                                        this.colors.push(this.phase1Color);
                                    }
                                    if (channelAddress.channelId == 'ProductionAcActivePowerL2') {
                                        datasets.push({
                                            label: this.translate.instant('General.phase') + ' ' + 'L2',
                                            data: totalProductionDataL2
                                        });
                                        this.colors.push(this.phase2Color);
                                    }
                                    if (channelAddress.channelId == 'ProductionAcActivePowerL3') {
                                        datasets.push({
                                            label: this.translate.instant('General.phase') + ' ' + 'L3',
                                            data: totalProductionDataL3
                                        });
                                        this.colors.push(this.phase3Color);
                                    }
                                }
                                if (channelAddress.channelId == 'ActivePower') {
                                    datasets.push({
                                        label: (channelAddress.componentId == component.alias ? channelAddress.componentId : component.alias),
                                        data: data
                                    });
                                    this.colors.push({
                                        backgroundColor: 'rgba(253,197,7,0.05)',
                                        borderColor: 'rgba(253,197,7,1)',
                                    });
                                }
                                if (channelAddress.channelId == 'ActualPower') {
                                    datasets.push({
                                        label: (channelAddress.componentId == component.alias ? channelAddress.componentId : component.alias),
                                        data: data
                                    });
                                    this.colors.push({
                                        backgroundColor: 'rgba(0,223,0,0.05)',
                                        borderColor: 'rgba(0,223,0,1)',
                                    });
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
                new ChannelAddress('_sum', 'ProductionActivePower'),
                new ChannelAddress('_sum', 'ProductionDcActualPower'),
                new ChannelAddress('_sum', 'ProductionAcActivePowerL1'),
                new ChannelAddress('_sum', 'ProductionAcActivePowerL2'),
                new ChannelAddress('_sum', 'ProductionAcActivePowerL3'),
            ];
            config.getComponentsImplementingNature("io.openems.edge.meter.api.SymmetricMeter").filter(component => config.isProducer(component)).forEach(productionMeter => {
                result.push(new ChannelAddress(productionMeter.id, 'ActivePower'))
            })
            config.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger").forEach(charger => {
                result.push(new ChannelAddress(charger.id, 'ActualPower'))
            })
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
        return window.innerHeight / 1.3;
    }
}