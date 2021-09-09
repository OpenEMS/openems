import { AbstractHistoryChart } from '../abstracthistorychart';
import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Service } from '../../../shared/shared';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { Data, TooltipItem } from '../shared';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { formatNumber } from '@angular/common';
import { QueryHistoricTimeseriesDataResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'consumptionEvcsChart',
    templateUrl: '../abstracthistorychart.html'
})
export class ConsumptionEvcsChartComponent extends AbstractHistoryChart implements OnInit, OnChanges {

    @Input() public period: DefaultTypes.HistoryPeriod;
    @Input() public componentId: string;

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


    ngOnInit() {
        this.spinnerId = "consumption-evcs-chart";
        this.service.startSpinner(this.spinnerId);
        this.service.setCurrentComponent('', this.route);
    }

    ngOnDestroy() {
        this.unsubscribeChartRefresh()
    }

    protected updateChart() {
        this.autoSubscribeChartRefresh();
        this.service.startSpinner(this.spinnerId);
        this.loading = true;
        this.queryHistoricTimeseriesData(this.period.from, this.period.to).then(response => {
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

            Object.keys(result.data).forEach((channel) => {
                let address = ChannelAddress.fromString(channel);
                let chargeData = result.data[channel].map(value => {
                    if (value == null) {
                        return null
                    } else {
                        return value / 1000; // convert to kW
                    }
                });
                if (address.channelId == "ChargePower") {
                    datasets.push({
                        label: this.translate.instant('General.consumption'),
                        data: chargeData,
                        hidden: false
                    });
                    this.colors.push({
                        backgroundColor: 'rgba(253,197,7,0.05)',
                        borderColor: 'rgba(253,197,7,1)',
                    })
                }
            })
            this.datasets = datasets;
            this.loading = false;
            this.service.stopSpinner(this.spinnerId);
        }).catch(reason => {
            console.error(reason); // TODO error message
            this.initializeChart();
            return;
        });
    }

    protected getChannelAddresses(): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {
            let result: ChannelAddress[] = [
                new ChannelAddress(this.componentId, 'ChargePower'),
            ];
            resolve(result);
        })
    }

    protected setLabel() {
        let options = this.createDefaultChartOptions();
        options.scales.yAxes[0].scaleLabel.labelString = "kW";
        options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
            let label = data.datasets[tooltipItem.datasetIndex].label;
            let value = tooltipItem.yLabel;
            return label + ": " + formatNumber(value, 'de', '1.0-2') + " kW";
        }
        this.options = options;
    }

    public getChartHeight(): number {
        return window.innerHeight / 21 * 9;
    }
}