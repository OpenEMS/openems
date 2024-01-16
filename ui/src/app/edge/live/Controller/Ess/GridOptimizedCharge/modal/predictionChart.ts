import { formatNumber } from '@angular/common';
import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Data } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { AbstractHistoryChart } from 'src/app/edge/history/abstracthistorychart';
import { ChartOptions, ChronoUnit, DEFAULT_TIME_CHART_OPTIONS, TooltipItem } from 'src/app/edge/history/shared';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChannelAddress, Edge, EdgeConfig, Service, Utils } from 'src/app/shared/shared';

@Component({
    selector: 'predictionChart',
    templateUrl: '../../../../../history/abstracthistorychart.html',
})
export class PredictionChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

    @Input() protected refresh: boolean;
    @Input() protected override edge: Edge;
    @Input() public component: EdgeConfig.Component;
    @Input() public targetEpochSeconds: number;
    @Input() public chargeStartEpochSeconds: number;

    private static DEFAULT_PERIOD: DefaultTypes.HistoryPeriod = new DefaultTypes.HistoryPeriod(new Date(), new Date());

    ngOnChanges() {
        this.updateChart();
    };

    constructor(
        protected override service: Service,
        protected override translate: TranslateService,
        private route: ActivatedRoute,
    ) {
        super("prediction-chart", service, translate);
    }

    ngOnInit() {
        this.service.startSpinner(this.spinnerId);
        this.service.setCurrentComponent('', this.route);
    }

    ngOnDestroy() {
        this.unsubscribeChartRefresh();
    }

    protected updateChart() {

        this.autoSubscribeChartRefresh();
        this.service.startSpinner(this.spinnerId);
        this.loading = true;
        this.colors = [];

        this.queryHistoricTimeseriesData(PredictionChartComponent.DEFAULT_PERIOD.from, PredictionChartComponent.DEFAULT_PERIOD.to, { unit: ChronoUnit.Type.MINUTES, value: 5 }).then(response => {
            let result = response.result;
            let datasets = [];

            // Get the 5 min index of the current time 
            let hours = new Date().getHours();
            let minutes = new Date().getMinutes();
            let currIndex = Math.trunc((hours * 60 + minutes) / 5);

            // Add one buffer hour at the beginning to see at least one hour of the past soc
            let startIndex = currIndex - 12;
            startIndex = startIndex < 0 ? 0 : startIndex;

            // Calculate soc and predicted soc data
            if ('_sum/EssSoc' in result.data) {

                let socData = result.data['_sum/EssSoc'].map(value => {
                    if (value == null) {
                        return null;
                    } else if (value > 100 || value < 0) {
                        return null;
                    } else {
                        return value;
                    }
                });

                // Calculate start soc
                let startSoc = null;
                for (let i = currIndex; i >= 0; i--) {
                    if (socData[i] != null) {
                        startSoc = socData[i];
                        break;
                    }
                }

                let targetTime = new Date(0);
                targetTime.setUTCSeconds(this.targetEpochSeconds);

                // Predicted charge start only used, if a value is present. There's no Channel for it in older Openems Versions.
                let isChargeStartPresent = this.chargeStartEpochSeconds != null;
                let chargeStartTime = new Date(0);
                let chargeStartIndex = 0;
                if (isChargeStartPresent) {
                    chargeStartTime.setUTCSeconds(this.chargeStartEpochSeconds);
                    let chargeStartHours = chargeStartTime.getHours();
                    let chargeStartMinutes = chargeStartTime.getMinutes();

                    // Calculate the index of the chargeStart
                    chargeStartIndex = Math.trunc((chargeStartHours * 60 + chargeStartMinutes) / 5);
                }

                let dataSteps = 0;
                let targetIndex = 0;
                let predictedSocData = Array(288).fill(null);

                // Calculate the predicted soc data
                if (startSoc != null && targetTime != null) {

                    let targetHours = targetTime.getHours();
                    let targetMinutes = targetTime.getMinutes();

                    // Calculate the index of the target minute
                    targetIndex = Math.trunc((targetHours * 60 + targetMinutes) / 5);

                    // Remaining capacity in %
                    let remainingCapacity = 100 - startSoc;

                    // Calculate how much time is left in 5 min steps
                    let remainingSteps = 0;
                    if (isChargeStartPresent) {
                        remainingSteps = targetIndex - chargeStartIndex;
                    } else {
                        remainingSteps = targetIndex - currIndex;
                    }
                    if (remainingSteps > 0) {

                        // Calculate how much percentage is needed in every time step (5 min)
                        dataSteps = remainingCapacity / remainingSteps;

                        // Set the data for the datasets 
                        let predictedSoc = startSoc - dataSteps;
                        for (let i = currIndex; i <= targetIndex; i++) {
                            // Predicted SoC increases only after charge start time, when channel is not zero (e.g. for older versions).
                            if (isChargeStartPresent && i < chargeStartIndex) {
                                predictedSocData[i] = +(predictedSoc + dataSteps).toFixed(2);
                                continue;
                            }
                            predictedSoc = predictedSoc + dataSteps;
                            predictedSocData[i] = +predictedSoc.toFixed(2);
                        }
                    }
                }

                // Add one buffer hour at the end to get more clarity in the chart 
                let chartEndIndex = targetIndex + 12;

                // Remove unimportant values that are after the end index
                if (chartEndIndex < result.data['_sum/EssSoc'].length - 1) {
                    socData.splice(chartEndIndex + 1, socData.length);
                    predictedSocData.splice(chartEndIndex + 1, predictedSocData.length);
                    result.timestamps.splice(chartEndIndex + 1, result.timestamps.length);
                }

                // Remove unimportant values that are before the start index
                if (startIndex > 0) {
                    socData.splice(0, startIndex);
                    predictedSocData.splice(0, startIndex);
                    result.timestamps.splice(0, startIndex);
                }

                // Convert labels
                let labels: Date[] = [];
                for (let timestamp of result.timestamps) {
                    labels.push(new Date(timestamp));
                }
                this.labels = labels;


                // Push the prepared data into the datasets
                datasets.push({
                    label: this.translate.instant('General.soc'),
                    data: socData,
                    hidden: false,
                    yAxisID: 'yAxis2',
                    position: 'right',
                }, {
                    label: this.translate.instant('Edge.Index.Widgets.GridOptimizedCharge.expectedSoc'),
                    data: predictedSocData,
                    hidden: false,
                    yAxisID: 'yAxis2',
                    position: 'right',
                });

                // Push the depending colors 
                this.colors.push({
                    backgroundColor: 'rgba(189, 195, 199,0.05)',
                    borderColor: 'rgba(189, 195, 199,1)',
                }, {
                    backgroundColor: 'rgba(0,223,0,0)',
                    borderColor: 'rgba(0,223,0,1)',
                });
            }

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
                new ChannelAddress('_sum', 'EssSoc'),
            ];
            if (this.component != null && this.component.id) {
                result.push(new ChannelAddress(this.component.id, 'DelayChargeMaximumChargeLimit'));
            }
            resolve(result);
        });
    }

    public getChartHeight(): number {
        return window.innerHeight / 4;
    }

    protected setLabel() {
        let translate = this.translate;
        let options = <ChartOptions>Utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS);

        // Remove left y axis for now
        options.scales.yAxes.shift();

        // adds second y-axis to chart
        options.scales.yAxes
            .push({
                id: 'yAxis2',
                position: 'right',
                scaleLabel: {
                    display: true,
                    labelString: "%",
                    padding: -2,
                    fontSize: 11,
                },
                gridLines: {
                    display: true,
                },
                ticks: {
                    beginAtZero: true,
                    max: 100,
                    padding: -5,
                    stepSize: 20,
                },
            });

        options.layout = {
            padding: {
                left: 2,
                right: 2,
                top: 0,
                bottom: 0,
            },
        };
        //x-axis
        options.scales.xAxes[0].time.unit = "hour";

        //y-axis
        options.tooltips.callbacks.label = function (tooltipItem: TooltipItem, data: Data) {
            let label = data.datasets[tooltipItem.datasetIndex].label;
            let value = tooltipItem.yLabel;
            if (label == translate.instant('General.soc') || label == translate.instant('Edge.Index.Widgets.GridOptimizedCharge.expectedSoc')) {
                return label + ": " + formatNumber(value, 'de', '1.0-0') + " %";
            } else {
                return label + ": " + formatNumber(value, 'de', '1.0-2') + " kW";
            }
        };
        this.options = options;
    }

}

export type ChannelChartDescription = {
    label: string,
    channelName: string,
    datasets: number[],
    colorRgb: string,
}
