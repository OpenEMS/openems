import { ChangeDetectorRef, Component, Input, OnChanges, SimpleChanges } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import * as Chart from "chart.js";

import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/chart.constants";
import { NavigationService } from "src/app/shared/components/navigation/service/navigation.service";
import { ViewUtils } from "src/app/shared/components/navigation/view/shared/shared";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { GetScheduleRequest } from "src/app/shared/jsonrpc/request/getScheduleRequest";
import { GetScheduleResponse } from "src/app/shared/jsonrpc/response/getScheduleResponse";
import { QueryHistoricTimeseriesDataResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse";
import { ChannelAddress, Edge, EdgeConfig, Logger, Service, Utils, Websocket } from "src/app/shared/shared";
import { ColorUtils } from "src/app/shared/utils/color/color.utils";
import { ChartAxis, HistoryUtils, TimeOfUseTariffUtils, YAxisType } from "src/app/shared/utils/utils";

@Component({
    selector: "oe-production-chart",
    templateUrl: "../../../../history/abstracthistorychart.html",
    standalone: false,
})
export class ProductionChartComponent extends AbstractHistoryChart implements OnChanges {

    @Input({ required: true }) public refresh!: boolean;
    @Input({ required: true }) public override edge!: Edge;
    @Input({ required: true }) public override component!: EdgeConfig.Component;

    constructor(
        public override service: Service,
        public override cdRef: ChangeDetectorRef,
        protected override translate: TranslateService,
        protected override route: ActivatedRoute,
        protected override logger: Logger,
        protected override navigationService: NavigationService,
        private websocket: Websocket,
    ) {
        super(service, cdRef, translate, route, logger, navigationService);
    }

    public ngOnChanges(changes: SimpleChanges): void {
        if (!this.config) {
            return;
        }

        if (changes["refresh"] || changes["component"] || changes["edge"]) {
            this.updateChart();
        }
    }

    protected override getChartData(): HistoryUtils.ChartData | null {
        return {
            input: [{
                name: "ProductionActivePower",
                powerChannel: ChannelAddress.fromString("_sum/ProductionActivePower"),
            }],
            output: () => [],
            tooltip: {
                formatNumber: "1.1-2",
            },
            yAxes: [{
                unit: YAxisType.ENERGY,
                position: "left",
                yAxisId: ChartAxis.LEFT,
            }],
        };
    }

    protected override getChartHeight(): number | null {
        const fourTimesTheHeight = 400;
        return ViewUtils.getChartContentHeightInVh(window.innerHeight, this.navigationService.position(), fourTimesTheHeight);
    }

    protected override async loadChart(): Promise<void> {
        if (this.edge == null || this.component == null) {
            return;
        }

        this.labels = [];
        this.errorResponse = null;
        this.loading = true;
        this.chartType = "line";
        this.chartObject = this.getChartData();

        try {
            const now = new Date();
            const nowMs = now.getTime();
            // Apply a delay to align datasets: forecast data is in 15-minute intervals,
            // while historical data is in 5-minute intervals, which can cause 15 minute gaps at the transition
            const delayedNowMs = nowMs - 10 * 60 * 1000;

            const startOfToday = new Date();
            startOfToday.setHours(0, 0, 0, 0);

            const endOfToday = new Date(startOfToday);
            endOfToday.setHours(24, 0, 0, 0);

            const [historyResponse, scheduleResponse] = await Promise.all([
                this.queryHistoricTimeseriesData(startOfToday, now) as Promise<QueryHistoricTimeseriesDataResponse>,
                this.edge.sendRequest(
                    this.websocket,
                    new ComponentJsonApiRequest({
                        componentId: this.component.id,
                        payload: new GetScheduleRequest(),
                    }),
                ) as Promise<GetScheduleResponse>,
            ]);

            const schedule = (scheduleResponse as GetScheduleResponse).result.schedule ?? [];

            const historyTimestamps = historyResponse?.result?.timestamps?.map(timestamp => new Date(timestamp)) ?? [];
            const historyValuesRaw =
                historyResponse?.result?.data?.["_sum/ProductionActivePower"]
                ?? historyResponse?.result?.data?.[ChannelAddress.fromString("_sum/ProductionActivePower").toString()]
                ?? [];

            const historyPoints = historyTimestamps
                .map((timestamp, index) => ({
                    timestamp: new Date(timestamp).getTime(),
                    value: Utils.divideSafely(historyValuesRaw[index], 1000),
                }))
                .filter(point =>
                    point.timestamp >= startOfToday.getTime()
                    && point.timestamp <= nowMs
                    && point.value != null,
                )
                .sort((a, b) => a.timestamp - b.timestamp);

            const forecastPoints = schedule
                .map(entry => ({
                    timestamp: new Date(entry.timestamp).getTime(),
                    value: Utils.divideSafely(entry.production, 1000),
                }))
                .filter(point =>
                    point.timestamp >= startOfToday.getTime()
                    && point.timestamp <= endOfToday.getTime(),
                )
                .sort((a, b) => a.timestamp - b.timestamp);

            const allTimestamps = Array.from(new Set([
                ...historyPoints.map(point => point.timestamp),
                ...forecastPoints.map(point => point.timestamp),
            ])).sort((a, b) => a - b);

            const actualMap = new Map<number, number | null>();
            const forecastMap = new Map<number, number | null>();

            historyPoints.forEach(point => actualMap.set(point.timestamp, point.value));
            forecastPoints.forEach(point => forecastMap.set(point.timestamp, point.value));

            const actualData: (number | null)[] = [];
            const forecastData: (number | null)[] = [];

            for (const timestamp of allTimestamps) {
                if (timestamp <= delayedNowMs) {
                    actualData.push(actualMap.get(timestamp) ?? null);
                    forecastData.push(null);
                } else {
                    actualData.push(null);
                    forecastData.push(forecastMap.get(timestamp) ?? null);
                }
            }

            this.labels = allTimestamps.map(timestamp => new Date(timestamp));

            this.datasets = [
                {
                    type: "line",
                    label: this.translate.instant("GENERAL.PRODUCTION"),
                    data: actualData,
                    hidden: false,
                    order: 1,
                    yAxisID: ChartAxis.LEFT,
                    backgroundColor: ColorUtils.rgbStringToRgba(ChartConstants.Colors.BLUE, 0.2),
                    borderColor: ChartConstants.Colors.BLUE,
                    borderWidth: 2,
                    tension: 0,
                    ...ChartConstants.Plugins.Datasets.HOVER_ENHANCE({ backgroundColor: ColorUtils.rgbStringToRgba(ChartConstants.Colors.BLUE, 0.2), borderColor: ChartConstants.Colors.BLUE }),
                },
                {
                    type: "line",
                    label: this.translate.instant("GENERAL.PRODUCTION"),
                    data: forecastData,
                    hidden: false,
                    order: 1,
                    yAxisID: ChartAxis.LEFT,
                    backgroundColor: ColorUtils.rgbStringToRgba(ChartConstants.Colors.BLUE, 0.2),
                    borderColor: ChartConstants.Colors.BLUE,
                    borderWidth: 2,
                    borderDash: [6, 6],
                    tension: 0,
                    ...ChartConstants.Plugins.Datasets.HOVER_ENHANCE({ backgroundColor: ColorUtils.rgbStringToRgba(ChartConstants.Colors.BLUE, 0.2), borderColor: ChartConstants.Colors.BLUE }),
                },
            ];

            this.legendOptions = [{
                label: this.translate.instant("GENERAL.PRODUCTION"),
                strokeThroughHidingStyle: false,
                hideLabelInLegend: false,
            }];

            this.channelData = { data: {} };

            this.options = AbstractHistoryChart.getDefaultXAxisOptions(
                this.xAxisScalingType,
                this.service,
                this.labels,
            );

            if (this.options == null) {
                return;
            }

            const tooltipCallbacks = this.options.plugins?.tooltip?.callbacks;
            const xScale = this.options.scales?.x;

            if (tooltipCallbacks == null || xScale == null) {
                return;
            }

            tooltipCallbacks.title = (tooltipItems: Chart.TooltipItem<any>[]): string => {
                if (tooltipItems.length === 0) {
                    return "";
                }

                const date = tooltipItems[0]?.label;
                if (date == null) {
                    return "";
                }

                return AbstractHistoryChart["toTooltipTitle"](
                    startOfToday,
                    endOfToday,
                    date,
                    this.service,
                    this.xAxisScalingType,
                );
            };

            tooltipCallbacks.label = (item: Chart.TooltipItem<any>): string => {
                const label = this.translate.instant("GENERAL.PRODUCTION");
                const value = item.dataset.data[item.dataIndex];

                return TimeOfUseTariffUtils.getLabel(value, label, this.translate);
            };

            tooltipCallbacks.labelColor = (item: Chart.TooltipItem<any>): Chart.TooltipLabelStyle => {
                const datasetBackgroundColor = item.dataset.backgroundColor;
                const datasetBorderColor = item.dataset.borderColor;

                let backgroundColor: string;

                if (Array.isArray(datasetBackgroundColor)) {
                    backgroundColor = datasetBackgroundColor[0]?.toString() ?? "rgba(0, 0, 0, 0.5)";
                } else if (datasetBackgroundColor != null) {
                    backgroundColor = datasetBackgroundColor.toString();
                } else if (Array.isArray(datasetBorderColor)) {
                    backgroundColor = datasetBorderColor[0]?.toString() ?? "rgba(0, 0, 0, 0.5)";
                } else if (datasetBorderColor != null) {
                    backgroundColor = datasetBorderColor.toString();
                } else {
                    backgroundColor = "rgba(0, 0, 0, 0.5)";
                }

                return {
                    borderColor: ColorUtils.changeOpacityFromRGBA(backgroundColor, 1) ?? backgroundColor,
                    backgroundColor: ColorUtils.changeOpacityFromRGBA(backgroundColor, 1) ?? backgroundColor,
                };
            };

            const leftYAxis: HistoryUtils.yAxes = {
                position: "left",
                unit: YAxisType.POWER,
                yAxisId: ChartAxis.LEFT,
            };

            this.options = AbstractHistoryChart.getYAxisOptions(
                this.options,
                leftYAxis,
                this.translate,
                "line",
                this.datasets,
                true,
            );

            const updatedXScale = this.options.scales?.x;
            const updatedLeftScale = this.options.scales?.[ChartAxis.LEFT];
            const updatedRightScale = this.options.scales?.[ChartAxis.RIGHT];

            if (updatedXScale != null) {
                updatedXScale.min = startOfToday.getTime();
                updatedXScale.max = endOfToday.getTime();
                updatedXScale.ticks = {
                    source: "auto",
                    autoSkip: false,
                    color: getComputedStyle(document.documentElement).getPropertyValue("--ion-color-chart-xAxis-ticks"),
                    callback: (value) => {
                        const date = new Date(value);
                        return date.getMinutes() === 0 ? `${date.getHours()}:00` : "";
                    },
                };
            }

            if (updatedRightScale != null) {
                updatedRightScale.grid = {
                    ...updatedRightScale.grid,
                    display: false,
                };
            }

            if (updatedLeftScale != null) {
                updatedLeftScale.suggestedMin = 0;
                updatedLeftScale.suggestedMax = 1;
            }

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
        }
    }
}
