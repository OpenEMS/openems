import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { CurrentData } from 'src/app/shared/edge/currentdata';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChannelAddress, Edge, EdgeConfig, Service, Utils } from '../../../shared/shared';
import { ChartOptions, Data, Dataset, DEFAULT_TIME_CHART_OPTIONS, EMPTY_DATASET, TooltipItem } from './../shared';
import { AbstractHistoryChart } from '../abstracthistorychart';
import { QueryHistoricTimeseriesDataResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';

@Component({
    selector: 'storageChart',
    templateUrl: '../abstracthistorychart.html'
})
export class StorageChartComponent extends AbstractHistoryChart implements OnInit, OnChanges {

    @Input() private period: DefaultTypes.HistoryPeriod;
    moreThanOneProducer: boolean = null;

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
                    // calculate total charge and discharge
                    let effectivePower;
                    // show Component-ID if there is more than one Channel
                    let showComponentId = Object.keys(result.data).length > 2 ? true : false;
                    if (config.getComponentsImplementingNature('io.openems.edge.ess.dccharger.api.EssDcCharger').length > 0) {
                        effectivePower = result.data['_sum/ProductionDcActualPower'].map((value, index) => {
                            return Utils.subtractSafely(result.data['_sum/EssActivePower'][index], value);
                        });
                    } else {
                        effectivePower = result.data['_sum/EssActivePower'];
                    }

                    let chargeDataTotal = effectivePower.map(value => {
                        if (value == null) {
                            return null
                        } else if (value < 0) {
                            return value / -1000; // convert to kW;
                        } else {
                            return 0;
                        }
                    })

                    let dischargeDataTotal = effectivePower.map(value => {
                        if (value == null) {
                            return null
                        } else if (value > 0) {
                            return value / 1000; // convert to kW
                        } else {
                            return 0;
                        }
                    })
                    console.log("ISMORE", this.moreThanOneProducer)
                    Object.keys(result.data).forEach((channel, index) => {
                        let address = ChannelAddress.fromString(channel);
                        // config.getComponent(address.componentId).factoryId != 'Ess.Cluster' ?
                        let component = config.getComponent(address.componentId);
                        let dischargeData = result.data[channel].map(value => {
                            if (value == null) {
                                return null
                            } else if (value > 0) {
                                return value / 1000; // convert to kW
                            } else {
                                return 0;
                            }
                        });
                        let chargeData = result.data[channel].map(value => {
                            if (value == null) {
                                return null;
                            } else if (value < 0) {
                                return value / -1000;
                            } else {
                                return 0;
                            }
                        });

                        // Phases
                        if (address.channelId == 'EssActivePowerL1') {
                            //Charge
                            datasets.push({
                                label: this.translate.instant('General.ChargePower') + ' ' + this.translate.instant('General.Phase') + ' ' + 'L1',
                                data: chargeData
                            });
                            this.colors.push({
                                backgroundColor: 'rgba(255,165,0,0.1)',
                                borderColor: 'rgba(255,165,0,1)',
                            });
                            //Discharge
                            datasets.push({
                                label: this.translate.instant('General.DischargePower') + ' ' + this.translate.instant('General.Phase') + ' ' + 'L1',
                                data: dischargeData
                            });
                            this.colors.push({
                                backgroundColor: 'rgba(255,165,0,0.1)',
                                borderColor: 'rgba(255,165,0,1)',
                            });
                        }
                        if (address.channelId == 'EssActivePowerL2') {
                            //Charge
                            datasets.push({
                                label: this.translate.instant('General.ChargePower') + ' ' + this.translate.instant('General.Phase') + ' ' + 'L2',
                                data: chargeData
                            });
                            this.colors.push({
                                backgroundColor: 'rgba(255,165,0,0.1)',
                                borderColor: 'rgba(255,165,0,1)',
                            });
                            //Discharge
                            datasets.push({
                                label: this.translate.instant('General.DischargePower') + ' ' + this.translate.instant('General.Phase') + ' ' + 'L2',
                                data: dischargeData
                            });
                            this.colors.push({
                                backgroundColor: 'rgba(255,165,0,0.1)',
                                borderColor: 'rgba(255,165,0,1)',
                            });
                        }
                        if (address.channelId == 'EssActivePowerL3') {
                            //Charge
                            datasets.push({
                                label: this.translate.instant('General.ChargePower') + ' ' + this.translate.instant('General.Phase') + ' ' + 'L3',
                                data: chargeData
                            });
                            this.colors.push({
                                backgroundColor: 'rgba(255,165,0,0.1)',
                                borderColor: 'rgba(255,165,0,1)',
                            });
                            //Discharge
                            datasets.push({
                                label: this.translate.instant('General.DischargePower') + + ' ' + this.translate.instant('General.Phase') + ' ' + 'L3',
                                data: dischargeData
                            });
                            this.colors.push({
                                backgroundColor: 'rgba(255,165,0,0.1)',
                                borderColor: 'rgba(255,165,0,1)',
                            });
                        }
                        // more than one production unit
                        if (this.moreThanOneProducer == true) {
                            if (address.channelId == "ActivePower" && component.factoryId != 'Ess.Cluster') {
                                switch (index % 2) {
                                    case 0:
                                        datasets.push({
                                            label: this.translate.instant('General.ChargePower') + (showComponentId ? ' (' + (address.componentId == component.alias ? address.componentId : component.alias) + ')' : ''),
                                            data: chargeData
                                        });
                                        this.colors.push({
                                            backgroundColor: 'rgba(255,165,0,0.1)',
                                            borderColor: 'rgba(255,165,0,1)',
                                        });
                                        datasets.push({
                                            label: this.translate.instant('General.DischargePower') + (showComponentId ? ' (' + (address.componentId == component.alias ? address.componentId : component.alias) + ')' : ''),
                                            data: dischargeData
                                        });
                                        this.colors.push({
                                            backgroundColor: 'rgba(255,255,0,0.1)',
                                            borderColor: 'rgba(255,255,0,1)',
                                        });
                                        break;
                                    case 1:
                                        datasets.push({
                                            label: this.translate.instant('General.ChargePower') + (showComponentId ? ' (' + (address.componentId == component.alias ? address.componentId : component.alias) + ')' : ''),
                                            data: chargeData
                                        });
                                        this.colors.push({
                                            backgroundColor: 'rgba(255,165,0,0.1)',
                                            borderColor: 'rgba(255,165,0,1)',
                                        });
                                        datasets.push({
                                            label: this.translate.instant('General.DischargePower') + (showComponentId ? ' (' + (address.componentId == component.alias ? address.componentId : component.alias) + ')' : ''),
                                            data: dischargeData
                                        });
                                        this.colors.push({
                                            backgroundColor: 'rgba(255,255,0,0.1)',
                                            borderColor: 'rgba(255,255,0,1)',
                                        });
                                        break;
                                }
                            } else if (address.channelId == "ActualPower") {
                                switch (index % 2) {
                                    case 0:
                                        datasets.push({
                                            label: this.translate.instant('General.ChargePower') + (showComponentId ? ' (' + (address.componentId == component.alias ? address.componentId : component.alias) + ')' : ''),
                                            data: chargeData
                                        });
                                        this.colors.push({
                                            backgroundColor: 'rgba(255,165,0,0.1)',
                                            borderColor: 'rgba(255,165,0,1)',
                                        });
                                        break;
                                    case 1:
                                        datasets.push({
                                            label: this.translate.instant('General.ChargePower') + (showComponentId ? ' (' + (address.componentId == component.alias ? address.componentId : component.alias) + ')' : ''),
                                            data: chargeData
                                        });
                                        this.colors.push({
                                            backgroundColor: 'rgba(255,255,0,0.1)',
                                            borderColor: 'rgba(255,255,0,1)',
                                        });
                                        break;
                                }
                            } else if (address.channelId == "EssActivePower") {
                                datasets.push({
                                    label: this.translate.instant('General.DischargePower') + ' (' + this.translate.instant('General.Total') + ')',
                                    data: dischargeDataTotal,
                                    hidden: false
                                });
                                this.colors.push({
                                    backgroundColor: 'rgba(200,0,0,0.05)',
                                    borderColor: 'rgba(200,0,0,1)',
                                });
                                datasets.push({
                                    label: this.translate.instant('General.ChargePower') + ' (' + this.translate.instant('General.Total') + ')',
                                    data: chargeDataTotal,
                                    hidden: false
                                });
                                this.colors.push({
                                    backgroundColor: 'rgba(0,223,0,0.05)',
                                    borderColor: 'rgba(0,223,0,1)',
                                })

                            }
                        } else if (this.moreThanOneProducer == false) {
                            //one production unit
                            if (address.channelId == "EssActivePower" || address.channelId == "ProductionDcActualPower") {
                                datasets.push({
                                    label: this.translate.instant('General.DischargePower') + (address.componentId == component.alias ? '' : ' (' + component.alias + ')'),
                                    data: dischargeDataTotal,
                                    hidden: false
                                });
                                this.colors.push({
                                    backgroundColor: 'rgba(200,0,0,0.05)',
                                    borderColor: 'rgba(200,0,0,1)',
                                });
                                datasets.push({
                                    label: this.translate.instant('General.ChargePower') + (address.componentId == component.alias ? '' : ' (' + component.alias + ')'),
                                    data: chargeDataTotal,
                                    hidden: false
                                });
                                this.colors.push({
                                    backgroundColor: 'rgba(0,223,0,0.05)',
                                    borderColor: 'rgba(0,223,0,1)',
                                })
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
        let channeladdresses: ChannelAddress[] = [];
        let isCharger: boolean = null;
        config.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger").forEach(charger => {
            isCharger = true;
            channeladdresses.push(new ChannelAddress(charger.id, 'ActualPower'))
        })
        config.getComponentsImplementingNature("io.openems.edge.ess.api.SymmetricEss").forEach(ess => {
            isCharger = false;
            channeladdresses.push(new ChannelAddress(ess.id, 'ActivePower'))
        })

        return new Promise((resolve, reject) => {
            if (channeladdresses.length > 1) {
                this.moreThanOneProducer = true;
                channeladdresses.push(new ChannelAddress('_sum', 'EssActivePower'))
                channeladdresses.push(new ChannelAddress('_sum', 'ProductionDcActualPower')),
                    //     channeladdresses.push(new ChannelAddress('_sum', 'EssActivePowerL1'))
                    // channeladdresses.push(new ChannelAddress('_sum', 'EssActivePowerL2'))
                    // channeladdresses.push(new ChannelAddress('_sum', 'EssActivePowerL3'))
                    resolve(channeladdresses);
            } else {
                console.log("NEIN")
                this.moreThanOneProducer = false;
                let result: ChannelAddress[] = [
                    new ChannelAddress('_sum', 'EssActivePower'),
                    new ChannelAddress('_sum', 'ProductionActivePower'),
                    new ChannelAddress('_sum', 'ProductionDcActualPower'),
                    // new ChannelAddress('_sum', 'EssActivePowerL1'),
                    // new ChannelAddress('_sum', 'EssActivePowerL1'),
                    // new ChannelAddress('_sum', 'EssActivePowerL1'),
                ];
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

    private getSoc(ids: string[], ignoreIds: string[]): ChannelAddress[] {
        let result: ChannelAddress[] = [];
        for (let id of ids) {
            if (ignoreIds.includes(id)) {
                continue;
            }
            result.push.apply(result, [
                new ChannelAddress(id, 'Soc'),
            ]);
        }
        return result;
    }

    /**
   * Calculates '_sum' values.
   * 
   * @param data 
   */
    private convertDeprecatedData(config: EdgeConfig, data: { [channelAddress: string]: any[] }) {
        let sumEssSoc = [];

        for (let channel of Object.keys(data)) {
            let channelAddress = ChannelAddress.fromString(channel)
            let componentId = channelAddress.componentId;
            let channelId = channelAddress.channelId;
            let natureIds = config.getNatureIdsByComponentId(componentId);

            if (natureIds.includes('EssNature') && channelId == 'Soc') {
                if (sumEssSoc.length == 0) {
                    sumEssSoc = data[channel];
                } else {
                    sumEssSoc = data[channel].map((value, index) => {
                        return Utils.addSafely(sumEssSoc[index], value);
                    });
                }
            }
        }
        data['_sum/EssSoc'] = sumEssSoc.map((value, index) => {
            return Utils.divideSafely(sumEssSoc[index], Object.keys(data).length);
        });

    }

    public getChartHeight(): number {
        return window.innerHeight / 4;
    }
}