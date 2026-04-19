import { ChangeDetectorRef, Component, Input, OnChanges, SimpleChanges } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import * as Chart from "chart.js";

import { ChronoUnit, Resolution } from "src/app/edge/history/shared";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/chart.constants";
import { NavigationService } from "src/app/shared/components/navigation/service/navigation.service";
import { ViewUtils } from "src/app/shared/components/navigation/view/shared/shared";
import { QueryHistoricTimeseriesDataResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse";
import { ChannelAddress, Edge, EdgeConfig, Logger, Service } from "src/app/shared/shared";
import { DefaultTypes } from "src/app/shared/type/defaulttypes";
import { ColorUtils } from "src/app/shared/utils/color/color.utils";
import { ChartAxis, HistoryUtils, YAxisType } from "src/app/shared/utils/utils";

@Component({
    selector: "oe-controller-ess-gridoptimizedcharge-prediction-chart",
    templateUrl: "../../../../../history/abstracthistorychart.html",
    standalone: false,
})
export class NewNavigationPredictionChartComponent extends AbstractHistoryChart implements OnChanges {

    private static readonly DEFAULT_PERIOD: DefaultTypes.HistoryPeriod =
        new DefaultTypes.HistoryPeriod(new Date(), new Date());

    private static readonly DEFAULT_RESOLUTION: Resolution = {
        unit: ChronoUnit.Type.MINUTES,
        value: 5,
    };

    @Input({ required: true }) public refresh!: boolean;
    @Input({ required: true }) public override edge!: Edge;
    @Input({ required: true }) public override component!: EdgeConfig.Component;
    @Input({ required: true }) public targetEpochSeconds!: number;
    @Input({ required: true }) public chargeStartEpochSeconds!: number;

    constructor(
        public override service: Service,
        public override cdRef: ChangeDetectorRef,
        protected override translate: TranslateService,
        protected override route: ActivatedRoute,
        protected override logger: Logger,
        protected override navigationService: NavigationService,
    ) {
        super(service, cdRef, translate, route, logger, navigationService);
    }

    public ngOnChanges(changes: SimpleChanges): void {
        if (!this.config) {
            return;
        }

        if (
            changes["refresh"] ||
            changes["component"] ||
            changes["edge"] ||
            changes["targetEpochSeconds"] ||
            changes["chargeStartEpochSeconds"]
        ) {
            this.updateChart();
        }
    }

    protected override getChartHeight(): number | null {
        const fourTimesTheHeight = 400;
        return ViewUtils.getChartContentHeightInVh(window.innerHeight, this.navigationService.position(), fourTimesTheHeight);
    }

    protected override getChartData(): HistoryUtils.ChartData {
        return {
            input: [
                {
                    name: "_sum/EssSoc",
                    powerChannel: new ChannelAddress("_sum", "EssSoc"),
                },
            ],
            output: () => [],
            tooltip: {
                formatNumber: ChartConstants.NumberFormat.ZERO_TO_TWO,
            },
            yAxes: [
                {
                    unit: YAxisType.PERCENTAGE,
                    position: "right",
                    yAxisId: ChartAxis.RIGHT,
                },
                {
                    unit: YAxisType.NONE,
                    position: "left",
                    yAxisId: ChartAxis.LEFT,
                    displayGrid: false,
                },
            ],
        };
    }

    protected override async loadChart(): Promise<void> {
        if (this.edge == null || this.component == null || this.config == null) {
            return;
        }

        this.labels = [];
        this.errorResponse = null;
        this.loading = true;
        this.chartType = "line";
        this.chartObject = this.getChartData();

        try {
            const response = await this.queryHistoricTimeseriesData(
                NewNavigationPredictionChartComponent.DEFAULT_PERIOD.from,
                NewNavigationPredictionChartComponent.DEFAULT_PERIOD.to,
                NewNavigationPredictionChartComponent.DEFAULT_RESOLUTION,
            );

            const prepared = this.preparePredictionData(response);

            if (prepared == null) {
                this.initializeChart();
                return;
            }

            this.labels = prepared.labels;
            this.datasets = this.prepareDatasets(prepared.datasets);
            this.legendOptions = this.datasets
                .filter((dataset, index, arr) => arr.findIndex(d => d.label === dataset.label) === index)
                .map(dataset => ({
                    label: dataset.label?.toString() ?? "",
                    strokeThroughHidingStyle: false,
                    hideLabelInLegend: false,
                }));

            this.options = this.createPredictionChartOptions();

            this.loading = false;
            this.stopSpinner();

            this.setChartConfig.emit({
                chartType: this.chartType,
                datasets: this.datasets,
                labels: this.labels,
                options: this.options,
            });
        } catch (error) {
            console.error(error);
            this.initializeChart();
        } finally {
            this.cdRef.markForCheck();
        }
    }

    private preparePredictionData(
        response: QueryHistoricTimeseriesDataResponse,
    ): { labels: Date[]; datasets: Chart.ChartDataset[] } | null {
        const result = response?.result;
        const rawSocData = result?.data?.["_sum/EssSoc"];

        if (!Array.isArray(rawSocData) || !Array.isArray(result?.timestamps)) {
            return null;
        }

        const currentIndex = this.getCurrentFiveMinuteIndex();
        const startIndex = Math.max(currentIndex - 12, 0);

        const socData: (number | null)[] = rawSocData.map(value => {
            if (value == null || value > 100 || value < 0) {
                return null;
            }
            return value;
        });

        const startSoc = this.findStartSoc(socData, currentIndex);

        const targetTime = new Date(0);
        targetTime.setUTCSeconds(this.targetEpochSeconds);

        const isChargeStartPresent = this.chargeStartEpochSeconds != null;
        let chargeStartIndex = 0;

        if (isChargeStartPresent) {
            const chargeStartTime = new Date(0);
            chargeStartTime.setUTCSeconds(this.chargeStartEpochSeconds);

            const chargeStartHours = chargeStartTime.getHours();
            const chargeStartMinutes = chargeStartTime.getMinutes();
            chargeStartIndex = Math.trunc((chargeStartHours * 60 + chargeStartMinutes) / 5);
        }

        const predictedSocData: (number | null)[] = Array(288).fill(null);

        const targetHours = targetTime.getHours();
        const targetMinutes = targetTime.getMinutes();
        let targetIndex = Math.trunc((targetHours * 60 + targetMinutes) / 5);

        if (targetIndex < currentIndex) {
            targetIndex += 288;
        }

        if (startSoc != null) {
            const remainingCapacity = 100 - startSoc;
            const remainingSteps = isChargeStartPresent
                ? targetIndex - chargeStartIndex
                : targetIndex - currentIndex;

            if (remainingSteps > 0) {
                const dataSteps = remainingCapacity / remainingSteps;
                let predictedSoc = startSoc - dataSteps;

                for (let i = currentIndex; i <= targetIndex; i++) {
                    if (isChargeStartPresent && i < chargeStartIndex) {
                        predictedSocData[i] = +(predictedSoc + dataSteps).toFixed(2);
                        continue;
                    }

                    predictedSoc = predictedSoc + dataSteps;
                    predictedSocData[i] = +predictedSoc.toFixed(2);
                }
            }
        }

        const chartEndIndex = targetIndex + 12;

        const trimmedSocData = [...socData];
        const trimmedPredictedSocData = [...predictedSocData];
        const trimmedTimestamps = [...result.timestamps];

        if (chartEndIndex < trimmedSocData.length - 1) {
            trimmedSocData.splice(chartEndIndex + 1, trimmedSocData.length);
            trimmedPredictedSocData.splice(chartEndIndex + 1, trimmedPredictedSocData.length);
            trimmedTimestamps.splice(chartEndIndex + 1, trimmedTimestamps.length);
        }

        if (startIndex > 0) {
            trimmedSocData.splice(0, startIndex);
            trimmedPredictedSocData.splice(0, startIndex);
            trimmedTimestamps.splice(0, startIndex);
        }

        return {
            labels: trimmedTimestamps.map(timestamp => new Date(timestamp)),
            datasets: [
                {
                    type: "line",
                    label: this.translate.instant("GENERAL.SOC"),
                    data: trimmedSocData,
                    hidden: false,
                    yAxisID: ChartAxis.RIGHT,
                    borderWidth: 2,
                    pointRadius: 0,
                    tension: 0,
                    backgroundColor: "rgba(189, 195, 199, 0.05)",
                    borderColor: "rgba(189, 195, 199, 1)",
                },
                {
                    type: "line",
                    label: this.translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.EXPECTED_SOC"),
                    data: trimmedPredictedSocData,
                    hidden: false,
                    yAxisID: ChartAxis.RIGHT,
                    borderWidth: 2,
                    pointRadius: 0,
                    tension: 0,
                    backgroundColor: "rgba(0, 223, 0, 0)",
                    borderColor: "rgba(0, 223, 0, 1)",
                },
            ],
        };
    }

    private prepareDatasets(datasets: Chart.ChartDataset[]): Chart.ChartDataset[] {
        return datasets.map((dataset) => {
            const preparedDataset = dataset as PredictionChartDataset;

            return {
                ...preparedDataset,
                yAxisID: ChartAxis.RIGHT,
                backgroundColor: preparedDataset.backgroundColor != null
                    ? (ColorUtils.changeOpacityFromRGBA(
                        preparedDataset.backgroundColor.toString(),
                        preparedDataset.label === this.translate.instant("GENERAL.SOC") ? 0.05 : 0,
                    ) ?? preparedDataset.backgroundColor)
                    : preparedDataset.backgroundColor,
                borderColor: preparedDataset.borderColor != null
                    ? (ColorUtils.changeOpacityFromRGBA(
                        preparedDataset.borderColor.toString(),
                        1,
                    ) ?? preparedDataset.borderColor)
                    : preparedDataset.borderColor,
            } as Chart.ChartDataset;
        });
    }

    private createPredictionChartOptions(): Chart.ChartOptions {
        let options = AbstractHistoryChart.getDefaultXAxisOptions(
            this.xAxisScalingType,
            this.service,
            this.labels,
        );

        if (this.chartObject != null) {
            for (const yAxis of this.chartObject.yAxes) {
                options = AbstractHistoryChart.getYAxisOptions(
                    options,
                    yAxis,
                    this.translate,
                    "line",
                    this.datasets,
                    true,
                    this.chartObject.tooltip.formatNumber,
                );
            }
        }

        const tooltipCallbacks = options.plugins?.tooltip?.callbacks;
        const legendLabels = options.plugins?.legend?.labels;
        const xScale = options.scales?.x as TimeScaleOptions | undefined;
        const rightScale = options.scales?.[ChartAxis.RIGHT];
        const leftScale = options.scales?.[ChartAxis.LEFT];

        if (xScale != null) {
            xScale.offset = false;
            xScale.ticks = {
                ...xScale.ticks,
                source: "auto",
                autoSkip: false,
                maxTicksLimit: 30,
                color: getComputedStyle(document.documentElement).getPropertyValue("--ion-color-chart-xAxis-ticks"),
                callback: (value) => {
                    const date = new Date(value as string | number);
                    return date.getMinutes() === 0 ? `${date.getHours()}:00` : "";
                },
            };
        }

        if (tooltipCallbacks != null) {
            tooltipCallbacks.label = (item: Chart.TooltipItem<any>): string => {
                const label = item.dataset.label ?? "";
                const value = item.dataset.data[item.dataIndex];

                if (value == null) {
                    return "";
                }

                return `${label}: ${value} %`;
            };

            tooltipCallbacks.labelColor = (item: Chart.TooltipItem<any>): Chart.TooltipLabelStyle => {
                let backgroundColor = item.dataset.backgroundColor;

                if (Array.isArray(backgroundColor)) {
                    backgroundColor = backgroundColor[0];
                }

                if (!backgroundColor) {
                    backgroundColor = item.dataset.borderColor || "rgba(0, 0, 0, 0.5)";
                }

                return {
                    borderColor: ColorUtils.changeOpacityFromRGBA(backgroundColor.toString(), 1) ?? backgroundColor.toString(),
                    backgroundColor: backgroundColor.toString(),
                };
            };
        }

        if (legendLabels != null) {
            legendLabels.generateLabels = (chart: Chart.Chart): Chart.LegendItem[] => {
                const legendItems: Chart.LegendItem[] = [];

                chart.data.datasets.forEach((dataset, index) => {
                    const existingItem = legendItems.find(item => item.text === dataset.label);

                    const borderColor = Array.isArray(dataset.borderColor) ? dataset.borderColor[0] : dataset.borderColor;
                    const backgroundColor = Array.isArray(dataset.backgroundColor) ? dataset.backgroundColor[0] : dataset.backgroundColor;

                    if (existingItem != null) {
                        existingItem.datasetIndex = index;
                        existingItem.hidden = !chart.isDatasetVisible(index);
                        existingItem.fillStyle = borderColor as string;
                        existingItem.strokeStyle = borderColor as string;
                        return;
                    }

                    legendItems.push({
                        text: dataset.label?.toString() ?? "",
                        datasetIndex: index,
                        fillStyle: backgroundColor as string,
                        fontColor: getComputedStyle(document.documentElement).getPropertyValue("--ion-color-text"),
                        hidden: !chart.isDatasetVisible(index),
                        lineWidth: 2,
                        strokeStyle: borderColor as string,
                        ...ChartConstants.Plugins.Legend.POINT_STYLE(dataset),
                    });
                });

                return legendItems;
            };
        }

        if (options.plugins?.tooltip != null) {
            options.plugins.tooltip.mode = "index";
        }

        if (rightScale != null) {
            rightScale.grid = {
                ...rightScale.grid,
                display: true,
            };
        }

        if (leftScale != null) {
            leftScale.display = false;
            leftScale.grid = {
                ...leftScale.grid,
                display: false,
            };
        }

        options.animation = false;

        return options;
    }

    private getCurrentFiveMinuteIndex(): number {
        const now = new Date();
        return Math.trunc((now.getHours() * 60 + now.getMinutes()) / 5);
    }

    private findStartSoc(socData: (number | null)[], currentIndex: number): number | null {
        for (let i = currentIndex; i >= 0; i--) {
            if (socData[i] != null) {
                return socData[i];
            }
        }
        return null;
    }
}

type TimeScaleOptions = Chart.TimeScaleOptions;

type PredictionChartDataset = Chart.ChartDataset<"line", (number | null)[]> & {
    yAxisID?: string;
    borderDash?: number[];
};
