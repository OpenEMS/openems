import { AbstractHistoryChart } from '../abstracthistorychart';
import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Service } from '../../../shared/shared';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { CurrentData } from 'src/app/shared/edge/currentdata';
import { Data, TooltipItem } from '../shared';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { formatNumber } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';
import { GeneratedFile } from '@angular/compiler';


@Component({
    selector: 'lukaschart',
    templateUrl: '../abstracthistorychart.html'
})
export class LukasChartComponent extends AbstractHistoryChart implements OnInit, OnChanges {

    @Input() private period: DefaultTypes.HistoryPeriod;

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
        this.spinnerId = "lukas-chart";
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
        this.colors = [];
        this.queryHistoricTimeseriesData(this.period.from, this.period.to).then(response => {
            let result = response.result;
            // convert labels
            /* let labels: Date[] = [];
             for (let timestamp of result.timestamps) {
                 labels.push(new Date(timestamp));
             }
             this.labels = labels;*/

            // convert datasets

        }).catch(reason => {
            let datasets = [];

            let lukas: number[] = [1233, 116, 1326, 4229, 2219, 2218, 1613, 1428, 12, 1442, 6228, 1426, 8421, 6421, 68, 1541, 2646, 6758, 1326, 8451, 6458]
            let labels: Date[] = [];
            //let minutes: number = labels.getUTCHours().value;
            for (let timestamp = 0; timestamp < lukas.length; timestamp++) {

                labels.push(new Date((timestamp)));
            }
            this.labels = labels;


            datasets.push({
                type: 'bar',
                label: this.translate.instant('General.lukas'),
                data: lukas,
                hidden: false
            })


            this.colors.push({
                backgroundColor: 'rgba(0,152,204,0.05)',
                borderColor: 'rgba(0,152,204,1)'
            })
            let socData: number[] = [20, 30, 20, 10, 15, 10]
            datasets.push({
                type: 'line',
                label: this.translate.instant('General.energy'),
                data: socData,
                hidden: false,
                borderDash: [10, 5],
            })


            this.colors.push({
                backgroundColor: 'rgba(0,52,104,0.05)',
                borderColor: 'rgba(0,52,104,1)'
            })
            this.datasets = datasets;
            this.loading = false;
            this.service.stopSpinner(this.spinnerId);
            return;
            this.datasets = datasets;
            this.loading = false;
            this.service.stopSpinner(this.spinnerId);
            return;


        });

    }

    protected getChannelAddresses(): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {
            let result: ChannelAddress[] = [
                new ChannelAddress('_sum', 'GridActivePower'),
                new ChannelAddress('_sum', 'ConsumptionActivePower')
            ];
            resolve(result);
        })
    }

    protected setLabel() {
        let options = this.createDefaultChartOptions();
        options.scales.yAxes[0].scaleLabel.labelString = this.translate.instant('General.percentage');

        options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
            let label = data.datasets[tooltipItem.datasetIndex].label;
            let value = tooltipItem.yLabel;
            return label + ": " + formatNumber(value, 'de', '1.0-0') + " %"; // TODO get locale dynamically
        }
        options.scales.yAxes[0].ticks.max = 100;
        options.scales.yAxes[0].id = "yAxis1";
        options.scales.yAxes[0].scaleLabel.labelString = "kW";

        this.options = options;
        options.scales.yAxes.push({
            id: 'yAxis2',
            position: 'right',
            scaleLabel: {
                display: true,
                labelString: '%',
                padding: -2,
                fontSize: 11
            },
            gridLines: {
                display: false
            },
            ticks: {
                beginAtZero: true,
                max: 100,
                padding: -5,
                stepSize: 5
            }
        })
        options.scales.yAxes.push({
            id: 'yAxis1',
            position: 'left',
            scaleLabel: {
                display: true,
                labelString: 'kw',
                padding: -2,
                fontSize: 11
            },
            gridLines: {
                display: false
            },
            ticks: {
                beginAtZero: true,
                padding: -5,
                stepSize: 100
            }
        })
        options.layout = {
            padding: {
                left: 2,
                right: 2,
                top: 0,
                bottom: 0
            }
        }
    }

    public getChartHeight(): number {
        return window.innerHeight / 1.3;
    }
}