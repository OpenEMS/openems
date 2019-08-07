import { Component, OnInit, OnChanges, Input } from "@angular/core";
import { AbstractHistoryChart } from '../abstracthistorychart';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { Service, Edge, ChannelAddress, Utils } from 'src/app/shared/shared';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { Dataset, EMPTY_DATASET, ChartOptions, DEFAULT_TIME_CHART_OPTIONS, TooltipItem, Data } from '../shared';
import { QueryHistoricTimeseriesDataResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { formatNumber } from '@angular/common';

@Component({
    selector: 'chpsoc',
    templateUrl: './chpsoc.component.html'
})
export class ChpSocComponent extends AbstractHistoryChart implements OnInit, OnChanges {
    @Input() private period: DefaultTypes.HistoryPeriod;

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

    public loading: boolean = true;

    protected labels: Date[] = [];
    protected datasets: Dataset[] = EMPTY_DATASET;
    protected options: ChartOptions;
    protected colors = [{
        backgroundColor: 'rgba(255,0,0,0.1)',
        borderColor: 'rgba(255,0,0,1)',
    }];

    private updateChart() {
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
            let showChannelId = Object.keys(result.data).length > 1 ? true : false;

            // convert datasets
            let datasets = [];

            for (let channel in result.data) {

                let address = ChannelAddress.fromString(channel);
                let data = result.data[channel].map(value => {
                    console.log("value : ", value)
                    if (value == null) {
                        return null
                    } else {
                        return value; // convert to % [0,100]
                    }
                });
                datasets.push({
                    label: "Ausgang" + (showChannelId ? ' (' + address.channelId + ')' : ''),
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
    }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route);
        let options = <ChartOptions>Utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS);
        options.scales.yAxes[0].scaleLabel.labelString = "On/Off";
        options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
            let label = data.datasets[tooltipItem.datasetIndex].label;
            let value = tooltipItem.yLabel;
            console.log("label : ", label, "value : ", formatNumber(value, 'de', '1.0-2'))

            return label + ": " + value//formatNumber(value, 'de', '1.0-2');

        }

        this.options = options;
    }

    protected getChannelAddresses(edge: Edge): Promise<ChannelAddress[]> {
        return new Promise((resolve, reject) => {
            this.service.getConfig().then(config => {
                let channeladdresses = [];
                // find all chpsoc components

                for (let componentId of config.getComponentsByFactory("Controller.CHP.SoC")) {
                    channeladdresses.push(ChannelAddress.fromString(componentId.properties.outputChannelAddress));
                    //channeladdresses.push(ChannelAddress.fromString(componentId.properties.inputChannelAddress));

                }
                resolve(channeladdresses);
            }).catch(reason => reject(reason));
        });
    }

    /* protected getChannelAddresses(edge: Edge): Promise<ChannelAddress[]> {
         return new Promise((resolve, reject) => {
           this.service.getConfig().then(config => {
             let channeladdresses = [];
             // find all ChannelThresholdControllers
             for (let controllerId of
               config.getComponentIdsImplementingNature("io.openems.impl.controller.channelthreshold.ChannelThresholdController")
                 .concat(config.getComponentIdsByFactory("Controller.ChannelThreshold"))) {
               const outputChannel = ChannelAddress.fromString(config.getComponentProperties(controllerId)['outputChannelAddress']);
               console.log("in channel threshold", outputChannel)
               channeladdresses.push(outputChannel);
             }
             resolve(channeladdresses);
           }).catch(reason => reject(reason));
         });
       }*/

    private initializeChart() {
        this.datasets = EMPTY_DATASET;
        this.labels = [];
        this.loading = false;
    }
}