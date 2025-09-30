// @ts-strict-ignore
import { Component, Input, OnChanges, OnDestroy, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import * as Chart from "CHART.JS";
import { AbstractHistoryChart } from "src/app/edge/history/abstracthistorychart";
import { AbstractHistoryChart as NewAbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/CHART.CONSTANTS";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from "src/app/shared/shared";
import { ColorUtils } from "src/app/shared/utils/color/COLOR.UTILS";
import { ChartAxis, HistoryUtils, TimeOfUseTariffUtils, Utils, YAxisType } from "src/app/shared/utils/utils";
import { GetScheduleRequest } from "../../../../../../shared/jsonrpc/request/getScheduleRequest";
import { GetScheduleResponse } from "../../../../../../shared/jsonrpc/response/getScheduleResponse";

@Component({
    selector: "powerSocChart",
    templateUrl: "../../../../../history/ABSTRACTHISTORYCHART.HTML",
    standalone: false,
})
export class SchedulePowerAndSocChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

    @Input({ required: true }) public refresh!: boolean;
    @Input({ required: true }) public override edge!: Edge;
    @Input({ required: true }) public component!: EDGE_CONFIG.COMPONENT;


    constructor(
        protected override service: Service,
        protected override translate: TranslateService,
        private route: ActivatedRoute,
        private websocket: Websocket,
    ) {
        super("powerSoc-chart", service, translate);
    }

    public ngOnChanges() {
        THIS.UPDATE_CHART();
    }

    public ngOnInit() {
        THIS.SERVICE.START_SPINNER(THIS.SPINNER_ID);
    }

    public ngOnDestroy() {
        THIS.UNSUBSCRIBE_CHART_REFRESH();
    }

    public getChartHeight(): number {
        return TIME_OF_USE_TARIFF_UTILS.GET_CHART_HEIGHT(THIS.SERVICE.IS_SMARTPHONE_RESOLUTION);
    }

    protected setLabel() {
        THIS.OPTIONS = THIS.CREATE_DEFAULT_CHART_OPTIONS();
        const translate = THIS.TRANSLATE;
        THIS.OPTIONS.PLUGINS = {
            tooltip: {
                callbacks: {
                    label: function (item: CHART.TOOLTIP_ITEM<any>) {

                        const label = ITEM.DATASET.LABEL;
                        const value = ITEM.DATASET.DATA[ITEM.DATA_INDEX];

                        return TIME_OF_USE_TARIFF_UTILS.GET_LABEL(value, label, translate);
                    },
                },
            },
        };
    }

    protected getChannelAddresses(): Promise<ChannelAddress[]> {
        return new Promise(() => { []; });
    }

    protected override updateChart() {

        THIS.AUTO_SUBSCRIBE_CHART_REFRESH();
        THIS.SERVICE.START_SPINNER(THIS.SPINNER_ID);
        THIS.LOADING = true;

        THIS.EDGE.SEND_REQUEST(
            THIS.WEBSOCKET,
            new ComponentJsonApiRequest({ componentId: THIS.COMPONENT.ID, payload: new GetScheduleRequest() }),
        ).then(response => {
            const result = (response as GetScheduleResponse).result;
            const schedule = RESULT.SCHEDULE;
            const datasets = [];

            // Extracting prices and states from the schedule array
            const { gridBuyArray, gridSellArray, productionArray, consumptionArray, essDischargeArray, essChargeArray, socArray, labels } = {
                gridBuyArray: SCHEDULE.MAP(entry => HISTORY_UTILS.VALUE_CONVERTER.NEGATIVE_AS_ZERO(ENTRY.GRID), 1000),
                gridSellArray: SCHEDULE.MAP(entry => HISTORY_UTILS.VALUE_CONVERTER.POSITIVE_AS_ZERO_AND_INVERT_NEGATIVE(ENTRY.GRID), 1000),
                productionArray: SCHEDULE.MAP(entry => ENTRY.PRODUCTION, 1000),
                consumptionArray: SCHEDULE.MAP(entry => ENTRY.CONSUMPTION, 1000),
                essDischargeArray: SCHEDULE.MAP(entry => HISTORY_UTILS.VALUE_CONVERTER.NEGATIVE_AS_ZERO(ENTRY.ESS), 1000),
                essChargeArray: SCHEDULE.MAP(entry => HISTORY_UTILS.VALUE_CONVERTER.POSITIVE_AS_ZERO_AND_INVERT_NEGATIVE(ENTRY.ESS), 1000),
                socArray: SCHEDULE.MAP(entry => ENTRY.SOC),
                labels: SCHEDULE.MAP(entry => new Date(ENTRY.TIMESTAMP)),
            };

            DATASETS.PUSH({
                type: "line",
                label: THIS.TRANSLATE.INSTANT("GENERAL.GRID_BUY"),
                data: GRID_BUY_ARRAY.MAP(v => UTILS.DIVIDE_SAFELY(v, 1000)), // [W] to [kW]
                hidden: true,
                order: 1,
            });
            THIS.COLORS.PUSH({
                backgroundColor: COLOR_UTILS.RGB_STRING_TO_RGBA(CHART_CONSTANTS.COLORS.BLUE_GREY, 0.2),
                borderColor: CHART_CONSTANTS.COLORS.BLUE_GREY,
            });

            DATASETS.PUSH({
                type: "line",
                label: THIS.TRANSLATE.INSTANT("GENERAL.GRID_SELL"),
                data: GRID_SELL_ARRAY.MAP(v => UTILS.DIVIDE_SAFELY(v, 1000)), // [W] to [kW]
                hidden: true,
                order: 1,
            });
            THIS.COLORS.PUSH({
                backgroundColor: COLOR_UTILS.RGB_STRING_TO_RGBA(CHART_CONSTANTS.COLORS.PURPLE, 0.2),
                borderColor: CHART_CONSTANTS.COLORS.PURPLE,
            });

            DATASETS.PUSH({
                type: "line",
                label: THIS.TRANSLATE.INSTANT("GENERAL.PRODUCTION"),
                data: PRODUCTION_ARRAY.MAP(v => UTILS.DIVIDE_SAFELY(v, 1000)), // [W] to [kW]
                hidden: false,
                order: 1,
            });
            THIS.COLORS.PUSH({
                backgroundColor: COLOR_UTILS.RGB_STRING_TO_RGBA(CHART_CONSTANTS.COLORS.BLUE, 0.2),
                borderColor: CHART_CONSTANTS.COLORS.BLUE,
            });

            DATASETS.PUSH({
                type: "line",
                label: THIS.TRANSLATE.INSTANT("GENERAL.CONSUMPTION"),
                data: CONSUMPTION_ARRAY.MAP(v => UTILS.DIVIDE_SAFELY(v, 1000)), // [W] to [kW]
                hidden: false,
                order: 1,
            });
            THIS.COLORS.PUSH({
                backgroundColor: COLOR_UTILS.RGB_STRING_TO_RGBA(CHART_CONSTANTS.COLORS.YELLOW, 0.2),
                borderColor: CHART_CONSTANTS.COLORS.YELLOW,
            });

            DATASETS.PUSH({
                type: "line",
                label: THIS.TRANSLATE.INSTANT("GENERAL.CHARGE"),
                data: ESS_CHARGE_ARRAY.MAP(v => UTILS.DIVIDE_SAFELY(v, 1000)), // [W] to [kW]
                hidden: true,
                order: 1,
                unit: YAXIS_TYPE.POWER,
            });
            THIS.COLORS.PUSH({
                backgroundColor: COLOR_UTILS.RGB_STRING_TO_RGBA(CHART_CONSTANTS.COLORS.GREEN, 0.2),
                borderColor: CHART_CONSTANTS.COLORS.GREEN,
            });

            DATASETS.PUSH({
                type: "line",
                label: THIS.TRANSLATE.INSTANT("GENERAL.DISCHARGE"),
                data: ESS_DISCHARGE_ARRAY.MAP(v => UTILS.DIVIDE_SAFELY(v, 1000)), // [W] to [kW]
                hidden: true,
                order: 1,
                unit: YAXIS_TYPE.POWER,
            });
            THIS.COLORS.PUSH({
                backgroundColor: COLOR_UTILS.RGB_STRING_TO_RGBA(CHART_CONSTANTS.COLORS.RED, 0.2),
                borderColor: CHART_CONSTANTS.COLORS.RED,
            });

            // State of charge data
            DATASETS.PUSH({
                type: "line",
                label: THIS.TRANSLATE.INSTANT("GENERAL.SOC"),
                data: socArray,
                hidden: false,
                yAxisID: CHART_AXIS.RIGHT,
                borderDash: [10, 10],
                order: 1,
                unit: YAXIS_TYPE.PERCENTAGE,
            });
            THIS.COLORS.PUSH({
                backgroundColor: "rgba(189, 195, 199,0.2)",
                borderColor: "rgba(189, 195, 199,1)",
            });

            THIS.DATASETS = datasets;
            THIS.LOADING = false;
            THIS.LABELS = labels;
            THIS.SET_LABEL();
            THIS.STOP_SPINNER();
        }).catch((reason) => {
            CONSOLE.ERROR(reason);
            THIS.INITIALIZE_CHART();
            return;
        }).finally(async () => {
            await THIS.SET_OPTIONS(THIS.OPTIONS);
            THIS.APPLY_CONTROLLER_SPECIFIC_OPTIONS();
        });
    }

    private applyControllerSpecificOptions() {
        const rightYAxis: HISTORY_UTILS.Y_AXES = { position: "right", unit: YAXIS_TYPE.PERCENTAGE, yAxisId: CHART_AXIS.RIGHT };
        const leftYAxis: HISTORY_UTILS.Y_AXES = { position: "left", unit: YAXIS_TYPE.POWER, yAxisId: CHART_AXIS.LEFT };

        THIS.OPTIONS = NEW_ABSTRACT_HISTORY_CHART.GET_YAXIS_OPTIONS(THIS.OPTIONS, rightYAxis, THIS.TRANSLATE, "line", ChartConstants.EMPTY_DATASETS, true);
        THIS.OPTIONS = NEW_ABSTRACT_HISTORY_CHART.GET_YAXIS_OPTIONS(THIS.OPTIONS, leftYAxis, THIS.TRANSLATE, "line", ChartConstants.EMPTY_DATASETS, true);

        THIS.DATASETS = THIS.DATASETS.MAP((el: CHART.CHART_DATASET) => {

            // align particular dataset element to right yAxis
            if (EL.LABEL === THIS.TRANSLATE.INSTANT("GENERAL.SOC")) {
                el["yAxisID"] = CHART_AXIS.RIGHT;
            }
            return el;
        });

        THIS.OPTIONS.SCALES.X["ticks"] = { source: "auto", autoSkip: false };
        THIS.OPTIONS.SCALES.X.TICKS.COLOR = getComputedStyle(DOCUMENT.DOCUMENT_ELEMENT).getPropertyValue("--ion-color-chart-xAxis-ticks");
        THIS.OPTIONS.SCALES.X.TICKS.CALLBACK = function (value, index, values) {
            const date = new Date(value);

            // Display the label only if the minutes are zero (full hour)
            return DATE.GET_MINUTES() === 0 ? DATE.GET_HOURS() + ":00" : "";
        };

        THIS.OPTIONS.SCALES[CHART_AXIS.RIGHT].GRID.DISPLAY = false;
        THIS.OPTIONS.SCALES[CHART_AXIS.LEFT].suggestedMin = 0;
        THIS.OPTIONS.SCALES[CHART_AXIS.LEFT].suggestedMax = 1;
    }

}
