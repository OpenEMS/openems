import { ChangeDetectorRef, Component, Input, OnChanges, SimpleChanges } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import * as Chart from "chart.js";

import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/chart.constants";
import { NavigationService } from "src/app/shared/components/navigation/service/navigation.service";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { GetScheduleRequest } from "src/app/shared/jsonrpc/request/getScheduleRequest";
import { GetScheduleResponse } from "src/app/shared/jsonrpc/response/getScheduleResponse";
import { Edge, EdgeConfig, Logger, Service, Websocket } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { ColorUtils } from "src/app/shared/utils/color/color.utils";
import { DateUtils } from "src/app/shared/utils/date/dateutils";
import { NumberUtils } from "src/app/shared/utils/number/number-utils";
import { ChartAxis, HistoryUtils, TimeOfUseTariffUtils, YAxisType } from "src/app/shared/utils/utils";

@Component({
    selector: "oe-power-soc-chart",
    templateUrl: "../../../../../history/abstracthistorychart.html",
    standalone: false,
})
export class SchedulePowerAndSocChartComponent extends AbstractHistoryChart implements OnChanges {

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

    protected override getChartData(): HistoryUtils.ChartData {
        return {
            input: [],
            output: (data: HistoryUtils.ChannelData) => [
                {
                    name: this.translate.instant("GENERAL.GRID_BUY"),
                    converter: () => data["gridBuy"] ?? [],
                    yAxisId: ChartAxis.LEFT,
                    color: ChartConstants.Colors.BLUE_GREY,
                    stack: 0,
                    order: 1,
                    hiddenOnInit: true,
                },
                {
                    name: this.translate.instant("GENERAL.GRID_SELL"),
                    converter: () => data["gridSell"] ?? [],
                    yAxisId: ChartAxis.LEFT,
                    color: ChartConstants.Colors.PURPLE,
                    stack: 0,
                    order: 1,
                    hiddenOnInit: true,
                },
                {
                    name: this.translate.instant("GENERAL.PRODUCTION"),
                    converter: () => data["production"] ?? [],
                    yAxisId: ChartAxis.LEFT,
                    color: ChartConstants.Colors.BLUE,
                    stack: 0,
                    order: 1,
                },
                {
                    name: this.translate.instant("GENERAL.CONSUMPTION"),
                    converter: () => data["consumption"] ?? [],
                    yAxisId: ChartAxis.LEFT,
                    color: ChartConstants.Colors.YELLOW,
                    stack: 0,
                    order: 1,
                },
                {
                    name: this.translate.instant("GENERAL.CHARGE"),
                    converter: () => data["essCharge"] ?? [],
                    yAxisId: ChartAxis.LEFT,
                    color: ChartConstants.Colors.GREEN,
                    stack: 0,
                    order: 1,
                    hiddenOnInit: true,
                },
                {
                    name: this.translate.instant("GENERAL.DISCHARGE"),
                    converter: () => data["essDischarge"] ?? [],
                    yAxisId: ChartAxis.LEFT,
                    color: ChartConstants.Colors.RED,
                    stack: 0,
                    order: 1,
                    hiddenOnInit: true,
                },
                {
                    name: this.translate.instant("GENERAL.SOC"),
                    converter: () => data["soc"] ?? [],
                    yAxisId: ChartAxis.RIGHT,
                    color: ChartConstants.Colors.GREY,
                    stack: 1,
                    order: 1,
                },
            ],
            tooltip: {
                formatNumber: ChartConstants.NumberFormat.ONE_TO_TWO,
            },
            yAxes: [
                {
                    unit: YAxisType.POWER,
                    position: "left",
                    yAxisId: ChartAxis.LEFT,
                },
                {
                    unit: YAxisType.PERCENTAGE,
                    position: "right",
                    yAxisId: ChartAxis.RIGHT,
                    displayGrid: false,
                },
            ],
        };
    }

    protected override getChartHeight(): number | null {
        return TimeOfUseTariffUtils.getChartHeight(this.service.isSmartphoneResolution);
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
            const response = await this.edge.sendRequest(
                this.websocket,
                new ComponentJsonApiRequest({
                    componentId: this.component.id,
                    payload: new GetScheduleRequest(),
                }),
            ) as GetScheduleResponse;

            const schedule = response?.result?.schedule ?? [];

            if (schedule.length === 0) {
                this.initializeChart();
                return;
            }

            this.labels = schedule.map(entry => new Date(entry.timestamp));

            const channelData: HistoryUtils.ChannelData = {
                gridBuy: schedule.map(entry => NumberUtils.divideSafely(HistoryUtils.ValueConverter.NEGATIVE_AS_ZERO(entry.grid), 1000) ?? 0),
                gridSell: schedule.map(entry => NumberUtils.divideSafely(HistoryUtils.ValueConverter.POSITIVE_AS_ZERO_AND_INVERT_NEGATIVE(entry.grid), 1000) ?? 0),
                production: schedule.map(entry => NumberUtils.divideSafely(entry.production, 1000) ?? 0),
                consumption: schedule.map(entry => NumberUtils.divideSafely(entry.consumption, 1000) ?? 0),
                essCharge: schedule.map(entry => NumberUtils.divideSafely(HistoryUtils.ValueConverter.POSITIVE_AS_ZERO_AND_INVERT_NEGATIVE(entry.ess), 1000) ?? 0),
                essDischarge: schedule.map(entry => NumberUtils.divideSafely(HistoryUtils.ValueConverter.NEGATIVE_AS_ZERO(entry.ess), 1000) ?? 0),
                soc: schedule.map(entry => entry.soc ?? 0),
            };

            this.datasets = this.buildDatasetsFromChartData(channelData, this.labels as Date[]);
            this.legendOptions = this.datasets
                .filter((dataset, index, arr) => arr.findIndex(d => d.label === dataset.label) === index)
                .map(dataset => ({
                    label: dataset.label?.toString() ?? "",
                    strokeThroughHidingStyle: false,
                    hideLabelInLegend: false,
                }));

            this.channelData = { data: channelData };
            this.options = this.createScheduleChartOptions();

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

    private buildDatasetsFromChartData(
        data: HistoryUtils.ChannelData,
        labels: Date[],
    ): Chart.ChartDataset[] {
        AssertionUtils.assertIsDefined(this.chartObject);

        const displayValues = this.chartObject.output(data, labels);

        const baseDatasets = displayValues.map(displayValue => {
            const dataset = AbstractHistoryChart.getDataSet(
                displayValue,
                displayValue.name,
                displayValue.converter(),
                displayValue.stack as number,
                this.chartObject ?? HistoryUtils.ChartData.EMPTY,
                "line",
            );

            dataset.hidden = displayValue.hiddenOnInit ?? false;

            return dataset;
        });

        const now = new Date();

        return baseDatasets.flatMap((dataset) => {
            const pastData: (number | null)[] = [];
            const futureData: (number | null)[] = [];

            let lastPastDatasetEntryIndex: number | null = null;

            labels.forEach((timestamp, i) => {
                const isPastOrNow = DateUtils.isDateBefore(timestamp, now);

                if (isPastOrNow) {
                    pastData.push(dataset.data[i] as number ?? null);
                    futureData.push(null);
                    lastPastDatasetEntryIndex = i;
                } else {
                    pastData.push(null);
                    futureData.push(dataset.data[i] as number ?? null);
                }
            });

            if (lastPastDatasetEntryIndex != null) {
                futureData[lastPastDatasetEntryIndex] = dataset.data[lastPastDatasetEntryIndex] as number ?? null;
            }

            return [
                {
                    ...dataset,
                    data: pastData,
                    borderDash: [],
                },
                {
                    ...dataset,
                    data: futureData,
                    borderDash: ChartConstants.Plugins.Datasets.DEFAULT_BORDER_DASH,
                },
            ];
        });
    }

    private createScheduleChartOptions(): Chart.ChartOptions {
        AssertionUtils.assertIsDefined(this.chartObject);

        let options = AbstractHistoryChart.getDefaultXAxisOptions(
            this.xAxisScalingType,
            this.service,
            this.labels,
        );

        options = AbstractHistoryChart.getYAxisOptions(
            options,
            this.chartObject.yAxes[0],
            this.translate,
            "line",
            this.datasets,
            true,
            this.chartObject.tooltip.formatNumber,
        );

        options = AbstractHistoryChart.getYAxisOptions(
            options,
            this.chartObject.yAxes[1],
            this.translate,
            "line",
            this.datasets,
            true,
            this.chartObject.tooltip.formatNumber,
        );

        const tooltipCallbacks = options.plugins?.tooltip?.callbacks;
        const legendLabels = options.plugins?.legend?.labels;
        const leftScale = options.scales?.[ChartAxis.LEFT];
        const rightScale = options.scales?.[ChartAxis.RIGHT];
        const xScale = options.scales?.x;

        if (tooltipCallbacks != null) {
            tooltipCallbacks.label = (item: Chart.TooltipItem<any>): string => {
                const label = item.dataset.label ?? "";
                const value = item.dataset.data[item.dataIndex];
                return TimeOfUseTariffUtils.getLabel(value, label, this.translate);
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
                    backgroundColor: ColorUtils.changeOpacityFromRGBA(backgroundColor.toString(), 1) ?? backgroundColor.toString(),
                };
            };
        }

        if (legendLabels != null) {
            legendLabels.generateLabels = (chart: Chart.Chart): Chart.LegendItem[] => {
                const legendItems: Chart.LegendItem[] = [];

                chart.data.datasets.forEach((dataset, index) => {
                    const typedDataset = dataset as StatePriceChartDataset;
                    const existingItem = legendItems.find(item => item.text === dataset.label);

                    const borderColor = Array.isArray(typedDataset.borderColor) ? typedDataset.borderColor[0] : dataset.borderColor;
                    const backgroundColor = Array.isArray(typedDataset.backgroundColor) ? typedDataset.backgroundColor[0] : typedDataset.backgroundColor;

                    if (existingItem != null) {
                        existingItem.datasetIndex = index;
                        existingItem.hidden = !chart.isDatasetVisible(index);
                        existingItem.fillStyle = borderColor as string;
                        existingItem.strokeStyle = borderColor as string;

                        if (typedDataset.borderDash != null) {
                            existingItem.lineDash = [];
                        }
                        return;
                    }

                    legendItems.push({
                        text: typedDataset.label?.toString() ?? "",
                        datasetIndex: index,
                        fillStyle: backgroundColor as string,
                        fontColor: getComputedStyle(document.documentElement).getPropertyValue("--ion-color-text"),
                        hidden: !chart.isDatasetVisible(index),
                        lineWidth: 2,
                        ...(typedDataset.borderDash && { lineDash: typedDataset.borderDash }),
                        strokeStyle: borderColor as string,
                        ...ChartConstants.Plugins.Legend.POINT_STYLE(dataset),
                    });
                });

                return legendItems;
            };
        }

        if (options.plugins?.legend != null) {
            options.plugins.legend.onClick = function (event: Chart.ChartEvent, legendItem: Chart.LegendItem) {
                const chart: Chart.Chart = this.chart;

                const legendItems = chart.data.datasets.reduce((arr, ds, i) => {
                    if (ds.label === legendItem.text) {
                        arr.push({ index: i });
                    }
                    return arr;
                }, [] as { index: number }[]);

                legendItems.forEach(item => {
                    const visible = chart.isDatasetVisible(legendItem.datasetIndex ?? 0);
                    const meta = chart.getDatasetMeta(item.index);
                    meta.hidden = visible;
                });

                const scales = chart.options.scales;
                if (scales == null) {
                    chart.update();
                    return;
                }

                for (const key of Object.keys(scales).filter(key => key !== "x")) {
                    const axisDatasets = chart.data.datasets
                        .map((d, i) => ({ dataset: d, index: i }))
                        .filter(d => {
                            const typedDataset = d.dataset as StatePriceChartDataset;
                            return typedDataset.yAxisID === key;
                        });

                    chart.scales[key].options.display = axisDatasets.some(d => chart.isDatasetVisible(d.index));
                }

                chart.update();
            };
        }

        if (xScale != null) {
            xScale.ticks = {
                source: "auto",
                autoSkip: false,
                color: getComputedStyle(document.documentElement).getPropertyValue("--ion-color-chart-xAxis-ticks"),
                callback: (value) => {
                    const date = new Date(value);
                    return date.getMinutes() === 0 ? `${date.getHours()}:00` : "";
                },
            };
        }

        if (rightScale != null) {
            rightScale.grid = {
                ...rightScale.grid,
                display: false,
            };
        }

        if (leftScale != null) {
            leftScale.suggestedMin = 0;
            leftScale.suggestedMax = 1;
        }

        return options;
    }
}

type StatePriceChartDataset =
    | (Chart.ChartDataset<"line", (number | null)[]> & {
        yAxisID?: string;
        borderDash?: number[];
    });
