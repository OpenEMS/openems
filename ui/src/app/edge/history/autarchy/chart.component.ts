import { AbstractHistoryChart } from '../abstracthistorychart';
import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Service } from '../../../shared/shared';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { CurrentData } from 'src/app/shared/edge/currentdata';
import { Data, TooltipItem } from './../shared';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { formatNumber } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'autarchychart',
    templateUrl: '../abstracthistorychart.html'
})
export class AutarchyChartComponent extends AbstractHistoryChart implements OnInit, OnChanges {

    @Input() public period: DefaultTypes.HistoryPeriod;

    ngOnChanges() {
        this.updateChart();
    };

    constructor(
        public service: Service,
        protected translate: TranslateService,
        private route: ActivatedRoute,
    ) {
        super(service, translate);
    }


    ngOnInit() {
        this.spinnerId = "autarchy-chart";
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
            let labels: Date[] = [];
            for (let timestamp of result.timestamps) {
                labels.push(new Date(timestamp));
            }
            this.labels = labels;

            // convert datasets
            let datasets = [];

            // required data for autarchy
            let buyFromGridData: number[] = [];
            let consumptionData: number[] = [];

            if ('_sum/ConsumptionActivePower' in result.data) {
                /*
                 * Consumption
                 */
                consumptionData = result.data['_sum/ConsumptionActivePower'].map(value => {
                    if (value == null) {
                        return null
                    } else {
                        return value;
                    }
                });
            }

            if ('_sum/GridActivePower' in result.data) {
                /*
                 * Buy From Grid
                 */
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

            /*
            * Autarchy
            */
            let autarchy = consumptionData.map((value, index) => {
                if (value == null) {
                    return null
                } else {
                    return CurrentData.calculateAutarchy(buyFromGridData[index], value);
                }
            })
            datasets.push({
                label: this.translate.instant('General.autarchy'),
                data: autarchy,
                hidden: false
            })
            this.colors.push({
                backgroundColor: 'rgba(0,152,204,0.05)',
                borderColor: 'rgba(0,152,204,1)'
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
        this.options = options;
    }

    public getChartHeight(): number {
        return window.innerHeight / 1.3;
    }
}