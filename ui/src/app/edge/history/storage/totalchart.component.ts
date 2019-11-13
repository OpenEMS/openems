import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnInit, AfterViewInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { CurrentData } from 'src/app/shared/edge/currentdata';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChannelAddress, Edge, EdgeConfig, Service, Utils } from '../../../shared/shared';
import { ChartOptions, Data, Dataset, DEFAULT_TIME_CHART_OPTIONS, EMPTY_DATASET, TooltipItem } from '../shared';
import { AbstractHistoryChart } from '../abstracthistorychart';
import { QueryHistoricTimeseriesDataResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';

@Component({
    selector: 'storageTotalChart',
    templateUrl: '../abstracthistorychart.html'
})
export class StorageTotalChartComponent extends AbstractHistoryChart implements OnInit, OnChanges {
    @Input() private period: DefaultTypes.HistoryPeriod;
    @Input() private showPhases: boolean;

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
                    this.colors = [];
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
                    if (config.getComponentsImplementingNature('io.openems.edge.ess.dccharger.api.EssDcCharger').length > 0) {
                        effectivePower = result.data['_sum/ProductionDcActualPower'].map((value, index) => {
                            return Utils.subtractSafely(result.data['_sum/EssActivePower'][index], value);
                        });
                    } else {
                        effectivePower = result.data['_sum/EssActivePower'];
                    }
                    let chargeDischargeDataTotal = effectivePower.map(value => {
                        if (value == null) {
                            return null
                        } else if (value == 0) {
                            return 0;
                        } else {
                            return value / 1000; // convert to kW
                        }
                    })

                    Object.keys(result.data).forEach((channel, index) => {
                        let address = ChannelAddress.fromString(channel);
                        let component = config.getComponent(address.componentId);
                        let chargeDischargeData = result.data[channel].map(value => {
                            if (value == null) {
                                return null
                            } else if (value == 0) {
                                return 0;
                            } else {
                                return value / 1000; // convert to kW
                            }
                        })
                        if (address.channelId == "EssActivePower") {
                            datasets.push({
                                label: this.translate.instant('General.ChargeDischarge') + ' (' + this.translate.instant('General.Total') + ')',
                                data: chargeDischargeDataTotal
                            });
                            this.colors.push({
                                backgroundColor: 'rgba(0,223,0,0.05)',
                                borderColor: 'rgba(0,223,0,1)',
                            })
                        } if ('_sum/EssActivePowerL1' && '_sum/EssActivePowerL2' && '_sum/EssActivePowerL3' in result.data && this.showPhases == true) {
                            if (address.channelId == 'EssActivePowerL1') {
                                datasets.push({
                                    label: this.translate.instant('General.Total') + ' ' + this.translate.instant('General.Phase') + ' ' + 'L1',
                                    data: chargeDischargeData
                                });
                                this.colors.push(this.phase1Color);
                            } if (address.channelId == 'EssActivePowerL2') {
                                datasets.push({
                                    label: this.translate.instant('General.Total') + ' ' + this.translate.instant('General.Phase') + ' ' + 'L2',
                                    data: chargeDischargeData
                                });
                                this.colors.push(this.phase2Color);
                            } if (address.channelId == 'EssActivePowerL3') {
                                datasets.push({
                                    label: this.translate.instant('General.Total') + ' ' + this.translate.instant('General.Phase') + ' ' + 'L3',
                                    data: chargeDischargeData
                                });
                                this.colors.push(this.phase3Color);
                            }
                        }
                        if (address.channelId == "ActivePower") {
                            datasets.push({
                                label: this.translate.instant('General.ChargeDischarge') + (address.componentId == component.alias ? ' (' + component.id + ')' : ' (' + component.alias + ')'),
                                data: chargeDischargeData,
                                hidden: false
                            });
                            this.colors.push({
                                backgroundColor: 'rgba(128,128,128,0.05)',
                                borderColor: 'rgba(128,128,128,1)',
                            })
                        }
                        if (component.id + '/ActivePowerL1' && component.id + '/ActivePowerL2' && component.id + '/ActivePowerL3' in result.data && this.showPhases == true) {
                            // Phases
                            if (address.channelId == 'ActivePowerL1') {
                                //Charge
                                datasets.push({
                                    label: (address.componentId == component.alias ? ' (' + component.id + ')' : ' (' + component.alias + ')') + ' ' + this.translate.instant('General.Phase') + ' ' + 'L1',
                                    data: chargeDischargeData
                                });
                                this.colors.push(this.phase1Color);
                            }
                            if (address.channelId == 'ActivePowerL2') {
                                //Charge
                                datasets.push({
                                    label: (address.componentId == component.alias ? ' (' + component.id + ')' : ' (' + component.alias + ')') + ' ' + this.translate.instant('General.Phase') + ' ' + 'L2',
                                    data: chargeDischargeData
                                });
                                this.colors.push(this.phase2Color);
                            }
                            if (address.channelId == 'ActivePowerL3') {
                                //Charge
                                datasets.push({
                                    label: (address.componentId == component.alias ? ' (' + component.id + ')' : ' (' + component.alias + ')') + ' ' + this.translate.instant('General.Phase') + ' ' + 'L3',
                                    data: chargeDischargeData
                                });
                                this.colors.push(this.phase3Color);
                            }
                        }
                        if (address.channelId == "ActualPower") {
                            let chargerData = result.data[channel].map(value => {
                                if (value == null) {
                                    return null
                                } else {
                                    return value / 1000; // convert to kW
                                }
                            });
                            datasets.push({
                                label: this.translate.instant('General.ChargePower') + (address.componentId == component.alias ? ' (' + component.id + ')' : ' (' + component.alias + ')'),
                                data: chargerData,
                                hidden: false
                            });
                            this.colors.push({
                                backgroundColor: 'rgba(255,215,0,0.05)',
                                borderColor: 'rgba(255,215,0,1)',
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
        return new Promise((resolve, reject) => {
            let result: ChannelAddress[] = [
                new ChannelAddress('_sum', 'EssActivePower'),
                new ChannelAddress('_sum', 'ProductionDcActualPower'),
                new ChannelAddress('_sum', 'EssActivePowerL1'),
                new ChannelAddress('_sum', 'EssActivePowerL2'),
                new ChannelAddress('_sum', 'EssActivePowerL3'),
            ];
            let storages = config.getComponentsImplementingNature("io.openems.edge.ess.api.SymmetricEss").filter(component => !component.factoryId.includes("Ess.Cluster"));
            storages.forEach(component => {
                let factoryID = component.factoryId;
                let factory = config.factories[factoryID];
                result.push(new ChannelAddress(component.id, 'ActivePower'));
                if ((factory.natureIds.includes("io.openems.edge.ess.api.AsymmetricEss"))) {
                    result.push(
                        new ChannelAddress(component.id, 'ActivePowerL1'),
                        new ChannelAddress(component.id, 'ActivePowerL2'),
                        new ChannelAddress(component.id, 'ActivePowerL3')
                    );
                }
            })
            let charger = config.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger");
            if (storages.length != 1 && charger.length > 0) {
                charger.forEach(component => {
                    result.push(new ChannelAddress(component.id, 'ActualPower'))
                })
            }
            resolve(result);
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
        return window.innerHeight / 21 * 9;
    }
}