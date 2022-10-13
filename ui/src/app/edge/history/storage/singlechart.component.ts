import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChannelAddress, Edge, EdgeConfig, Service, Utils } from '../../../shared/shared';
import { AbstractHistoryChart } from '../abstracthistorychart';
import { Data, TooltipItem } from '../shared';

@Component({
    selector: 'storageSingleChart',
    templateUrl: '../abstracthistorychart.html'
})
export class StorageSingleChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

    @Input() public period: DefaultTypes.HistoryPeriod;
    @Input() public showPhases: boolean;

    ngOnChanges() {
        this.updateChart();
    }

    constructor(
        protected service: Service,
        protected translate: TranslateService,
        private route: ActivatedRoute
    ) {
        super("storage-single-chart", service, translate);
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
        this.colors = [];
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

                    // calculate total charge and discharge
                    let effectivePower = [];
                    let effectivePowerL1 = [];
                    let effectivePowerL2 = [];
                    let effectivePowerL3 = [];

                    if (config.getComponentsImplementingNature('io.openems.edge.ess.dccharger.api.EssDcCharger').length > 0) {
                        result.data['_sum/ProductionDcActualPower'].forEach((value, index) => {
                            if (result.data['_sum/ProductionDcActualPower'][index] != null) {
                                effectivePower[index] = Utils.subtractSafely(result.data['_sum/EssActivePower'][index], value);
                                effectivePowerL1[index] = Utils.subtractSafely(result.data['_sum/EssActivePowerL1'][index], value / 3);
                                effectivePowerL2[index] = Utils.subtractSafely(result.data['_sum/EssActivePowerL2'][index], value / 3);
                                effectivePowerL3[index] = Utils.subtractSafely(result.data['_sum/EssActivePowerL3'][index], value / 3);
                            }
                        });
                    } else {
                        effectivePower = result.data['_sum/EssActivePower'];
                        effectivePowerL1 = result.data['_sum/EssActivePowerL1'];
                        effectivePowerL2 = result.data['_sum/EssActivePowerL2'];
                        effectivePowerL3 = result.data['_sum/EssActivePowerL3'];
                    }

                    let totalData = effectivePower.map(value => {
                        if (value == null) {
                            return null
                        } else {
                            return value / 1000; // convert to kW
                        }
                    })

                    let totalDataL1 = effectivePowerL1.map(value => {
                        if (value == null) {
                            return null
                        } else {
                            return value / 1000 // convert to kW
                        }
                    })

                    let totalDataL2 = effectivePowerL2.map(value => {
                        if (value == null) {
                            return null
                        } else {
                            return value / 1000 // convert to kW
                        }
                    })

                    let totalDataL3 = effectivePowerL3.map(value => {
                        if (value == null) {
                            return null
                        } else {
                            return value / 1000 // convert to kW
                        }
                    })

                    this.getChannelAddresses(edge, config).then(channelAddresses => {
                        channelAddresses.forEach(channelAddress => {
                            let data = result.data[channelAddress.toString()]?.map(value => {
                                if (value == null) {
                                    return null
                                } else {
                                    return value / 1000; // convert to kW
                                }
                            })
                            if (!data) {
                                return;
                            } else {
                                if (channelAddress.channelId == "EssActivePower") {
                                    datasets.push({
                                        label: this.translate.instant('General.chargeDischarge'),
                                        data: totalData
                                    });
                                    this.colors.push({
                                        backgroundColor: 'rgba(0,223,0,0.05)',
                                        borderColor: 'rgba(0,223,0,1)',
                                    })
                                }
                                if ('_sum/EssActivePowerL1' && '_sum/EssActivePowerL2' && '_sum/EssActivePowerL3' in result.data && this.showPhases == true) {
                                    if (channelAddress.channelId == 'EssActivePowerL1') {
                                        datasets.push({
                                            label: this.translate.instant('General.phase') + ' ' + 'L1',
                                            data: totalDataL1
                                        });
                                        this.colors.push(this.phase1Color);
                                    } if (channelAddress.channelId == 'EssActivePowerL2') {
                                        datasets.push({
                                            label: this.translate.instant('General.phase') + ' ' + 'L2',
                                            data: totalDataL2
                                        });
                                        this.colors.push(this.phase2Color);
                                    } if (channelAddress.channelId == 'EssActivePowerL3') {
                                        datasets.push({
                                            label: this.translate.instant('General.phase') + ' ' + 'L3',
                                            data: totalDataL3
                                        });
                                        this.colors.push(this.phase3Color);
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
                new ChannelAddress('_sum', 'EssActivePower'),
                new ChannelAddress('_sum', 'ProductionDcActualPower'),
                new ChannelAddress('_sum', 'EssActivePowerL1'),
                new ChannelAddress('_sum', 'EssActivePowerL2'),
                new ChannelAddress('_sum', 'EssActivePowerL3'),
            ];
            resolve(result);
        })
    }

    protected setLabel() {
        let translate = this.translate; // enables access to TranslateService
        let options = this.createDefaultChartOptions();
        options.scales.yAxes[0].scaleLabel.labelString = "kW";
        options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
            let label = data.datasets[tooltipItem.datasetIndex].label;
            let value = tooltipItem.yLabel;
            // 0.005 to prevent showing Charge or Discharge if value is e.g. 0.00232138
            if (value < -0.005) {
                if (label.includes(translate.instant('General.phase'))) {
                    label += ' ' + translate.instant('General.chargePower');
                } else {
                    label = translate.instant('General.chargePower');
                }
            } else if (value > 0.005) {
                if (label.includes(translate.instant('General.phase'))) {
                    label += ' ' + translate.instant('General.dischargePower');
                } else {
                    label = translate.instant('General.dischargePower');
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