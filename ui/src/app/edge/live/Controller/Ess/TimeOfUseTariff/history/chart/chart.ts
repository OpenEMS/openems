// @ts-strict-ignore
import { ChangeDetectorRef, Component, effect, inject, ChangeDetectionStrategy } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import * as Chart from "chart.js";
import { calculateResolution, ChronoUnit, Resolution } from "src/app/edge/history/shared";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/chart.constants";
import { NavigationService } from "src/app/shared/components/navigation/service/navigation.service";
import { UserService } from "src/app/shared/service/user.service";
import { ChannelAddress, Currency, EdgeConfig, Logger, Service, Websocket } from "src/app/shared/shared";
import { ColorUtils } from "src/app/shared/utils/color/color.utils";
import { ChartAxis, HistoryUtils, TimeOfUseTariffUtils, Utils, YAxisType } from "src/app/shared/utils/utils";

@Component({
    selector: "oe-time-of-use-detail-chart",
    templateUrl: "../../../../../../../shared/components/chart/abstracthistorychart.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class ChartComponent extends AbstractHistoryChart {
    private currencyUnit: Currency.Unit | null = null;
    private currencyLabel: Currency.Label; // Default

    // TODO INTERSOLAR
    private userService: UserService = inject(UserService);

    constructor(
        private websocket: Websocket,
        public override service: Service,
        public override cdRef: ChangeDetectorRef,
        protected override translate: TranslateService,
        protected override route: ActivatedRoute,
        protected override logger: Logger,
        protected override navigationService: NavigationService,
    ) {
        super(service, cdRef, translate, route, logger, navigationService);
        effect(() => {
            const edge = this.service.currentEdge();

            if (!edge) {
                return;
            }

            edge.getFirstValidConfig(this.websocket).then((config) => {
                const meta: EdgeConfig.Component = config?.getComponent("_meta");
                const currency: string = config?.getPropertyFromComponent<string>(meta, "currency");
                this.currencyUnit = Currency.getChartCurrencyUnitLabel(currency);
            });
        });
    }

    protected override getChartData(): HistoryUtils.ChartData {
        // Assigning the component to be able to use the id.
        const componentId: string = this.config.getComponentIdsByFactory("Controller.Ess.Time-Of-Use-Tariff")[0];
        this.component = this.config.components[componentId];

        const meta: EdgeConfig.Component = this.config?.getComponent("_meta");
        const currency: string = this.config?.getPropertyFromComponent<string>(meta, "currency");
        this.currencyLabel = Currency.getCurrencyLabelByCurrency(currency);
        this.chartType = "bar";

        // TODO INTERSOLAR
        const edgeId: string | null = this.edge?.id ?? null;
        const userId: string | null = this.userService.currentUser()?.id ?? null;

        return {
            input: [
                {
                    name: "QuarterlyPrice",
                    powerChannel: ChannelAddress.fromString(this.component.id + "/QuarterlyPrices"),
                },
                {
                    name: "StateMachine",
                    powerChannel: ChannelAddress.fromString(this.component.id + "/StateMachine"),
                },
                {
                    name: "Soc",
                    powerChannel: ChannelAddress.fromString("_sum/EssSoc"),
                },
                {
                    name: "GridBuy",
                    powerChannel: ChannelAddress.fromString("_sum/GridActivePower"),
                    converter: HistoryUtils.ValueConverter.NEGATIVE_AS_ZERO,
                },
            ],
            output: (data: HistoryUtils.ChannelData) => {
                return [
                    {
                        name: this.translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.BALANCING"),
                        converter: () => this.getDataset(data, TimeOfUseTariffUtils.State.Balancing, edgeId, userId),
                        color: ChartConstants.Colors.ESS_MODE_BALANCING,
                        stack: 1,
                        custom: {
                            formatNumber: ChartConstants.NumberFormat.TWO,
                        },
                        order: 2,
                    },
                    {
                        name: this.translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.CHARGE_GRID"),
                        converter: () => this.getDataset(data, TimeOfUseTariffUtils.State.ChargeGrid, edgeId, userId),
                        color: ChartConstants.Colors.ESS_MODE_CHARGE_GRID,
                        stack: 1,
                        order: 2,
                    },
                    {
                        name: this.translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.DELAY_DISCHARGE"),
                        converter: () =>
                            this.getDataset(data, TimeOfUseTariffUtils.State.DelayDischarge, edgeId, userId),
                        color: ChartConstants.Colors.ESS_MODE_DELAY_DISCHARGE,
                        stack: 1,
                        order: 2,
                    },
                    {
                        name: this.translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.PEAK_SHAVING"),
                        converter: () => this.getDataset(data, TimeOfUseTariffUtils.State.PeakShaving, edgeId, userId),
                        color: ChartConstants.Colors.ESS_MODE_PEAK_SHAVING,
                        stack: 1,
                        order: 2,
                    },
                    {
                        name: this.translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.DELAY_CHARGE"),
                        converter: () => this.getDataset(data, TimeOfUseTariffUtils.State.DelayCharge, edgeId, userId),
                        color: ChartConstants.Colors.ESS_MODE_DELAY_CHARGE,
                        stack: 1,
                        order: 2,
                    },
                    {
                        name: this.translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.LIMIT_CHARGE"),
                        converter: () => this.getDataset(data, TimeOfUseTariffUtils.State.LimitCharge, edgeId, userId),
                        color: ChartConstants.Colors.ESS_MODE_LIMIT_CHARGE,
                        stack: 1,
                        order: 2,
                    },
                    {
                        name: this.translate.instant(
                            "EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.AVOID_GRID_SELL_LIMIT",
                        ),
                        converter: () =>
                            this.getDataset(data, TimeOfUseTariffUtils.State.AvoidGridSellLimit, edgeId, userId),
                        color: ChartConstants.Colors.ESS_MODE_AVOID_FEED_IN_LIMIT,
                        stack: 1,
                        order: 2,
                    },
                    {
                        name: this.translate.instant(
                            "EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.DISCHARGE_CONSUMPTION",
                        ),
                        converter: () =>
                            this.getDataset(data, TimeOfUseTariffUtils.State.DischargeConsumption, edgeId, userId),
                        color: ChartConstants.Colors.ESS_MODE_DISCHARGE_CONSUMPTION,
                        stack: 1,
                        order: 2,
                    },
                    {
                        name: this.translate.instant("GENERAL.SOC"),
                        converter: () => data["Soc"]?.map((value) => Utils.multiplySafely(value, 1000)),
                        color: "rgb(189, 195, 199)",
                        borderDash: [10, 10],
                        yAxisId: ChartAxis.RIGHT,
                        custom: {
                            type: "line",
                            unit: YAxisType.PERCENTAGE,
                            formatNumber: "1.0-0",
                        },
                        order: 1,
                    },
                    {
                        name: this.translate.instant("GENERAL.GRID_BUY_ADVANCED"),
                        converter: () => data["GridBuy"],
                        color: ChartConstants.Colors.BLUE_GREY,
                        yAxisId: ChartAxis.RIGHT_2,
                        custom: {
                            type: "line",
                            formatNumber: "1.0-0",
                        },
                        hiddenOnInit: true,
                        order: 0,
                    },
                ];
            },

            tooltip: {
                formatNumber: ChartConstants.NumberFormat.TWO,
            },
            yAxes: [
                {
                    unit: YAxisType.CURRENCY,
                    position: "left",
                    yAxisId: ChartAxis.LEFT,
                    customTitle: Currency.getChartCurrencyUnitLabel(currency),
                    scale: {
                        dynamicScale: true,
                    },
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

    protected override async loadChart() {
        this.labels = [];
        this.errorResponse = null;

        const unit: Resolution = { unit: ChronoUnit.Type.MINUTES, value: 15 };
        this.queryHistoricTimeseriesData(
            this.service.historyPeriod.value.from,
            this.service.historyPeriod.value.to,
            unit,
        ).then((dataResponse) => {
            this.chartType = "line";
            this.chartObject = this.getChartData();

            const displayValues = AbstractHistoryChart.fillChart(this.chartType, this.chartObject, dataResponse);

            // Hide certain datasets if they contain no data
            [this.translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.PEAK_SHAVING")].forEach((label) => {
                const dataset = displayValues.datasets.find((ds) => ds.label === label);
                if (dataset && dataset.data.every((value: any) => value === null)) {
                    displayValues.datasets = displayValues.datasets.filter((ds) => ds.label !== label);
                    displayValues.labels = displayValues.labels.filter((l) => l !== label);
                }
            });

            this.datasets = displayValues.datasets;
            this.legendOptions = displayValues.legendOptions;
            this.labels = displayValues.labels;
            this.setChartLabel();

            this.chartObject.yAxes.forEach((element) => {
                this.options = AbstractHistoryChart.getYAxisOptions(
                    this.options,
                    element,
                    this.translate,
                    this.chartType,
                    this.datasets,
                    true,
                    this.chartObject.tooltip.formatNumber,
                );
            });

            this.options.scales.x["time"].unit = calculateResolution(
                this.service,
                this.service.historyPeriod.value.from,
                this.service.historyPeriod.value.to,
            ).timeFormat;
            this.options.scales.x.ticks["source"] = "auto";
            this.options.scales.x.grid = { offset: false };
            this.options.plugins.tooltip.mode = "index";
            this.options.scales.x.ticks.maxTicksLimit = 30;
            this.options.scales[ChartAxis.LEFT].min = this.getMinimumAxisValue(this.datasets);

            this.options.plugins.tooltip.callbacks.labelColor = (item: Chart.TooltipItem<any>) => {
                return {
                    borderColor: ColorUtils.changeOpacityFromRGBA(item.dataset.borderColor, 1),
                    backgroundColor: item.dataset.backgroundColor,
                };
            };
            this.options.scales.x["bounds"] = "ticks";

            this.options.plugins.tooltip.callbacks.label = (item: Chart.TooltipItem<any>) => {
                const label = item.dataset.label;
                const value = item.dataset.data[item.dataIndex];

                return TimeOfUseTariffUtils.getLabel(value, label, this.translate, this.currencyLabel);
            };

            this.options.scales[ChartAxis.LEFT]["title"].text = this.currencyUnit;
            this.datasets = this.datasets.map((el) => {
                const opacity = el.type === "line" ? 0.2 : 0.5;

                el.backgroundColor = ColorUtils.changeOpacityFromRGBA(el.backgroundColor.toString(), opacity);
                el.borderColor = ColorUtils.changeOpacityFromRGBA(el.borderColor.toString(), 1);
                return el;
            });

            this.options.scales.x["offset"] = false;
            this.options["animation"] = false;
        });
    }

    /**
     * Returns only the desired state data extracted from the whole dataset.
     *
     * @param data The historic data.
     * @param desiredState The desired state data from the whole dataset.
     * @returns The desired state array data.
     */
    private getDataset(
        data: HistoryUtils.ChannelData,
        desiredState,
        edgeId: string | null,
        userId: string | null,
    ): any[] {
        const prices = data["QuarterlyPrice"].map((val) =>
            TimeOfUseTariffUtils.formatPrice(Utils.multiplySafely(val, 1000)),
        );
        const states = data["StateMachine"]
            .map((val) => Utils.multiplySafely(val, 1000))
            .map((val) => {
                if (val === null) {
                    return null;
                } else if (val < 0.5) {
                    return 0; // DelayDischarge
                } else if (val > 8.5) {
                    return 9; // DischargeConsumption
                } else if (val > 7.5) {
                    return 8; // AvoidGridSellLimit
                } else if (val > 6.5) {
                    return 7; // LimitCharge
                } else if (val > 5.5) {
                    return 6; // DelayCharge
                } else if (val > 4.5) {
                    // TODO INTERSOLAR
                    if (userId === "intersolar@fenecon.de") {
                        if (edgeId === "fems17289") {
                            return 5; // PeakShaving
                        } else {
                            return 1; // Balancing
                        }
                    }
                    return 5; // PeakShaving
                } else if (val > 2.5) {
                    return 3; // ChargeGrid
                } else {
                    return 1; // Balancing
                }
            });
        const length = prices.length;
        const dataset = Array(length).fill(null);

        for (let index = 0; index < length; index++) {
            const quarterlyPrice = prices[index];
            const state = states[index];

            if (state !== null && state === desiredState) {
                dataset[index] = quarterlyPrice;
            }
        }

        return dataset;
    }

    /**
     * Returns the minimum value the chart should be scaled to.
     *
     * @param datasets The chart datasets.
     * @returns The minimum axis value.
     */
    private getMinimumAxisValue(datasets: Chart.ChartDataset[]): number {
        const labels = [
            this.translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.BALANCING"),
            this.translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.CHARGE_GRID"),
            this.translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.DELAY_DISCHARGE"),
            this.translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.PEAK_SHAVING"),
            this.translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.DELAY_CHARGE"),
            this.translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.LIMIT_CHARGE"),
            this.translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.AVOID_GRID_SELL_LIMIT"),
            this.translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.DISCHARGE_CONSUMPTION"),
        ];

        const finalArray: number[] = labels
            .map((label) => {
                const dataArray = datasets.find((dataset) => dataset.label === label)?.data as number[];
                return dataArray ? (dataArray.filter((price) => price !== null) as number[]) : [];
            })
            .reduce((acc, curr) => acc.concat(curr), []);

        if (finalArray.length === 0) {
            return 0;
        }

        const min = Math.floor(Math.min(...finalArray));
        return Math.floor(min - min * 0.05);
    }
}
