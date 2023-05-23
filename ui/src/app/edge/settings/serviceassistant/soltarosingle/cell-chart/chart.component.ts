
import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Data } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { AbstractHistoryChart } from 'src/app/edge/history/abstracthistorychart';
import { ChartOptions, DEFAULT_TIME_CHART_OPTIONS, TooltipItem } from 'src/app/edge/history/shared';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChannelAddress, Service, Utils } from 'src/app/shared/shared';
import { ChannelChartDescription } from '../../abstractbattery.component';

@Component({
    selector: 'soltarocellchart',
    templateUrl: '../../../../history/abstracthistorychart.html'
})
export class SoltaroCellChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

    @Input() battery: string;
    @Input() channels: ChannelChartDescription[];
    @Input() refresh: boolean;

    private static DEFAULT_PERIOD: DefaultTypes.HistoryPeriod = new DefaultTypes.HistoryPeriod(new Date(), new Date());

    ngOnChanges() {
        this.updateChart();
    };

    constructor(
        protected service: Service,
        protected translate: TranslateService,
        private route: ActivatedRoute,
    ) {
        super("soltarocell-chart", service, translate);
    }

    ngOnInit() {
        this.startSpinner();
        this.service.setCurrentComponent('', this.route);
    }

    ngOnDestroy() {
        this.unsubscribeChartRefresh()
    }

    protected updateChart() {
        this.refresh = false;
        this.autoSubscribeChartRefresh();
        this.startSpinner();
        this.loading = true;
        this.colors = [];
        this.queryHistoricTimeseriesData(SoltaroCellChartComponent.DEFAULT_PERIOD.from, SoltaroCellChartComponent.DEFAULT_PERIOD.to).then(response => {
            let result = response.result;
            // convert labels
            let labels: Date[] = [];
            for (let timestamp of result.timestamps) {
                labels.push(new Date(timestamp));
            }
            this.labels = labels;

            // convert datasets
            let datasets = [];

            this.channels.forEach(channel => {
                if (this.battery + '/' + channel.channelName in result.data) {

                    channel.datasets = result.data[this.battery + '/' + channel.channelName].map(value => {
                        if (value == null) {
                            return null
                        } else {
                            return value;
                        }
                    });
                    datasets.push({
                        label: channel.label,
                        data: channel.datasets,
                        hidden: false
                    })
                    this.colors.push({
                        backgroundColor: 'rgba(' + channel.colorRgb + ',0.05)',
                        borderColor: 'rgba(' + channel.colorRgb + ',1)'
                    })
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

    protected getChannelAddresses(): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {
            let result: ChannelAddress[] = [];
            this.channels.forEach(channel => {
                result.push(new ChannelAddress(this.battery, channel.channelName));
            });
            resolve(result);
        })
    }

    protected setLabel() {
        let options = <ChartOptions>Utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS);
        options.scales.yAxes[0].scaleLabel.labelString = 'Spannungen in mV';
        options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
            let label = data.datasets[tooltipItem.datasetIndex].label;
            let value = tooltipItem.yLabel;
            return label + ": " + formatNumber(value, 'de', '1.0-0') + " mV";
        }
        options.scales.yAxes[0].ticks.beginAtZero = false;
        this.options = options;
    }

    public getChartHeight(): number {
        return window.innerHeight / 3;
    }
}
