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
    @Input() public component: EdgeConfig.Component;

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
                    let meterIdActivePower = config.getComponent(this.component.id).properties['meter.id'] + '/ActivePower';
                    let peakshavingPower = this.component.id + '/_PropertyPeakShavingPower';
                    let rechargePower = this.component.id + '/_PropertyRechargePower';
                    let result = response.result;
                    this.colors = [];
                    // convert labels
                    let labels: Date[] = [];
                    for (let timestamp of result.timestamps) {
                        labels.push(new Date(timestamp));
                    }
                    this.labels = labels;

                    // convert datasets
                    let datasets = [];

                    if (meterIdActivePower in result.data) {
                        let data = result.data[meterIdActivePower].map(value => {
                            if (value == null) {
                                return null
                            } else if (value == 0) {
                                return 0;
                            } else {
                                return value / 1000; // convert to kW
                            }
                        });
                        datasets.push({
                            label: this.translate.instant('General.Grid'),
                            data: data,
                            hidden: false
                        });
                        this.colors.push({
                            backgroundColor: 'rgba(0,0,0,0.05)',
                            borderColor: 'rgba(0,0,0,1)'
                        })
                    }
                    if (peakshavingPower in result.data) {
                        let data = result.data[peakshavingPower].map(value => {
                            if (value == null) {
                                return null
                            } else if (value == 0) {
                                return 0;
                            } else {
                                return value / 1000; // convert to kW
                            }
                        });
                        datasets.push({
                            label: this.translate.instant('Edge.Index.Widgets.Peakshaving.peakshavingPower'),
                            data: data,
                            hidden: false
                        });
                        this.colors.push({
                            backgroundColor: 'rgba(0,0,0,0)',
                            borderColor: 'rgba(200,0,0,1)',
                        })
                    }
                    if (rechargePower in result.data) {
                        let data = result.data[rechargePower].map(value => {
                            if (value == null) {
                                return null
                            } else if (value == 0) {
                                return 0;
                            } else {
                                return value / 1000; // convert to kW
                            }
                        });
                        datasets.push({
                            label: this.translate.instant('Edge.Index.Widgets.Peakshaving.rechargePower'),
                            data: data,
                            hidden: false
                        });
                        this.colors.push({
                            backgroundColor: 'rgba(0,0,0,0)',
                            borderColor: 'rgba(0,223,0,1)',
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
                new ChannelAddress(this.component.id, '_PropertyRechargePower'),
                new ChannelAddress(this.component.id, '_PropertyPeakShavingPower'),
                new ChannelAddress(config.getComponent(this.component.id).properties['meter.id'], 'ActivePower')
            ];
            resolve(result);
        })
    }

    protected setLabel() {
        let options = <ChartOptions>Utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS);
        options.scales.yAxes[0].scaleLabel.labelString = "kW";
        options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
            let label = data.datasets[tooltipItem.datasetIndex].label;
            let value = tooltipItem.yLabel;
            return label + ": " + formatNumber(value, 'de', '1.0-2') + " kW";
        }
        this.options = options;
    }

    public getChartHeight(): number {
        return window.innerHeight / 1.3;
    }
}