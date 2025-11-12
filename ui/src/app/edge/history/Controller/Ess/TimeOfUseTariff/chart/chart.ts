// @ts-strict-ignore
import { ChangeDetectorRef, Component, effect } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import * as Chart from "chart.js";
import { calculateResolution, ChronoUnit, Resolution } from "src/app/edge/history/shared";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/chart.constants";
import { NavigationService } from "src/app/shared/components/navigation/service/navigation.service";
import { ChannelAddress, Currency, EdgeConfig, Logger, Service, Websocket } from "src/app/shared/shared";
import { ColorUtils } from "src/app/shared/utils/color/color.utils";
import { ChartAxis, HistoryUtils, TimeOfUseTariffUtils, Utils, YAxisType } from "src/app/shared/utils/utils";

@Component({
    selector: "scheduleChart",
    templateUrl: "../../../../../../shared/components/chart/abstracthistorychart.html",
    standalone: false,
})
export class ChartComponent extends AbstractHistoryChart {

    private currencyUnit: Currency.Unit | null = null;
    private currencyLabel: Currency.Label; // Default

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

            edge.getFirstValidConfig(this.websocket).then(config => {
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
                return [{
                    name: this.translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.BALANCING"),
                    converter: () => this.getDataset(data, TimeOfUseTariffUtils.State.Balancing),
                    color: "rgb(51,102,0)",
                    stack: 1,
                    custom: {
                        formatNumber: ChartConstants.NumberFormat.TWO,
                    },
                    order: 2,
                },
                {
                    name: this.translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.CHARGE_GRID"),
                    converter: () => this.getDataset(data, TimeOfUseTariffUtils.State.ChargeGrid),
                    color: "rgb(0, 204, 204)",
                    stack: 1,
                    order: 2,
                },
                {
                    name: this.translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.DELAY_DISCHARGE"),
                    converter: () => this.getDataset(data, TimeOfUseTariffUtils.State.DelayDischarge),
                    color: "rgb(0,0,0)",
                    stack: 1,
                    order: 2,
                },
                {
                    name: this.translate.instant("GENERAL.SOC"),
                    converter: () => data["Soc"]?.map(value => Utils.multiplySafely(value, 1000)),
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
            yAxes: [{
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
        this.queryHistoricTimeseriesData(this.service.historyPeriod.value.from, this.service.historyPeriod.value.to, unit)
            .then((dataResponse) => {
                this.chartType = "line";
                this.chartObject = this.getChartData();

                const displayValues = AbstractHistoryChart.fillChart(this.chartType, this.chartObject, dataResponse);
                this.datasets = displayValues.datasets;
                this.legendOptions = displayValues.legendOptions;
                this.labels = displayValues.labels;
                this.setChartLabel();

                this.chartObject.yAxes.forEach((element) => {
                    this.options = AbstractHistoryChart.getYAxisOptions(this.options, element, this.translate, this.chartType, this.datasets, true, this.chartObject.tooltip.formatNumber,);
                });

                this.options.scales.x["time"].unit = calculateResolution(this.service, this.service.historyPeriod.value.from, this.service.historyPeriod.value.to).timeFormat;
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
     * @returns the desired state array data.
     */
    private getDataset(data: HistoryUtils.ChannelData, desiredState): any[] {
        const prices = data["QuarterlyPrice"]
            .map(val => TimeOfUseTariffUtils.formatPrice(Utils.multiplySafely(val, 1000)));
        const states = data["StateMachine"]
            .map(val => Utils.multiplySafely(val, 1000))
            .map(val => {
                if (val === null) {
                    return null;
                } else if (val < 0.5) {
                    return 0; // DelayDischarge
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
     * @returns the minumum axis value.
     */
    private getMinimumAxisValue(datasets: Chart.ChartDataset[]): number {

        const labels = [
            this.translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.BALANCING"),
            this.translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.CHARGE_GRID"),
            this.translate.instant("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.DELAY_DISCHARGE"),
        ];

        const finalArray: number[] = labels
            .map(label => {
                const dataArray = datasets.find(dataset => dataset.label === label)?.data as number[];
                return dataArray ? dataArray.filter(price => price !== null) as number[] : [];
            })
            .reduce((acc, curr) => acc.concat(curr), []);

        if (finalArray.length === 0) {
            return 0;
        }

        const min = Math.floor(Math.min(...finalArray));
        return Math.floor(min - (min * 0.05));
    }
}
