
import { ActivatedRoute, Data } from '@angular/router';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { formatNumber } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';
import { ChannelAddress, Service, Utils } from 'src/app/shared/shared';
import { ChartOptions, DEFAULT_TIME_CHART_OPTIONS, TooltipItem } from 'src/app/edge/history/shared';
import { AbstractHistoryChart } from 'src/app/edge/history/abstracthistorychart';
import { ChartLegendLabelItem } from 'chart.js';

@Component({
    selector: 'soltarocellchart',
    templateUrl: '../../../history/abstracthistorychart.html'
})
export class SoltaroCellChartComponent extends AbstractHistoryChart implements OnInit, OnChanges {

    @Input() battery: string;
    @Input() refresh: boolean;

    private static DEFAULT_PERIOD: DefaultTypes.HistoryPeriod = new DefaultTypes.HistoryPeriod((new Date(Date.now() - 1800000)), new Date());

    private importantCellChannels: Channel[] = [
        {
            label: "Minimale Zellspannung", channelName: "Cluster1MinCellVoltage", datasets: [], colorRgb: '45, 171, 91'
        },
        {
            label: "Maximale Zellspannung", channelName: "Cluster1MaxCellVoltage", datasets: [], colorRgb: '45, 123, 171'
        },
        {
            label: "Untere Zuschaltspannungen (Recover)", channelName: "StopParameterCellUnderVoltageRecover", datasets: [], colorRgb: '217, 149, 4'
        },
        {
            label: "Untere Zuschaltspannungen (Protection)", channelName: "StopParameterCellUnderVoltageProtection", datasets: [], colorRgb: '173, 24, 24'
        },
        {
            label: "Obere Abschaltspannungen (Recover)", channelName: "StopParameterCellOverVoltageRecover", datasets: [], colorRgb: '217, 149, 4'
        },
        {
            label: "Obere Abschaltspannungen (Protection)", channelName: "StopParameterCellOverVoltageProtection", datasets: [], colorRgb: '173, 24, 24'
        }
    ];

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
        this.spinnerId = "soltarocell-chart";
        this.service.startSpinner(this.spinnerId);
        this.service.setCurrentComponent('', this.route);
    }

    ngOnDestroy() {
        this.unsubscribeChartRefresh()
    }

    protected updateChart() {
        this.refresh = false;
        this.autoSubscribeChartRefresh();
        this.service.startSpinner(this.spinnerId);
        this.loading = true;
        this.colors = [];
        console.log("From", SoltaroCellChartComponent.DEFAULT_PERIOD.from);
        console.log("From", SoltaroCellChartComponent.DEFAULT_PERIOD.to);
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

            // required data for soltaro cells
            let buyFromGridData: number[] = [];
            let consumptionData: number[] = [];



            this.importantCellChannels.forEach(channel => {
                if (this.battery + '/' + channel.channelName in result.data) {

                    channel.datasets = result.data[this.battery + '/' + channel.channelName].map(value => {
                        if (value == null) {
                            return null
                        } else {
                            return value;
                        }
                    });
                    let currentValue = channel.datasets[channel.datasets.length - 1];
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

            /*
            if ('_sum/ConsumptionActivePower' in result.data) {
                
                consumptionData = result.data['_sum/ConsumptionActivePower'].map(value => {
                    if (value == null) {
                        return null
                    } else {
                        return value;
                    }
                });
            }

            if ('_sum/GridActivePower' in result.data) {
                
                buyFromGridData = result.data['_sum/GridActivePower'].map(value => {
                    if (value == null) {
                        return null
                    } else if (value > 0) {
                        return value;
                    } else {
                        return 0;
                    }
                })
            };
        */


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
                new ChannelAddress(this.battery, 'Cluster1MinCellVoltage'),
                new ChannelAddress(this.battery, 'Cluster1MaxCellVoltage'),
                new ChannelAddress(this.battery, 'StopParameterCellUnderVoltageRecover'),
                new ChannelAddress(this.battery, 'StopParameterCellUnderVoltageProtection'),
                new ChannelAddress(this.battery, 'StopParameterCellOverVoltageRecover'),
                new ChannelAddress(this.battery, 'StopParameterCellOverVoltageProtection'),
            ];
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
        //options.scales.yAxes[0].ticks.max = 4000;
        options.scales.yAxes[0].ticks.beginAtZero = false;
        this.options = options;
    }

    public getChartHeight(): number {
        return window.innerHeight / 3;
    }

}
export type Channel = {
    label: string,
    channelName: string,
    datasets: number[],
    colorRgb: string,
}