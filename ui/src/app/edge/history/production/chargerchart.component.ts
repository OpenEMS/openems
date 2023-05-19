import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChannelAddress, Edge, EdgeConfig, Service } from '../../../shared/shared';
import { AbstractHistoryChart } from '../abstracthistorychart';
import { Data, TooltipItem } from '../shared';

@Component({
    selector: 'productionChargerChart',
    templateUrl: '../abstracthistorychart.html'
})
export class ProductionChargerChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

    @Input() public period: DefaultTypes.HistoryPeriod;
    @Input() public componentId: string;
    @Input() public isOnlyChart: boolean;

    ngOnChanges() {
        this.updateChart();
    };

    constructor(
        protected service: Service,
        protected translate: TranslateService,
        private route: ActivatedRoute,
    ) {
        super("production-charger-chart", service, translate);
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
        this.colors = [];
        this.queryHistoricTimeseriesData(this.period.from, this.period.to).then(response => {
            let result = response.result;
            // convert labels
            let labels: Date[] = [];
            for (let timestamp of result.timestamps) {
                labels.push(new Date(timestamp));
            }
            this.labels = labels;
            // convert datasets
            let datasets = [];

            Object.keys(result.data).forEach(channel => {
                let address = ChannelAddress.fromString(channel);
                let data = result.data[channel].map(value => {
                    if (value == null) {
                        return null;
                    } else {
                        return value / 1000; // convert to kW
                    }
                });
                if (address.channelId == 'ActualPower') {
                    datasets.push({
                        label: this.translate.instant('General.production'),
                        data: data
                    });
                    this.colors.push({
                        backgroundColor: 'rgba(45,143,171,0.05)',
                        borderColor: 'rgba(45,143,171,1)'
                    });
                }
            });
            this.datasets = datasets;
            this.loading = false;
            this.stopSpinner();

        }).catch(reason => {
            console.error(reason); // TODO error message
            this.initializeChart();
            return;
        });
    }

    protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {
            let result: ChannelAddress[] = [
                new ChannelAddress(this.componentId, 'ActualPower'),
            ];
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
        if (this.isOnlyChart == true) {
            return window.innerHeight / 1.3;
        } else {
            return window.innerHeight / 21 * 9;
        }
    }
}