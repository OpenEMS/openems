import { Component, OnInit, OnChanges, Input } from "@angular/core";
import { AbstractHistoryChart } from '../abstracthistorychart';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { Service, Edge, ChannelAddress } from 'src/app/shared/shared';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { Dataset, EMPTY_DATASET, ChartOptions } from '../shared';
import { QueryHistoricTimeseriesDataResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';

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
        backgroundColor: 'rgba(204,204,204,0.1)',
        borderColor: 'rgba(204,204,204,1)',
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
                    if (value == null) {
                        return null
                    } else {
                        return value * 100; // convert to % [0,100]
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

    }

    protected getChannelAddresses(edge: Edge): Promise<ChannelAddress[]> {
        return new Promise((resolve, reject) => {
            this.service.getConfig().then(config => {
                let channeladdresses = [];
                // find all chpsoc components
                for (let componentId of config.getComponentIdsImplementingNature("io.openems.edge.controller.chp.soc")) {
                    channeladdresses.push(new ChannelAddress(componentId, 'outputChannelAddress'));
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