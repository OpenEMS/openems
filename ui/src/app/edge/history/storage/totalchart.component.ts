import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChannelAddress, Edge, EdgeConfig, Service, Utils } from '../../../shared/shared';
import { AbstractHistoryChart } from '../abstracthistorychart';
import { Data, TooltipItem } from '../shared';

@Component({
    selector: 'storageTotalChart',
    templateUrl: '../abstracthistorychart.html'
})
export class StorageTotalChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {
    @Input() public period: DefaultTypes.HistoryPeriod;
    @Input() public showPhases: boolean;

    ngOnChanges() {
        this.updateChart();
    };

    constructor(
        protected service: Service,
        protected translate: TranslateService,
        private route: ActivatedRoute
    ) {
        super("storage-total-chart", service, translate);
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
                    this.colors = [];
                    // convert labels
                    let labels: Date[] = [];
                    for (let timestamp of result.timestamps) {
                        labels.push(new Date(timestamp));
                    }
                    this.labels = labels;

                    // calculate total charge and discharge
                    let effectivePower;
                    if (config.getComponentsImplementingNature('io.openems.edge.ess.dccharger.api.EssDcCharger').length > 0) {
                        effectivePower = result.data['_sum/ProductionDcActualPower'].map((value, index) => {
                            return Utils.subtractSafely(result.data['_sum/EssActivePower'][index], value);
                        });
                    } else {
                        effectivePower = result.data['_sum/EssActivePower'];
                    }
                    let totalData = effectivePower.map(value => {
                        if (value == null) {
                            return null;
                        } else {
                            return value / 1000; // convert to kW
                        }
                    });

                    // convert datasets
                    let datasets = [];

                    this.getChannelAddresses(edge, config).then(channelAddresses => {
                        channelAddresses.forEach(channelAddress => {
                            let component = config.getComponent(channelAddress.componentId);
                            let data = result.data[channelAddress.toString()]?.map(value => {
                                if (value == null) {
                                    return null;
                                } else {
                                    return value / 1000; // convert to kW
                                }
                            });
                            let chargerData = result.data[channelAddress.toString()].map(value => {
                                if (value == null) {
                                    return null;
                                } else {
                                    return value / 1000 * -1; // convert to kW + make values negative for fitting totalchart where chargedata is negative
                                }
                            });
                            if (!data) {
                                return;
                            }

                            if (channelAddress.channelId == "EssActivePower") {
                                datasets.push({
                                    label: this.translate.instant('General.TOTAL'),
                                    data: totalData
                                });
                                this.colors.push({
                                    backgroundColor: 'rgba(0,223,0,0.05)',
                                    borderColor: 'rgba(0,223,0,1)'
                                });
                            } if ('_sum/EssActivePowerL1' && '_sum/EssActivePowerL2' && '_sum/EssActivePowerL3' in result.data && this.showPhases == true) {
                                if (channelAddress.channelId == 'EssActivePowerL1') {
                                    datasets.push({
                                        label: this.translate.instant('General.phase') + ' ' + 'L1',
                                        data: data
                                    });
                                    this.colors.push(this.phase1Color);
                                } if (channelAddress.channelId == 'EssActivePowerL2') {
                                    datasets.push({
                                        label: this.translate.instant('General.phase') + ' ' + 'L2',
                                        data: data
                                    });
                                    this.colors.push(this.phase2Color);
                                } if (channelAddress.channelId == 'EssActivePowerL3') {
                                    datasets.push({
                                        label: this.translate.instant('General.phase') + ' ' + 'L3',
                                        data: data
                                    });
                                    this.colors.push(this.phase3Color);
                                }
                            }
                            if (channelAddress.channelId == "ActivePower") {
                                datasets.push({
                                    label: (channelAddress.componentId == component.alias ? component.id : component.alias),
                                    data: data,
                                    hidden: false
                                });
                                this.colors.push({
                                    backgroundColor: 'rgba(45,143,171,0.05)',
                                    borderColor: 'rgba(45,143,171,1)'
                                });
                            }
                            if (component.id + '/ActivePowerL1' && component.id + '/ActivePowerL2' && component.id + '/ActivePowerL3' in result.data && this.showPhases == true) {
                                if (channelAddress.channelId == 'ActivePowerL1') {
                                    datasets.push({
                                        label: (channelAddress.componentId == component.alias ? ' (' + component.id + ')' : ' (' + component.alias + ')') + ' ' + this.translate.instant('General.phase') + ' ' + 'L1',
                                        data: data
                                    });
                                    this.colors.push(this.phase1Color);
                                }
                                if (channelAddress.channelId == 'ActivePowerL2') {
                                    datasets.push({
                                        label: (channelAddress.componentId == component.alias ? ' (' + component.id + ')' : ' (' + component.alias + ')') + ' ' + this.translate.instant('General.phase') + ' ' + 'L2',
                                        data: data
                                    });
                                    this.colors.push(this.phase2Color);
                                }
                                if (channelAddress.channelId == 'ActivePowerL3') {
                                    datasets.push({
                                        label: (channelAddress.componentId == component.alias ? ' (' + component.id + ')' : ' (' + component.alias + ')') + ' ' + this.translate.instant('General.phase') + ' ' + 'L3',
                                        data: data
                                    });
                                    this.colors.push(this.phase3Color);
                                }
                            }
                            if (channelAddress.channelId == "ActualPower") {
                                datasets.push({
                                    label: (channelAddress.componentId == component.alias ? component.id : component.alias),
                                    data: chargerData,
                                    hidden: false
                                });
                                this.colors.push({
                                    backgroundColor: 'rgba(255,215,0,0.05)',
                                    borderColor: 'rgba(255,215,0,1)'
                                });
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
                new ChannelAddress('_sum', 'EssActivePowerL3')
            ];
            config.getComponentsImplementingNature("io.openems.edge.ess.api.SymmetricEss")
                .filter(component => !component.factoryId.includes("Ess.Cluster"))
                .forEach(component => {
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
                });
            let charger = config.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger");
            if (config.getComponentsImplementingNature("io.openems.edge.ess.api.SymmetricEss")
                .filter(component => !component.factoryId.includes("Ess.Cluster")).length != 1 && charger.length > 0) {
                charger.forEach(component => {
                    result.push(new ChannelAddress(component.id, 'ActualPower'));
                });
            }
            resolve(result);
        });
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
                label += ' ' + translate.instant('General.chargePower');
            } else if (value > 0.005) {
                label += ' ' + translate.instant('General.dischargePower');
            }
            return label + ": " + formatNumber(value, 'de', '1.0-2') + " kW";
        };
        this.options = options;
    }

    public getChartHeight(): number {
        return window.innerHeight / 21 * 9;
    }
}
