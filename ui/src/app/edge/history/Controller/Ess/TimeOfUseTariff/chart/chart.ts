// @ts-strict-ignore
import { ChangeDetectorRef, Component, effect } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import * as Chart from "CHART.JS";
import { calculateResolution, ChronoUnit, Resolution } from "src/app/edge/history/shared";
import { AbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/CHART.CONSTANTS";
import { ChannelAddress, Currency, EdgeConfig, Logger, Service, Websocket } from "src/app/shared/shared";
import { ColorUtils } from "src/app/shared/utils/color/COLOR.UTILS";
import { ChartAxis, HistoryUtils, TimeOfUseTariffUtils, Utils, YAxisType } from "src/app/shared/utils/utils";

@Component({
    selector: "scheduleChart",
    templateUrl: "../../../../../../shared/components/chart/ABSTRACTHISTORYCHART.HTML",
    standalone: false,
})
export class ChartComponent extends AbstractHistoryChart {

    private currencyUnit: CURRENCY.UNIT | null = null;
    private currencyLabel: CURRENCY.LABEL; // Default

    constructor(
        private websocket: Websocket,
        public override service: Service,
        public override cdRef: ChangeDetectorRef,
        protected override translate: TranslateService,
        protected override route: ActivatedRoute,
        protected override logger: Logger,
    ) {
        super(service, cdRef, translate, route, logger);
        effect(() => {
            const edge = THIS.SERVICE.CURRENT_EDGE();

            if (!edge) {
                return;
            }

            EDGE.GET_FIRST_VALID_CONFIG(THIS.WEBSOCKET).then(config => {
                const meta: EDGE_CONFIG.COMPONENT = config?.getComponent("_meta");
                const currency: string = config?.getPropertyFromComponent<string>(meta, "currency");
                THIS.CURRENCY_UNIT = CURRENCY.GET_CHART_CURRENCY_UNIT_LABEL(currency);
            });
        });
    }

    protected override getChartData(): HISTORY_UTILS.CHART_DATA {
        // Assigning the component to be able to use the id.
        const componentId: string = THIS.CONFIG.GET_COMPONENT_IDS_BY_FACTORY("CONTROLLER.ESS.TIME-Of-Use-Tariff")[0];
        THIS.COMPONENT = THIS.CONFIG.COMPONENTS[componentId];

        const meta: EDGE_CONFIG.COMPONENT = THIS.CONFIG?.getComponent("_meta");
        const currency: string = THIS.CONFIG?.getPropertyFromComponent<string>(meta, "currency");
        THIS.CURRENCY_LABEL = CURRENCY.GET_CURRENCY_LABEL_BY_CURRENCY(currency);
        THIS.CHART_TYPE = "bar";

        return {
            input: [
                {
                    name: "QuarterlyPrice",
                    powerChannel: CHANNEL_ADDRESS.FROM_STRING(THIS.COMPONENT.ID + "/QuarterlyPrices"),
                },
                {
                    name: "StateMachine",
                    powerChannel: CHANNEL_ADDRESS.FROM_STRING(THIS.COMPONENT.ID + "/StateMachine"),
                },
                {
                    name: "Soc",
                    powerChannel: CHANNEL_ADDRESS.FROM_STRING("_sum/EssSoc"),
                },
                {
                    name: "GridBuy",
                    powerChannel: CHANNEL_ADDRESS.FROM_STRING("_sum/GridActivePower"),
                    converter: HISTORY_UTILS.VALUE_CONVERTER.NEGATIVE_AS_ZERO,
                },
            ],
            output: (data: HISTORY_UTILS.CHANNEL_DATA) => {
                return [{
                    name: THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.BALANCING"),
                    converter: () => THIS.GET_DATASET(data, TIME_OF_USE_TARIFF_UTILS.STATE.BALANCING),
                    color: "rgb(51,102,0)",
                    stack: 1,
                    order: 2,
                },
                {
                    name: THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.CHARGE_GRID"),
                    converter: () => THIS.GET_DATASET(data, TIME_OF_USE_TARIFF_UTILS.STATE.CHARGE_GRID),
                    color: "rgb(0, 204, 204)",
                    stack: 1,
                    order: 2,
                },
                {
                    name: THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.DELAY_DISCHARGE"),
                    converter: () => THIS.GET_DATASET(data, TIME_OF_USE_TARIFF_UTILS.STATE.DELAY_DISCHARGE),
                    color: "rgb(0,0,0)",
                    stack: 1,
                    order: 2,
                },
                {
                    name: THIS.TRANSLATE.INSTANT("GENERAL.SOC"),
                    converter: () => data["Soc"]?.map(value => UTILS.MULTIPLY_SAFELY(value, 1000)),
                    color: "rgb(189, 195, 199)",
                    borderDash: [10, 10],
                    yAxisId: CHART_AXIS.RIGHT,
                    custom: {
                        type: "line",
                        unit: YAXIS_TYPE.PERCENTAGE,
                        formatNumber: "1.0-0",
                    },
                    order: 1,
                },
                {
                    name: THIS.TRANSLATE.INSTANT("GENERAL.GRID_BUY_ADVANCED"),
                    converter: () => data["GridBuy"],
                    color: CHART_CONSTANTS.COLORS.BLUE_GREY,
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
                formatNumber: "1.0-4",
            },
            yAxes: [{
                unit: YAXIS_TYPE.CURRENCY,
                position: "left",
                yAxisId: CHART_AXIS.LEFT,
                customTitle: CURRENCY.GET_CHART_CURRENCY_UNIT_LABEL(currency),
                scale: {
                    dynamicScale: true,
                },
            },
            {
                unit: YAXIS_TYPE.PERCENTAGE,
                position: "right",
                yAxisId: CHART_AXIS.RIGHT,
                displayGrid: false,
            },
            {
                unit: YAXIS_TYPE.POWER,
                position: "right",
                yAxisId: ChartAxis.RIGHT_2,
                displayGrid: false,
            },
            ],
        };
    }

    protected override async loadChart() {
        THIS.LABELS = [];
        THIS.ERROR_RESPONSE = null;

        const unit: Resolution = { unit: CHRONO_UNIT.TYPE.MINUTES, value: 15 };
        THIS.QUERY_HISTORIC_TIMESERIES_DATA(THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM, THIS.SERVICE.HISTORY_PERIOD.VALUE.TO, unit)
            .then((dataResponse) => {
                THIS.CHART_TYPE = "line";
                THIS.CHART_OBJECT = THIS.GET_CHART_DATA();

                const displayValues = ABSTRACT_HISTORY_CHART.FILL_CHART(THIS.CHART_TYPE, THIS.CHART_OBJECT, dataResponse);
                THIS.DATASETS = DISPLAY_VALUES.DATASETS;
                THIS.LEGEND_OPTIONS = DISPLAY_VALUES.LEGEND_OPTIONS;
                THIS.LABELS = DISPLAY_VALUES.LABELS;
                THIS.SET_CHART_LABEL();

                THIS.CHART_OBJECT.Y_AXES.FOR_EACH((element) => {
                    THIS.OPTIONS = ABSTRACT_HISTORY_CHART.GET_YAXIS_OPTIONS(THIS.OPTIONS, element, THIS.TRANSLATE, THIS.CHART_TYPE, THIS.DATASETS, true, THIS.CHART_OBJECT.TOOLTIP.FORMAT_NUMBER,);
                });

                THIS.OPTIONS.SCALES.X["time"].unit = calculateResolution(THIS.SERVICE, THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM, THIS.SERVICE.HISTORY_PERIOD.VALUE.TO).timeFormat;
                THIS.OPTIONS.SCALES.X.TICKS["source"] = "auto";
                THIS.OPTIONS.SCALES.X.GRID = { offset: false };
                THIS.OPTIONS.PLUGINS.TOOLTIP.MODE = "index";
                THIS.OPTIONS.SCALES.X.TICKS.MAX_TICKS_LIMIT = 30;
                THIS.OPTIONS.SCALES[CHART_AXIS.LEFT].min = THIS.GET_MINIMUM_AXIS_VALUE(THIS.DATASETS);

                THIS.OPTIONS.PLUGINS.TOOLTIP.CALLBACKS.LABEL_COLOR = (item: CHART.TOOLTIP_ITEM<any>) => {
                    return {
                        borderColor: COLOR_UTILS.CHANGE_OPACITY_FROM_RGBA(ITEM.DATASET.BORDER_COLOR, 1),
                        backgroundColor: ITEM.DATASET.BACKGROUND_COLOR,
                    };
                };
                THIS.OPTIONS.SCALES.X["bounds"] = "ticks";

                THIS.OPTIONS.PLUGINS.TOOLTIP.CALLBACKS.LABEL = (item: CHART.TOOLTIP_ITEM<any>) => {
                    const label = ITEM.DATASET.LABEL;
                    const value = ITEM.DATASET.DATA[ITEM.DATA_INDEX];

                    return TIME_OF_USE_TARIFF_UTILS.GET_LABEL(value, label, THIS.TRANSLATE, THIS.CURRENCY_LABEL);
                };

                THIS.OPTIONS.SCALES[CHART_AXIS.LEFT]["title"].text = THIS.CURRENCY_UNIT;
                THIS.DATASETS = THIS.DATASETS.MAP((el) => {
                    const opacity = EL.TYPE === "line" ? 0.2 : 0.5;

                    EL.BACKGROUND_COLOR = COLOR_UTILS.CHANGE_OPACITY_FROM_RGBA(EL.BACKGROUND_COLOR.TO_STRING(), opacity);
                    EL.BORDER_COLOR = COLOR_UTILS.CHANGE_OPACITY_FROM_RGBA(EL.BORDER_COLOR.TO_STRING(), 1);
                    return el;
                });

                THIS.OPTIONS.SCALES.X["offset"] = false;
                THIS.OPTIONS["animation"] = false;
            });
    }

    /**
     * Returns only the desired state data extracted from the whole dataset.
     *
     * @param data The historic data.
     * @param desiredState The desired state data from the whole dataset.
     * @returns the desired state array data.
     */
    private getDataset(data: HISTORY_UTILS.CHANNEL_DATA, desiredState): any[] {
        const prices = data["QuarterlyPrice"]
            .map(val => TIME_OF_USE_TARIFF_UTILS.FORMAT_PRICE(UTILS.MULTIPLY_SAFELY(val, 1000)));
        const states = data["StateMachine"]
            .map(val => UTILS.MULTIPLY_SAFELY(val, 1000))
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
        const length = PRICES.LENGTH;
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
    private getMinimumAxisValue(datasets: CHART.CHART_DATASET[]): number {

        const labels = [
            THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.BALANCING"),
            THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.CHARGE_GRID"),
            THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.TIME_OF_USE_TARIFF.STATE.DELAY_DISCHARGE"),
        ];

        const finalArray: number[] = labels
            .map(label => {
                const dataArray = DATASETS.FIND(dataset => DATASET.LABEL === label)?.data as number[];
                return dataArray ? DATA_ARRAY.FILTER(price => price !== null) as number[] : [];
            })
            .reduce((acc, curr) => ACC.CONCAT(curr), []);

        if (FINAL_ARRAY.LENGTH === 0) {
            return 0;
        }

        const min = MATH.FLOOR(MATH.MIN(...finalArray));
        return MATH.FLOOR(min - (min * 0.05));
    }
}
