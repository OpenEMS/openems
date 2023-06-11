import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { QueryHistoricTimeseriesDataResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChannelAddress, Edge, EdgeConfig, Service, Utils } from '../../../shared/shared';
import { AbstractHistoryChart } from '../abstracthistorychart';
import { Data, TooltipItem } from '../shared';

@Component({
    selector: 'consumptionTotalChart',
    templateUrl: '../abstracthistorychart.html'
})
export class ConsumptionTotalChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

    @Input() public period: DefaultTypes.HistoryPeriod;
    @Input() public showPhases: boolean;

    // reference to the Utils method to access via html
    public isLastElement = Utils.isLastElement;

    public ngOnChanges() {
        this.updateChart();
    };

    constructor(
        protected service: Service,
        protected translate: TranslateService,
        private route: ActivatedRoute,
    ) {
        super("consumption-total-chart", service, translate);
    }

    public ngOnInit() {
        this.startSpinner();
        this.service.setCurrentComponent('', this.route);
        this.setLabel();
    }

    public ngOnDestroy() {
        this.unsubscribeChartRefresh();
    }

    protected updateChart() {
        this.autoSubscribeChartRefresh();
        this.startSpinner();
        this.loading = true;
        this.queryHistoricTimeseriesData(this.period.from, this.period.to).then(response => {
            this.service.getCurrentEdge().then(edge => {
                this.service.getConfig().then(config => {
                    this.colors = [];
                    let result = (response as QueryHistoricTimeseriesDataResponse).result;

                    // convert labels
                    let labels: Date[] = [];
                    for (let timestamp of result.timestamps) {
                        labels.push(new Date(timestamp));
                    }
                    this.labels = labels;

                    // gather EVCS consumption
                    let totalEvcsConsumption: number[] = [];
                    config.getComponentsImplementingNature("io.openems.edge.evcs.api.Evcs")
                        .filter(component => !(
                            component.factoryId == 'Evcs.Cluster' ||
                            component.factoryId == 'Evcs.Cluster.PeakShaving' ||
                            component.factoryId == 'Evcs.Cluster.SelfConsumption'))
                        .forEach(component => {
                            if (result.data[component.id + '/ChargePower']) {
                                totalEvcsConsumption = result.data[component.id + '/ChargePower'].map((value, index) => {
                                    return Utils.addSafely(totalEvcsConsumption[index], value / 1000);
                                });
                            }
                        });

                    // gather consumptionMetered consumption
                    let totalMeteredConsumption: number[] = [];
                    config.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
                        .filter(component => component.isEnabled && config.isTypeConsumptionMetered(component))
                        .forEach(component => {
                            if (result.data[component.id + '/ActivePower']) {
                                totalMeteredConsumption = result.data[component.id + "/ActivePower"].map((value, index) => {
                                    return Utils.addSafely(totalMeteredConsumption[index], value / 1000);
                                });
                            }
                        });

                    // gather other Consumption (Total - EVCS - consumptionMetered)
                    let otherConsumption: number[] = [];
                    otherConsumption = result.data['_sum/ConsumptionActivePower'].map((value, index) => {

                        if (value != null) {
                            // Check if either totalEvcsConsumption or totalMeteredConsumption is not null and the endValue not below 0
                            return Utils.roundSlightlyNegativeValues(Utils.subtractSafely(Utils.subtractSafely(value / 1000, totalEvcsConsumption[index]), totalMeteredConsumption[index]));
                        }
                    });
                    // convert datasets
                    let datasets = [];

                    //RGB Colors for evcs'
                    let evcsRedColor = 255;
                    let evcsGreenColor = 255;
                    let evcsBlueColor = 255;
                    let evcsIndex = 0;

                    //EVCS Component Array
                    let regularEvcsComponents = config.getComponentsImplementingNature("io.openems.edge.evcs.api.Evcs")
                        .filter(component => !(
                            component.factoryId == 'Evcs.Cluster' ||
                            component.factoryId == 'Evcs.Cluster.PeakShaving'
                            || component.factoryId == 'Evcs.Cluster.SelfConsumption'
                        ));

                    this.getChannelAddresses(edge, config).then(channelAddresses => {
                        channelAddresses.forEach((channelAddress, index) => {
                            if (!Object.keys(result.data).includes(channelAddress.toString())) {
                                result.data[channelAddress.toString()] = [].fill(null);
                            }
                            let component = config.getComponent(channelAddress.componentId);
                            let data = result.data[channelAddress.toString()].map(value => {
                                if (value == null) {
                                    return null;
                                } else {
                                    return value / 1000;
                                }
                            });
                            if (!data) {
                                return;
                            } else {
                                if (channelAddress.channelId == 'ConsumptionActivePower') {
                                    datasets.push({
                                        label: this.translate.instant('General.total'),
                                        data: data,
                                        hidden: false
                                    });
                                    this.colors.push({
                                        backgroundColor: 'rgba(253,197,7,0.05)',
                                        borderColor: 'rgba(253,197,7,1)',
                                    });
                                }

                                // Phases View is shown
                                if (this.showPhases == true) {
                                    if (channelAddress.channelId == 'ConsumptionActivePowerL1') {
                                        datasets.push({
                                            label: this.translate.instant('General.phase') + ' ' + 'L1',
                                            data: data
                                        });
                                        this.colors.push(this.phase1Color);
                                    }
                                    if (channelAddress.channelId == 'ConsumptionActivePowerL2') {
                                        datasets.push({
                                            label: this.translate.instant('General.phase') + ' ' + 'L2',
                                            data: data
                                        });
                                        this.colors.push(this.phase2Color);
                                    }
                                    if (channelAddress.channelId == 'ConsumptionActivePowerL3') {
                                        datasets.push({
                                            label: this.translate.instant('General.phase') + ' ' + 'L3',
                                            data: data
                                        });
                                        this.colors.push(this.phase3Color);
                                    }

                                    // consumptionMeter Phases
                                    if (channelAddress.channelId == 'ActivePowerL1') {
                                        datasets.push({
                                            label: component.alias + ' Phase ' + 'L1',
                                            data: data
                                        });
                                        this.colors.push({
                                            backgroundColor: 'rgba(255,193,193,0.1)',
                                            borderColor: 'rgba(139,35,35,1)',
                                        });
                                    }
                                    if (channelAddress.channelId == 'ActivePowerL2') {
                                        datasets.push({
                                            label: component.alias + ' Phase ' + 'L2',
                                            data: data
                                        });
                                        this.colors.push({
                                            backgroundColor: 'rgba(198,226,255,0.1)',
                                            borderColor: 'rgba(198,226,255,1)',
                                        });
                                    }
                                    if (channelAddress.channelId == 'ActivePowerL3') {
                                        datasets.push({
                                            label: component.alias + ' Phase ' + 'L3',
                                            data: data
                                        });
                                        this.colors.push({
                                            backgroundColor: 'rgba(121,205,205,0.1)',
                                            borderColor: 'rgba(121,205,205,1)',
                                        });
                                    }
                                }

                                if (regularEvcsComponents.length > 1 && totalEvcsConsumption.length != 0) {
                                    if (!this.translate.instant('Edge.Index.Widgets.EVCS.chargingStation') + ' (' + this.translate.instant('General.total') + ')' in datasets) {
                                        datasets.push({
                                            label: this.translate.instant('Edge.Index.Widgets.EVCS.chargingStation') + ' (' + this.translate.instant('General.total') + ')',
                                            data: totalEvcsConsumption,
                                            hidden: false
                                        });
                                        this.colors.push({
                                            backgroundColor: 'rgba(45,143,171,0.05)',
                                            borderColor: 'rgba(45,143,171,1)'
                                        });
                                    }
                                    if (channelAddress.channelId == "ChargePower") {
                                        if (evcsIndex == 0) {
                                            evcsRedColor = 0;
                                            evcsGreenColor = 153;
                                            evcsBlueColor = 153;
                                        } else if (evcsIndex == regularEvcsComponents.length - 1) {
                                            evcsRedColor = 204;
                                            evcsGreenColor = 0;
                                            evcsBlueColor = 0;
                                        } else {
                                            evcsRedColor = (evcsIndex + 1) % 3 == 0 ? 255 : 204 / (evcsIndex + 1);
                                            evcsGreenColor = 0;
                                            evcsBlueColor = (evcsIndex + 1) % 3 == 0 ? 127 : 408 / (evcsIndex + 1);
                                        }
                                        evcsIndex += 1;
                                        datasets.push({
                                            label: component.alias,
                                            data: data,
                                            hidden: false
                                        });
                                        this.colors.push({
                                            backgroundColor: 'rgba(' + evcsRedColor.toString() + ',' + evcsGreenColor.toString() + ',' + evcsBlueColor.toString() + ',0.05)',
                                            borderColor: 'rgba(' + evcsRedColor.toString() + ',' + evcsGreenColor.toString() + ',' + evcsBlueColor.toString() + ',1)'
                                        });
                                    }
                                } else if (regularEvcsComponents.length == 1) {
                                    if (channelAddress.channelId == "ChargePower") {
                                        datasets.push({
                                            label: (component.id == component.alias ? component.id : component.alias),
                                            data: data,
                                            hidden: false
                                        });
                                        this.colors.push({
                                            backgroundColor: 'rgba(45,143,171,0.05)',
                                            borderColor: 'rgba(45,143,171,1)'
                                        });
                                    }
                                }

                                if (totalMeteredConsumption.length != 0) {
                                    if (channelAddress.channelId == "ActivePower") {
                                        datasets.push({
                                            label: (component.id == component.alias ? component.id : component.alias),
                                            data: data,
                                            hidden: false
                                        });
                                        this.colors.push({
                                            backgroundColor: 'rgba(220,20,60,0.05)',
                                            borderColor: 'rgba(220,20,60,1)'
                                        });
                                    }
                                }

                                // show OtherConsumption 
                                if (Utils.isLastElement(channelAddress, channelAddresses) && (

                                    // Check if Evcs is in config
                                    config.getComponentsImplementingNature("io.openems.edge.evcs.api.Evcs")
                                        .filter(component => !(
                                            component.factoryId == 'Evcs.Cluster' ||
                                            component.factoryId == 'Evcs.Cluster.PeakShaving' ||
                                            component.factoryId == 'Evcs.Cluster.SelfConsumption'
                                        )).length > 0
                                    ||

                                    // Check if ElectricityMeter is in config
                                    config.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
                                        .filter(component =>
                                            component.isEnabled &&
                                            config.isTypeConsumptionMetered(component)
                                        ).length > 0
                                )) {
                                    datasets.push({
                                        label: this.translate.instant('General.otherConsumption'),
                                        data: otherConsumption,
                                        hidden: false
                                    });
                                    this.colors.push({
                                        backgroundColor: 'rgba(0,223,0,0.00)',
                                        borderColor: 'rgba(0,223,0,05)',
                                    });
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
                new ChannelAddress('_sum', 'ConsumptionActivePower'),
                new ChannelAddress('_sum', 'ConsumptionActivePowerL1'),
                new ChannelAddress('_sum', 'ConsumptionActivePowerL2'),
                new ChannelAddress('_sum', 'ConsumptionActivePowerL3'),
            ];
            config.getComponentsImplementingNature("io.openems.edge.evcs.api.Evcs")
                .filter(component => !(
                    component.factoryId == 'Evcs.Cluster' ||
                    component.factoryId == 'Evcs.Cluster.PeakShaving' ||
                    component.factoryId == 'Evcs.Cluster.SelfConsumption'))
                .forEach(component => {
                    result.push(new ChannelAddress(component.id, 'ChargePower'));
                });

            let consumptionMeters = config.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
                .filter(component => component.isEnabled && config.isTypeConsumptionMetered(component));

            for (let meter of consumptionMeters) {
                result.push(new ChannelAddress(meter.id, 'ActivePower'));
                result.push(new ChannelAddress(meter.id, 'ActivePowerL1'));
                result.push(new ChannelAddress(meter.id, 'ActivePowerL2'));
                result.push(new ChannelAddress(meter.id, 'ActivePowerL3'));
            }
            resolve(result);
        });
    }

    protected setLabel() {
        let options = this.createDefaultChartOptions();
        options.scales.yAxes[0].scaleLabel.labelString = "kW";
        options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
            let label = data.datasets[tooltipItem.datasetIndex].label;
            let value = tooltipItem.yLabel;
            return label + ": " + formatNumber(value, 'de', '1.0-2') + " kW";
        };
        this.options = options;
    }

    public getChartHeight(): number {
        return window.innerHeight / 1.3;
    }
}