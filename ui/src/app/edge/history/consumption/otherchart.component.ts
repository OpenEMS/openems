import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { QueryHistoricTimeseriesDataResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';

import { ChannelAddress, Edge, EdgeConfig, Service, Utils } from '../../../shared/shared';
import { AbstractHistoryChart } from '../abstracthistorychart';
import * as Chart from 'chart.js';
@Component({
    selector: 'consumptionOtherChart',
    templateUrl: '../abstracthistorychart.html'
})
export class ConsumptionOtherChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

    @Input() public period: DefaultTypes.HistoryPeriod;

    ngOnChanges() {
        this.updateChart();
    };

    constructor(
        protected override service: Service,
        protected override translate: TranslateService,
        private route: ActivatedRoute
    ) {
        super("consumption-other-chart", service, translate);
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
        this.loading = true;
        this.queryHistoricTimeseriesData(this.period.from, this.period.to).then(response => {
            this.service.getConfig().then(config => {
                this.colors = [];
                let result = (response as QueryHistoricTimeseriesDataResponse).result;
                // convert labels
                let labels: Date[] = [];
                for (let timestamp of result.timestamps) {
                    labels.push(new Date(timestamp));
                }
                this.labels = labels;

                // convert datasets
                let datasets = [];

                // gather EVCS consumption
                let totalEvcsConsumption: number[] = [];
                config.getComponentsImplementingNature("io.openems.edge.evcs.api.Evcs")
                    .filter(component => !(
                        component.factoryId == 'Evcs.Cluster' ||
                        component.factoryId == 'Evcs.Cluster.PeakShaving' ||
                        component.factoryId == 'Evcs.Cluster.SelfConsumption')
                    ).forEach(component => {
                        if (result.data[component.id + '/ChargePower']) {
                            totalEvcsConsumption = result.data[component.id + '/ChargePower'].map((value, index) => {
                                return Utils.addSafely(totalEvcsConsumption[index], value / 1000);
                            });
                        }
                    });

                let totalMeteredConsumption: number[] = [];
                config.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
                    .filter(component => component.isEnabled && config.isTypeConsumptionMetered(component))
                    .forEach(component => {
                        if (result.data[component.id + "/ActivePower"]) {
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

                // show other consumption if at least one of the arrays is not empty
                if (totalEvcsConsumption.length > 0 || totalMeteredConsumption.length > 0) {
                    datasets.push({
                        label: this.translate.instant('General.consumption'),
                        data: otherConsumption,
                        hidden: false
                    });
                    this.colors.push({
                        backgroundColor: 'rgba(253,197,7,0.05)',
                        borderColor: 'rgba(253,197,7,1)'
                    });
                }
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
    }

    protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {
            let result: ChannelAddress[] = [
                new ChannelAddress('_sum', 'ConsumptionActivePower')
            ];
            config.getComponentsImplementingNature("io.openems.edge.evcs.api.Evcs").filter(component => !(component.factoryId == 'Evcs.Cluster')).forEach(component => {
                result.push(new ChannelAddress(component.id, 'ChargePower'));
            });
            config.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
                .filter(component => component.isEnabled && config.isTypeConsumptionMetered(component))
                .forEach(component => {
                    result.push(new ChannelAddress(component.id, "ActivePower"));
                });
            resolve(result);
        });
    }

    protected setLabel() {
        let options = this.createDefaultChartOptions();
        options.scales.yAxes[0].scaleLabel.labelString = "kW";
        options.plugins.tooltip.callbacks.label = function (tooltipItem: Chart.TooltipItem<any>) {
            // let label = data.datasets[tooltipItem.datasetIndex].label;
            // let value = tooltipItem.yLabel;
            // return label + ": " + formatNumber(value, 'de', '1.0-2') + " kW";
        };
        this.options = options;
    }

    public getChartHeight(): number {
        return window.innerHeight / 21 * 9;
    }
}
