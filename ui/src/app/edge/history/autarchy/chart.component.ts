import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { CurrentData } from 'src/app/shared/edge/currentdata';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChannelAddress, Edge, EdgeConfig, Service, Utils } from '../../../shared/shared';
import { ChartOptions, Data, Dataset, DEFAULT_TIME_CHART_OPTIONS, EMPTY_DATASET, TooltipItem } from './../shared';
import { AbstractHistoryChart } from '../abstracthistorychart';

@Component({
    selector: 'autarchychart',
    templateUrl: '../abstracthistorychart.html'
})
export class AutarchyChartComponent extends AbstractHistoryChart implements OnInit, OnChanges {

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


    ngOnInit() {
        this.service.setCurrentComponent('', this.route);
        this.setLabel();
    }

    protected updateChart() {
        this.loading = true;
        this.queryHistoricTimeseriesData(this.period.from, this.period.to).then(response => {
            this.service.getCurrentEdge().then(edge => {
                this.service.getConfig().then(config => {
                    let result = response.result;
                    // convert labels
                    let labels: Date[] = [];
                    for (let timestamp of result.timestamps) {
                        labels.push(new Date(timestamp));
                    }
                    this.labels = labels;

                    // convert datasets
                    let datasets = [];

                    // required data for autarchy and self consumption
                    let buyFromGridData: number[] = [];
                    let consumptionData: number[] = [];

                    if (!edge.isVersionAtLeast('2018.8')) {
                        this.convertDeprecatedData(config, result.data); // TODO deprecated
                    }

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
                        label: this.translate.instant('General.Autarchy'),
                        data: autarchy,
                        hidden: false
                    })
                    this.colors.push({
                        backgroundColor: 'rgba(0,152,204,0.05)',
                        borderColor: 'rgba(0,152,204,1)'
                    })
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
        return new Promise((resolve, reject) => {
            let result: ChannelAddress[] = [
                new ChannelAddress('_sum', 'GridActivePower'),
                new ChannelAddress('_sum', 'ConsumptionActivePower')
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
            if (label == this.grid) {
                if (value < 0) {
                    value *= -1;
                    label = this.gridBuy;
                } else {
                    label = this.gridSell;
                }
            }
            return label + ": " + formatNumber(value, 'de', '1.0-0') + " %"; // TODO get locale dynamically
        }
        options.scales.yAxes[0].ticks.max = 100;
        this.options = options;
    }

    private getSoc(ids: string[], ignoreIds: string[]): ChannelAddress[] {
        let result: ChannelAddress[] = [];
        for (let id of ids) {
            if (ignoreIds.includes(id)) {
                continue;
            }
            result.push.apply(result, [
                new ChannelAddress(id, 'Soc'),
            ]);
        }
        return result;
    }

    protected initializeChart() {
        this.datasets = EMPTY_DATASET;
        this.labels = [];
        this.loading = false;
    }

    /**
   * Calculates '_sum' values.
   * 
   * @param data 
   */
    private convertDeprecatedData(config: EdgeConfig, data: { [channelAddress: string]: any[] }) {
        let sumEssSoc = [];

        for (let channel of Object.keys(data)) {
            let channelAddress = ChannelAddress.fromString(channel)
            let componentId = channelAddress.componentId;
            let channelId = channelAddress.channelId;
            let natureIds = config.getNatureIdsByComponentId(componentId);

            if (natureIds.includes('EssNature') && channelId == 'Soc') {
                if (sumEssSoc.length == 0) {
                    sumEssSoc = data[channel];
                } else {
                    sumEssSoc = data[channel].map((value, index) => {
                        return Utils.addSafely(sumEssSoc[index], value);
                    });
                }
            }
        }
        data['_sum/EssSoc'] = sumEssSoc.map((value, index) => {
            return Utils.divideSafely(sumEssSoc[index], Object.keys(data).length);
        });

    }
}