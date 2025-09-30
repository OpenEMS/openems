// @ts-strict-ignore
import { Component, Input, OnChanges, OnDestroy, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import * as Chart from "CHART.JS";
import { AbstractHistoryChart } from "src/app/edge/history/abstracthistorychart";
import { ChronoUnit, DEFAULT_TIME_CHART_OPTIONS } from "src/app/edge/history/shared";
import { ChannelAddress, Edge, EdgeConfig, Service, Utils } from "src/app/shared/shared";
import { DefaultTypes } from "src/app/shared/type/defaulttypes";
import { ChartAxis, YAxisType } from "src/app/shared/utils/utils";

@Component({
    selector: "predictionChart",
    templateUrl: "../../../../../history/ABSTRACTHISTORYCHART.HTML",
    standalone: false,
})
export class PredictionChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

    private static DEFAULT_PERIOD: DEFAULT_TYPES.HISTORY_PERIOD = new DEFAULT_TYPES.HISTORY_PERIOD(new Date(), new Date());

    @Input({ required: true }) public component!: EDGE_CONFIG.COMPONENT;
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
        THIS.UPDATE_CHART();
    }

    ngOnInit() {
        THIS.SERVICE.START_SPINNER(THIS.SPINNER_ID);
    }

    ngOnDestroy() {
        THIS.UNSUBSCRIBE_CHART_REFRESH();
    }

    public getChartHeight(): number {
        return WINDOW.INNER_HEIGHT / 4;
    }

    protected updateChart() {

        THIS.AUTO_SUBSCRIBE_CHART_REFRESH();
        THIS.SERVICE.START_SPINNER(THIS.SPINNER_ID);
        THIS.LOADING = true;
        THIS.COLORS = [];

        THIS.QUERY_HISTORIC_TIMESERIES_DATA(PredictionChartComponent.DEFAULT_PERIOD.from, PredictionChartComponent.DEFAULT_PERIOD.to, { unit: CHRONO_UNIT.TYPE.MINUTES, value: 5 }).then(async response => {
            const result = RESPONSE.RESULT;
            const datasets = [];

            // Get the 5 min index of the current time
            const hours = new Date().getHours();
            const minutes = new Date().getMinutes();
            const currIndex = MATH.TRUNC((hours * 60 + minutes) / 5);

            // Add one buffer hour at the beginning to see at least one hour of the past soc
            let startIndex = currIndex - 12;
            startIndex = startIndex < 0 ? 0 : startIndex;

            // Calculate soc and predicted soc data
            if ("_sum/EssSoc" in RESULT.DATA) {

                const socData = RESULT.DATA["_sum/EssSoc"].map(value => {
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
                TARGET_TIME.SET_UTCSECONDS(THIS.TARGET_EPOCH_SECONDS);

                // Predicted charge start only used, if a value is present. There's no Channel for it in older Openems Versions.
                const isChargeStartPresent = THIS.CHARGE_START_EPOCH_SECONDS != null;
                const chargeStartTime = new Date(0);
                let chargeStartIndex = 0;
                if (isChargeStartPresent) {
                    CHARGE_START_TIME.SET_UTCSECONDS(THIS.CHARGE_START_EPOCH_SECONDS);
                    const chargeStartHours = CHARGE_START_TIME.GET_HOURS();
                    const chargeStartMinutes = CHARGE_START_TIME.GET_MINUTES();

                    // Calculate the index of the chargeStart
                    chargeStartIndex = MATH.TRUNC((chargeStartHours * 60 + chargeStartMinutes) / 5);
                }

                let dataSteps = 0;
                let targetIndex = 0;
                const predictedSocData = Array(288).fill(null);

                // Calculate the predicted soc data
                if (startSoc != null && targetTime != null) {

                    const targetHours = TARGET_TIME.GET_HOURS();
                    const targetMinutes = TARGET_TIME.GET_MINUTES();

                    // Calculate the index of the target minute
                    targetIndex = MATH.TRUNC((targetHours * 60 + targetMinutes) / 5);

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
                            // Predicted SoC increases only after charge start time, when channel is not zero (E.G. for older versions).
                            if (isChargeStartPresent && i < chargeStartIndex) {
                                predictedSocData[i] = +(predictedSoc + dataSteps).toFixed(2);
                                continue;
                            }
                            predictedSoc = predictedSoc + dataSteps;
                            predictedSocData[i] = +PREDICTED_SOC.TO_FIXED(2);
                        }
                    }
                }

                // Add one buffer hour at the end to get more clarity in the chart
                const chartEndIndex = targetIndex + 12;

                // Remove unimportant values that are after the end index
                if (chartEndIndex < RESULT.DATA["_sum/EssSoc"].length - 1) {
                    SOC_DATA.SPLICE(chartEndIndex + 1, SOC_DATA.LENGTH);
                    PREDICTED_SOC_DATA.SPLICE(chartEndIndex + 1, PREDICTED_SOC_DATA.LENGTH);
                    RESULT.TIMESTAMPS.SPLICE(chartEndIndex + 1, RESULT.TIMESTAMPS.LENGTH);
                }

                // Remove unimportant values that are before the start index
                if (startIndex > 0) {
                    SOC_DATA.SPLICE(0, startIndex);
                    PREDICTED_SOC_DATA.SPLICE(0, startIndex);
                    RESULT.TIMESTAMPS.SPLICE(0, startIndex);
                }

                // Convert labels
                const labels: Date[] = [];
                for (const timestamp of RESULT.TIMESTAMPS) {
                    LABELS.PUSH(new Date(timestamp));
                }
                THIS.LABELS = labels;


                // Push the prepared data into the datasets
                DATASETS.PUSH({
                    label: THIS.TRANSLATE.INSTANT("GENERAL.SOC"),
                    data: socData,
                    hidden: false,
                    yAxisID: CHART_AXIS.RIGHT,
                }, {
                    label: THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.EXPECTED_SOC"),
                    data: predictedSocData,
                    hidden: false,
                    yAxisID: CHART_AXIS.RIGHT,
                });

                // Push the depending colors
                THIS.COLORS.PUSH({
                    backgroundColor: "rgba(189, 195, 199,0.05)",
                    borderColor: "rgba(189, 195, 199,1)",
                }, {
                    backgroundColor: "rgba(0,223,0,0)",
                    borderColor: "rgba(0,223,0,1)",
                });
            }

            THIS.DATASETS = datasets;
            THIS.LOADING = false;
            THIS.SERVICE.STOP_SPINNER(THIS.SPINNER_ID);

            // Overwrite default options
            THIS.UNIT = YAXIS_TYPE.PERCENTAGE;
            THIS.FORMAT_NUMBER = "1.0-0";
            THIS.CHART_AXIS = CHART_AXIS.RIGHT;
            THIS.POSITION = "right";

            await THIS.SET_OPTIONS(THIS.OPTIONS);
            THIS.APPLY_CONTROLLER_SPECIFIC_OPTIONS();

        }).catch(reason => {
            CONSOLE.ERROR(reason); // TODO error message
            THIS.INITIALIZE_CHART();
            return;
        });
    }

    protected setLabel() {
        THIS.OPTIONS = <CHART.CHART_OPTIONS>UTILS.DEEP_COPY(DEFAULT_TIME_CHART_OPTIONS);
    }

    protected getChannelAddresses(): Promise<ChannelAddress[]> {

        return new Promise((resolve) => {
            const result: ChannelAddress[] = [
                new ChannelAddress("_sum", "EssSoc"),
            ];
            if (THIS.COMPONENT != null && THIS.COMPONENT.ID) {
                RESULT.PUSH(new ChannelAddress(THIS.COMPONENT.ID, "DelayChargeMaximumChargeLimit"));
            }
            resolve(result);
        });
    }

    private applyControllerSpecificOptions() {
        THIS.OPTIONS.SCALES[CHART_AXIS.LEFT] = {
            position: "left",
            display: false,
        };

        /** Overwrite default yAxisId */
        THIS.DATASETS = THIS.DATASETS
            .map(el => {
                el["yAxisID"] = CHART_AXIS.RIGHT;
                return el;
            });

        THIS.OPTIONS.SCALES.X.TICKS.CALLBACK = function (value, index, values) {
            const date = new Date(value);

            // Display the label only if the minutes are zero (full hour)
            return DATE.GET_MINUTES() === 0 ? DATE.GET_HOURS() + ":00" : "";
        };
    }

}

export type ChannelChartDescription = {
    label: string,
    channelName: string,
    datasets: number[],
    colorRgb: string,
};
