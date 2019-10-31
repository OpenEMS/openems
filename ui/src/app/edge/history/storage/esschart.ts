import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChannelAddress, Edge, EdgeConfig, Service, Utils, Websocket } from '../../../shared/shared';
import { ChartOptions, Data, DEFAULT_TIME_CHART_OPTIONS, TooltipItem } from '../shared';
import { AbstractHistoryChart } from '../abstracthistorychart';

@Component({
    selector: 'storageESSChart',
    templateUrl: '../abstracthistorychart.html'
})
export class StorageESSChartComponent extends AbstractHistoryChart implements OnInit, OnChanges {


    @Input() private period: DefaultTypes.HistoryPeriod;
    @Input() private componentId: string;

    moreThanOneProducer: boolean = null;

    ngOnChanges() {
        this.updateChart();
    };

    constructor(
        protected service: Service,
        private route: ActivatedRoute,
        private translate: TranslateService,
        private websocket: Websocket,
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

                    Object.keys(result.data).forEach((channel) => {
                        let address = ChannelAddress.fromString(channel);
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
                        if (address.channelId == "ActivePower") {
                            datasets.push({
                                label: this.translate.instant('General.ChargePower') + (address.componentId == component.alias ? ' (' + component.id + ')' : ' (' + component.alias + ')'),
                                data: chargeData,
                                hidden: false
                            });
                            this.colors.push({
                                backgroundColor: 'rgba(0,223,0,0.05)',
                                borderColor: 'rgba(0,223,0,1)',
                            })
                            datasets.push({
                                label: this.translate.instant('General.DischargePower') + (address.componentId == component.alias ? ' (' + component.id + ')' : ' (' + component.alias + ')'),
                                data: dischargeData,
                                hidden: false
                            });
                            this.colors.push({
                                backgroundColor: 'rgba(200,0,0,0.05)',
                                borderColor: 'rgba(200,0,0,1)',
                            });
                        }
                        if (this.componentId + '/ActivePowerL1' && this.componentId + '/ActivePowerL2' && this.componentId + '/ActivePowerL3' in result.data) {
                            // Phases
                            if (address.channelId == 'ActivePowerL1') {
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
                            if (address.channelId == 'ActivePowerL2') {
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
                            if (address.channelId == 'ActivePowerL3') {
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
                                    label: this.translate.instant('General.DischargePower') + ' ' + this.translate.instant('General.Phase') + ' ' + 'L3',
                                    data: dischargeData
                                });
                                this.colors.push({
                                    backgroundColor: 'rgba(255,165,0,0.1)',
                                    borderColor: 'rgba(255,165,0,1)',
                                });
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
        let component = config.getComponent(this.componentId);
        let factoryID = component.factoryId;
        let factory = config.factories[factoryID];
        return new Promise((resolve, reject) => {
            let result: ChannelAddress[] = [
                new ChannelAddress(this.componentId, 'ActivePower'),
            ];
            if ((factory.natureIds.includes("io.openems.edge.ess.api.AsymmetricEss"))) {
                result.push(
                    new ChannelAddress(component.id, 'ActivePowerL1'),
                    new ChannelAddress(component.id, 'ActivePowerL2'),
                    new ChannelAddress(component.id, 'ActivePowerL3')
                );
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



    public getChartHeight(): number {
        return window.innerHeight / 4;
    }
}