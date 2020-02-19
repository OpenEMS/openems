import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { CurrentData } from 'src/app/shared/edge/currentdata';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChannelAddress, Edge, EdgeConfig, Service, Utils } from '../../../../shared/shared';
import { AbstractHistoryChart } from '../../abstracthistorychart';
import { ChartOptions, Data, DEFAULT_TIME_CHART_OPTIONS, TooltipItem } from './../../shared';

@Component({
    selector: 'symmetricpeakshavingchart',
    templateUrl: '../../abstracthistorychart.html'
})
export class SymmetricPeakshavingChartComponent extends AbstractHistoryChart implements OnInit, OnChanges {

    @Input() private period: DefaultTypes.HistoryPeriod;
    @Input() public controllerId: string;

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
        this.service.setCurrentComponent('', this.route);
        this.setLabel();
    }

    protected updateChart() {
        this.loading = true;
        this.queryHistoricTimeseriesData(this.period.from, this.period.to).then(response => {
            this.service.getCurrentEdge().then(() => {
                this.service.getConfig().then(config => {
                    let result = response.result;
                    console.log("result", result)
                    this.colors = [];
                    // convert labels
                    let labels: Date[] = [];
                    for (let timestamp of result.timestamps) {
                        labels.push(new Date(timestamp));
                    }
                    this.labels = labels;

                    // convert datasets
                    let datasets = [];

                    if (config.getComponent(this.controllerId).properties['meter.id'] + '/ActivePower' in result.data) {
                        let gridData = result.data[config.getComponent(this.controllerId).properties['meter.id'] + '/ActivePower'].map(value => {
                            if (value == null) {
                                return null
                            } else if (value == 0) {
                                return 0;
                            } else {
                                return value / 1000;
                            }
                        });
                        datasets.push({
                            label: this.translate.instant('General.Grid'),
                            data: gridData,
                            hidden: false
                        });
                        this.colors.push({
                            backgroundColor: 'rgba(0,0,0,0.05)',
                            borderColor: 'rgba(0,0,0,1)'
                        })
                    }
                    this.datasets = datasets;
                    this.loading = false;

                }).catch(reason => {
                    console.error(reason); // TODO error message
                    this.initializeChart();
                    return;
                });
            }).catch(reason => {
                console.error(reason); // TODO error message
                this.initializeChart();
                return;
            });
        }).catch(reason => {
            console.error(reason); // TODO error message
            this.initializeChart();
            return;
        });
    }

    protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {
            let result: ChannelAddress[] = [
                new ChannelAddress(this.controllerId, '_PropertyRechargePower'),
                new ChannelAddress(this.controllerId, '_PropertyPeakShavingPower'),
                new ChannelAddress(config.getComponent(this.controllerId).properties['meter.id'], 'ActivePower')
            ];
            resolve(result);
        })
    }

    protected setLabel() {
        let options = <ChartOptions>Utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS);
        options.scales.yAxes[0].scaleLabel.labelString = this.translate.instant('General.Percentage');
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