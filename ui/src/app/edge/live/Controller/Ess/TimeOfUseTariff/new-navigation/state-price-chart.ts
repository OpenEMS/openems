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
import { Currency, Edge, EdgeConfig, Logger, Service, Websocket } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { ColorUtils } from "src/app/shared/utils/color/color.utils";
import { ChartAxis, HistoryUtils, TimeOfUseTariffUtils, YAxisType } from "src/app/shared/utils/utils";

import { Controller_Ess_TimeOfUseTariffUtils } from "../utils";

@Component({
    selector: "oe-state-price-chart",
    templateUrl: "../../../../../history/abstracthistorychart.html",
    standalone: false,
})
export class ScheduleStateAndPriceChartComponent extends AbstractHistoryChart implements OnChanges {

    @Input({ required: true }) public refresh!: boolean;
    @Input({ required: true }) public override edge!: Edge;
    @Input({ required: true }) public override component!: EdgeConfig.Component;

    private currencyLabel: Currency.Label | undefined = undefined;
    private currencyUnit: Currency.Unit | undefined = undefined;

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
            output: () => [],
            tooltip: {
                formatNumber: ChartConstants.NumberFormat.ZERO_TO_TWO,
            },
            yAxes: [
                {
                    unit: YAxisType.CURRENCY,
                    position: "left",
                    yAxisId: ChartAxis.LEFT,
                    customTitle: this.currencyUnit ?? "",
                    scale: { dynamicScale: true },
                },
                {
                    unit: YAxisType.PERCENTAGE,
                    position: "right",
                    yAxisId: ChartAxis.RIGHT,
                    displayGrid: false,
                },
                {
                    unit: YAxisType.POWER,
                    position: "right",
                    yAxisId: ChartAxis.RIGHT_2,
                    displayGrid: false,
                },
            ],
        };
    }

    protected override getChartHeight(): number | null {
        return TimeOfUseTariffUtils.getChartHeight(this.service.isSmartphoneResolution);
    }

    protected override async loadChart(): Promise<void> {
        if (this.edge == null || this.component == null || this.config == null) {
            return;
        }

        this.labels = [];
        this.errorResponse = null;
        this.loading = true;
        this.chartType = "line";

        try {
            const meta: EdgeConfig.Component = this.config.getComponent("_meta");
            const currency = this.config.getPropertyFromComponent<string>(meta, "currency");
            if (currency != null) {
                this.currencyLabel = Currency.getCurrencyLabelByCurrency(currency);
                this.currencyUnit = Currency.getChartCurrencyUnitLabel(currency);
            }

            this.chartObject = this.getChartData();

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

            const priceArray = schedule.map(entry => entry.price);
            const stateArray = schedule.map(entry => entry.state);
            const timestampArray = schedule.map(entry => entry.timestamp);
            const gridBuyArray = schedule.map(entry => HistoryUtils.ValueConverter.NEGATIVE_AS_ZERO(entry.grid));
            const socArray = schedule.map(entry => entry.soc);

            const scheduleChartData = Controller_Ess_TimeOfUseTariffUtils.getScheduleChartData(
                schedule.length,
                priceArray,
                stateArray,
                timestampArray,
                gridBuyArray,
                socArray,
                this.translate,
                this.component.properties.controlMode,
            );

            this.labels = scheduleChartData.labels;
            this.datasets = this.prepareDatasets(scheduleChartData.datasets);
            this.legendOptions = this.datasets
                .filter((dataset, index, arr) => arr.findIndex(d => d.label === dataset.label) === index)
                .map(dataset => ({
                    label: dataset.label?.toString() ?? "",
                    strokeThroughHidingStyle: false,
                    hideLabelInLegend: false,
                }));

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

    private prepareDatasets(datasets: Chart.ChartDataset[]): Chart.ChartDataset[] {
        const typedDatasets = datasets as StatePriceChartDataset[];

        return typedDatasets.map((dataset) => {
            const preparedDataset: StatePriceChartDataset = {
                ...dataset,
            };

            const opacity = preparedDataset.type === "line" ? 0.2 : 0.5;

            if (preparedDataset.backgroundColor != null) {
                preparedDataset.backgroundColor = ColorUtils.changeOpacityFromRGBA(
                    preparedDataset.backgroundColor.toString(),
                    opacity,
                ) ?? preparedDataset.backgroundColor;
            }

            if (preparedDataset.borderColor != null) {
                preparedDataset.borderColor = ColorUtils.changeOpacityFromRGBA(
                    preparedDataset.borderColor.toString(),
                    1,
                ) ?? preparedDataset.borderColor;
            }

            if (preparedDataset.label === this.translate.instant("GENERAL.GRID_BUY_ADVANCED")) {
                preparedDataset.yAxisID = ChartAxis.RIGHT_2;
            } else if (preparedDataset.label === this.translate.instant("GENERAL.SOC")) {
                preparedDataset.yAxisID = ChartAxis.RIGHT;
            } else {
                preparedDataset.yAxisID = ChartAxis.LEFT;
            }

            return preparedDataset;
        }) as Chart.ChartDataset[];
    }

    private createScheduleChartOptions(): Chart.ChartOptions {
        let options = AbstractHistoryChart.getDefaultXAxisOptions(
            this.xAxisScalingType,
            this.service,
            this.labels,
        );

        AssertionUtils.assertIsDefined(this.chartObject);

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

        const tooltipCallbacks = options.plugins?.tooltip?.callbacks;
        const legendLabels = options.plugins?.legend?.labels;
        const xScale = options.scales?.x as TimeScaleOptions | undefined;
        const rightScale = options.scales?.[ChartAxis.RIGHT];
        const right2Scale = options.scales?.[ChartAxis.RIGHT_2];
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
                return TimeOfUseTariffUtils.getLabel(value, label, this.translate, this.currencyLabel);
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
                    const typedDataset = dataset as StatePriceChartDataset;
                    const existingItem = legendItems.find(item => item.text === typedDataset.label);

                    const borderColor = Array.isArray(typedDataset.borderColor) ? typedDataset.borderColor[0] : typedDataset.borderColor;
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
                        ...ChartConstants.Plugins.Legend.POINT_STYLE(typedDataset as Chart.ChartDataset),
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
                display: false,
            };
        }

        if (right2Scale != null) {
            right2Scale.grid = {
                ...right2Scale.grid,
                display: false,
            };
            right2Scale.suggestedMin = 0;
            right2Scale.suggestedMax = 1;
        }

        if (leftScale != null && options.scales != null) {
            const leftDatasets = (this.datasets as StatePriceChartDataset[])
                .filter(dataset => dataset.yAxisID === ChartAxis.LEFT) as Chart.ChartDataset[];

            options.scales[ChartAxis.LEFT] = {
                ...leftScale,
                ...ChartConstants.DEFAULT_Y_SCALE_OPTIONS(
                    {
                        position: "left",
                        unit: YAxisType.CURRENCY,
                        yAxisId: ChartAxis.LEFT,
                        customTitle: this.currencyUnit,
                        scale: { dynamicScale: true },
                    },
                    this.translate,
                    "bar",
                    leftDatasets,
                    true,
                ),
            };
        }

        options.animation = false;

        return options;
    }
}

type StatePriceChartDataset =
    | (Chart.ChartDataset<"line", (number | null)[]> & {
        yAxisID?: string;
        borderDash?: number[];
    })
    | (Chart.ChartDataset<"bar", (number | null)[]> & {
        yAxisID?: string;
        borderDash?: number[];
    });

type TimeScaleOptions = Chart.TimeScaleOptions;
