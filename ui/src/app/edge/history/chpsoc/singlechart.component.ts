import { Component, OnInit, OnChanges, Input } from "@angular/core";
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { Service, Edge, ChannelAddress, Utils, EdgeConfig } from 'src/app/shared/shared';
import { ActivatedRoute } from '@angular/router';
import { Dataset, EMPTY_DATASET, ChartOptions, DEFAULT_TIME_CHART_OPTIONS, TooltipItem, Data } from '../shared';
import { QueryHistoricTimeseriesDataResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { formatNumber } from '@angular/common';
import { AbstractHistoryChart } from '../abstracthistorychart';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'chpsocSingleChart',
    templateUrl: '../abstracthistorychart.html'
})
export class ChpSocSingleChartComponent extends AbstractHistoryChart implements OnInit, OnChanges {

    @Input() private period: DefaultTypes.HistoryPeriod;
    @Input() private controllerId: string;
    @Input() private isOnlyChart: boolean;

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

    protected updateChart() {
        this.loading = true;
        this.queryHistoricTimeseriesData(this.period.from, this.period.to).then(response => {
            let result = (response as QueryHistoricTimeseriesDataResponse).result;

            // convert labels
            let labels: Date[] = [];
            for (let timestamp of result.timestamps) {
                labels.push(new Date(timestamp));
            }
            this.labels = labels;

            // show Channel-ID if there is more than one Channel
            let showChannelId: boolean;
            if (this.isOnlyChart == true) {
                showChannelId = false;
            } else if (this.isOnlyChart == false) {
                showChannelId = true;
            }

            // convert datasets
            let datasets = [];

            for (let channel in result.data) {

                let address = ChannelAddress.fromString(channel);
                let data = result.data[channel].map(value => {

                    if (value == null) {
                        return null
                    } else {
                        return Math.floor(parseInt(value)); //  Rounding up the mean values to integer value 
                    }
                });
                datasets.push({
                    label: "Ausgang" + (showChannelId ? ' (' + address.channelId + ')' : ''),
                    data: data
                });
                this.colors.push({
                    backgroundColor: 'rgba(0,191,255,0.05)',
                    borderColor: 'rgba(0,191,255,1)',
                })
            }
            this.datasets = datasets;
            this.loading = false;

        }).catch(reason => {
            console.error(reason); // TODO error message
            this.initializeChart();
            return;
        });
    }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route);
        this.setLabel();
    }

    protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
        return new Promise((resolve, reject) => {
            for (let componentId of config.getComponentsByFactory("Controller.CHP.SoC")) {
                let channeladdresses = [];
                if (this.controllerId == componentId.toString()) {
                    channeladdresses.push(ChannelAddress.fromString(componentId.properties.outputChannelAddress));
                }
                resolve(channeladdresses);
            }
        });
    }

    protected setLabel() {
        let options = <ChartOptions>Utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS);
        options.scales.yAxes[0].scaleLabel.labelString = "On/Off";
        options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
            let label = data.datasets[tooltipItem.datasetIndex].label;
            let value = tooltipItem.yLabel;
            let parsedIntValue = Math.floor(parseInt(formatNumber(value, 'de', '1.0-2')))
            if (parsedIntValue == 1) {
                return label + ": " + "ON";
            } else {
                return label + ":" + "OFF"
            }
        }
        this.options = options;
    }

    public getChartHeight(): number {
        if (this.isOnlyChart == true) {
            return window.innerHeight / 1.3;
        } else if (this.isOnlyChart == false) {
            return window.innerHeight / 21 * 9;
        }
    }
}