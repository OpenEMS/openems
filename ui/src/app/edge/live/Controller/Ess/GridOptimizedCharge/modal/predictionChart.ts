// @ts-strict-ignore
import { Component, Input, OnChanges, OnDestroy, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import * as Chart from "chart.js";
import { AbstractHistoryChart } from "src/app/edge/history/abstracthistorychart";
import { ChronoUnit, DEFAULT_TIME_CHART_OPTIONS } from "src/app/edge/history/shared";
import { DefaultTypes } from "src/app/shared/service/defaulttypes";
import { ChartAxis, YAxisType } from "src/app/shared/service/utils";
import { ChannelAddress, Edge, EdgeConfig, Service, Utils } from "src/app/shared/shared";

@Component({
    selector: "predictionChart",
    templateUrl: "../../../../../history/abstracthistorychart.html",
})
export class PredictionChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

    private static DEFAULT_PERIOD: DefaultTypes.HistoryPeriod = new DefaultTypes.HistoryPeriod(new Date(), new Date());

    @Input({ required: true }) public component!: EdgeConfig.Component;
    @Input({ required: true }) public targetEpochSeconds!: number;
    @Input({ required: true }) public chargeStartEpochSeconds!: number;
    @Input({ required: true }) protected refresh!: boolean;
    @Input({ required: true }) protected override edge!: Edge;

    constructor(
        protected override service: Service,
        protected override translate: TranslateService,
        private route: ActivatedRoute,
    ) {
        super("prediction-chart", service, translate);
    }

    ngOnChanges() {
        this.updateChart();
    }

    ngOnInit() {
        this.service.startSpinner(this.spinnerId);
    }

    ngOnDestroy() {
        this.unsubscribeChartRefresh();
    }

    public getChartHeight(): number {
        return window.innerHeight / 4;
    }

    protected updateChart() {

        this.autoSubscribeChartRefresh();
        this.service.startSpinner(this.spinnerId);
        this.loading = true;
        this.colors = [];

        this.queryHistoricTimeseriesData(PredictionChartComponent.DEFAULT_PERIOD.from, PredictionChartComponent.DEFAULT_PERIOD.to, { unit: ChronoUnit.Type.MINUTES, value: 5 }).then(async response => {
            const result = response.result;
            const datasets = [];

            // Get the 5 min index of the current time
            const hours = new Date().getHours();
            const minutes = new Date().getMinutes();
            const currIndex = Math.trunc((hours * 60 + minutes) / 5);

            // Add one buffer hour at the beginning to see at least one hour of the past soc
            let startIndex = currIndex - 12;
            startIndex = startIndex < 0 ? 0 : startIndex;

            // Calculate soc and predicted soc data
            if ("_sum/EssSoc" in result.data) {

                const socData = result.data["_sum/EssSoc"].map(value => {
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

                const targetTime = new Date(0);
                targetTime.setUTCSeconds(this.targetEpochSeconds);

                // Predicted charge start only used, if a value is present. There's no Channel for it in older Openems Versions.
                const isChargeStartPresent = this.chargeStartEpochSeconds != null;
                const chargeStartTime = new Date(0);
                let chargeStartIndex = 0;
                if (isChargeStartPresent) {
                    chargeStartTime.setUTCSeconds(this.chargeStartEpochSeconds);
                    const chargeStartHours = chargeStartTime.getHours();
                    const chargeStartMinutes = chargeStartTime.getMinutes();

                    // Calculate the index of the chargeStart
                    chargeStartIndex = Math.trunc((chargeStartHours * 60 + chargeStartMinutes) / 5);
                }

                let dataSteps = 0;
                let targetIndex = 0;
                const predictedSocData = Array(288).fill(null);

                // Calculate the predicted soc data
                if (startSoc != null && targetTime != null) {

                    const targetHours = targetTime.getHours();
                    const targetMinutes = targetTime.getMinutes();

                    // Calculate the index of the target minute
                    targetIndex = Math.trunc((targetHours * 60 + targetMinutes) / 5);

                    // Remaining capacity in %
                    const remainingCapacity = 100 - startSoc;

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
                const chartEndIndex = targetIndex + 12;

                // Remove unimportant values that are after the end index
                if (chartEndIndex < result.data["_sum/EssSoc"].length - 1) {
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
                const labels: Date[] = [];
                for (const timestamp of result.timestamps) {
                    labels.push(new Date(timestamp));
                }
                this.labels = labels;


                // Push the prepared data into the datasets
                datasets.push({
                    label: this.translate.instant("General.soc"),
                    data: socData,
                    hidden: false,
                    yAxisID: ChartAxis.RIGHT,
                }, {
                    label: this.translate.instant("Edge.Index.Widgets.GridOptimizedCharge.expectedSoc"),
                    data: predictedSocData,
                    hidden: false,
                    yAxisID: ChartAxis.RIGHT,
                });

                // Push the depending colors
                this.colors.push({
                    backgroundColor: "rgba(189, 195, 199,0.05)",
                    borderColor: "rgba(189, 195, 199,1)",
                }, {
                    backgroundColor: "rgba(0,223,0,0)",
                    borderColor: "rgba(0,223,0,1)",
                });
            }

            this.datasets = datasets;
            this.loading = false;
            this.service.stopSpinner(this.spinnerId);
            this.unit = YAxisType.PERCENTAGE;
            this.formatNumber = "1.0-0";
            await this.setOptions(this.options);
            this.applyControllerSpecificOptions();

        }).catch(reason => {
            console.error(reason); // TODO error message
            this.initializeChart();
            return;
        });
    }

    protected setLabel() {
        this.options = <Chart.ChartOptions>Utils.deepCopy(DEFAULT_TIME_CHART_OPTIONS);
    }

    protected getChannelAddresses(): Promise<ChannelAddress[]> {

        return new Promise((resolve) => {
            const result: ChannelAddress[] = [
                new ChannelAddress("_sum", "EssSoc"),
            ];
            if (this.component != null && this.component.id) {
                result.push(new ChannelAddress(this.component.id, "DelayChargeMaximumChargeLimit"));
            }
            resolve(result);
        });
    }

    private applyControllerSpecificOptions() {
        this.options.scales[ChartAxis.LEFT]["position"] = "right";
        this.options.scales.x.ticks.callback = function (value, index, values) {
            const date = new Date(value);

            // Display the label only if the minutes are zero (full hour)
            return date.getMinutes() === 0 ? date.getHours() + ":00" : "";
        };
    }

}

export type ChannelChartDescription = {
    label: string,
    channelName: string,
    datasets: number[],
    colorRgb: string,
};
