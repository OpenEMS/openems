import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { ActivatedRoute, Data } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { QueryHistoricTimeseriesDataResponse } from '../../../../../shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { ChannelAddress, Edge, Service, Utils, Widget, Websocket } from '../../../../../shared/shared';
import { Dataset, EMPTY_DATASET, ChartOptions, DEFAULT_TIME_CHART_OPTIONS, TooltipItem } from 'src/app/edge/history/chart/shared';
import { AbstractHistoryChart } from 'src/app/edge/history/abstracthistorychart';

@Component({
    selector: 'evcs',
    templateUrl: './evcs-modal.page.html'
})
export class EvcsChartComponent extends AbstractHistoryChart implements OnInit, OnChanges {

    private static readonly SELECTOR = "evcs";

    ngOnChanges() {
        this.updateChart();
    };

    constructor(
        protected service: Service,
        private websocket: Websocket,
        private route: ActivatedRoute,
        private translate: TranslateService
    ) {
        super(service);
    }

    public loading: boolean = true;
    public edge: Edge = null;
    public widgets: Widget[] = [];
    public chargingStations: Widget[] = [];
    protected labels: Date[] = [];
    protected datasets: Dataset[] = EMPTY_DATASET;
    protected options: ChartOptions;
    protected colors = [{
        // Actual Power
        backgroundColor: 'rgba(173,255,47,0.1)',
        borderColor: 'rgba(173,255,47,1)',
    }];

    ngOnInit() {
        this.service.getWidgets().then(widgets => this.widgets = widgets);
        this.widgets.forEach(widget => {
            if (widget.componentId = "'io.openems.edge.evcs.api.Evcs'") {
                this.chargingStations.push(widget);
            }
        });
        alert(this.chargingStations.length);
        alert("hallo");
        alert("funktioniere");
        // Subscribe to CurrentData
        this.service.setCurrentEdge(this.route).then(edge => {
            this.edge = edge;
            this.chargingStations.forEach(station => {
                edge.subscribeChannels(this.websocket, EvcsChartComponent.SELECTOR + station.componentId, [
                    // Evcs
                    new ChannelAddress(station.componentId, 'ChargePower'),
                    new ChannelAddress(station.componentId, 'HardwarePowerLimit'),
                    new ChannelAddress(station.componentId, 'Phases'),
                    new ChannelAddress(station.componentId, 'Plug'),
                    new ChannelAddress(station.componentId, 'Status'),
                    new ChannelAddress(station.componentId, 'State'),
                    new ChannelAddress(station.componentId, 'EnergySession'),
                    new ChannelAddress(station.componentId, 'MinimumPower'),
                    new ChannelAddress(station.componentId, 'MaximumPower')
                ]);
            });
        });
        let options = <ChartOptions>Utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS);
        options.scales.yAxes[0].scaleLabel.labelString = "kW";
        options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
            let label = data.datasets[tooltipItem.datasetIndex].label;
            let value = tooltipItem.yLabel;
            return label + ": " + formatNumber(value, 'de', '1.0-2') + " kW";
        }
        this.options = options;
    }

    private updateChart() {
        this.loading = true;
        let datasets = [];
        this.chargingStations.forEach(stations => {
            let chargePower = this.edge.currentData[stations.componentId + '/ChargePower'];
            datasets.push({
                label: this.translate.instant('General.ActualPower') + (this.chargingStations.length > 1 ? ' (' + stations.componentId + ')' : ''),
                data: chargePower
            });
        });

        this.loading = false;

        /*
        this.queryHistoricTimeseriesData(this.fromDate, this.toDate).then(response => {
            let result = (response as QueryHistoricTimeseriesDataResponse).result;

            // convert labels
            let labels: Date[] = [];
            for (let timestamp of result.timestamps) {
                labels.push(new Date(timestamp));
            }
            this.labels = labels;

            // show Component-ID if there is more than one Channel
            let showComponentId = Object.keys(result.data).length > 1 ? true : false;

            // convert datasets
            let datasets = [];
            for (let channel in result.data) {
                let address = ChannelAddress.fromString(channel);
                let data = result.data[channel].map(value => {
                    if (value == null) {
                        return null
                    } else {
                        return value / 1000; // convert to kW
                    }
                });
                datasets.push({
                    label: this.translate.instant('General.ActualPower') + (showComponentId ? ' (' + address.componentId + ')' : ''),
                    data: data
                });
            }
            this.datasets = datasets;

            this.loading = false;

        }).catch(reason => {
            console.error(reason); // TODO error message
            this.initializeChart();
            return;
        });
        */
    }

    protected getChannelAddresses(edge: Edge): Promise<ChannelAddress[]> {
        return new Promise((resolve, reject) => {
            this.service.getConfig().then(config => {
                let channeladdresses = [];
                // find all EVCS components
                for (let componentId of config.getComponentIdsImplementingNature("io.openems.edge.evcs.api.Evcs")) {
                    channeladdresses.push(new ChannelAddress(componentId, 'ChargePower'));
                }
                resolve(channeladdresses);
            }).catch(reason => reject(reason));
        });
    }

    private initializeChart() {
        this.datasets = EMPTY_DATASET;
        this.labels = [];
        this.loading = false;
    }
}
