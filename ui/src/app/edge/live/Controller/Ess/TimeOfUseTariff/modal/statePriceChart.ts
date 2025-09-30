// @ts-strict-ignore
import { Component, Input, OnChanges, OnDestroy, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import * as Chart from "CHART.JS";
import { filter, take } from "rxjs/operators";
import { AbstractHistoryChart } from "src/app/edge/history/abstracthistorychart";
import { calculateResolution } from "src/app/edge/history/shared";
import { AbstractHistoryChart as NewAbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/CHART.CONSTANTS";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { ChannelAddress, Currency, Edge, EdgeConfig, Service, Websocket } from "src/app/shared/shared";
import { ColorUtils } from "src/app/shared/utils/color/COLOR.UTILS";
import { ChartAxis, HistoryUtils, TimeOfUseTariffUtils, YAxisType } from "src/app/shared/utils/utils";
import { GetScheduleRequest } from "../../../../../../shared/jsonrpc/request/getScheduleRequest";
import { GetScheduleResponse } from "../../../../../../shared/jsonrpc/response/getScheduleResponse";
import { Controller_Ess_TimeOfUseTariffUtils } from "../utils";

@Component({
    selector: "statePriceChart",
    templateUrl: "../../../../../history/ABSTRACTHISTORYCHART.HTML",
    standalone: false,
})
export class ScheduleStateAndPriceChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

    @Input({ required: true }) public refresh!: boolean;
    @Input({ required: true }) public override edge!: Edge;
    @Input({ required: true }) public component!: EDGE_CONFIG.COMPONENT;

    private currencyLabel: CURRENCY.LABEL; // Default
    private currencyUnit: CURRENCY.UNIT; // Default

    constructor(
        protected override service: Service,
        protected override translate: TranslateService,
        private route: ActivatedRoute,
        private websocket: Websocket,
    ) {
        super("schedule-chart", service, translate);
    }

    public getChartHeight(): number {
        return TIME_OF_USE_TARIFF_UTILS.GET_CHART_HEIGHT(THIS.SERVICE.IS_SMARTPHONE_RESOLUTION);
    }

    public async ngOnChanges() {
        THIS.EDGE.GET_CONFIG(THIS.WEBSOCKET).pipe(filter(config => !!config), take(1)).subscribe(config => {
            const meta: EDGE_CONFIG.COMPONENT = config?.getComponent("_meta");
            const currency: string = config?.getPropertyFromComponent<string>(meta, "currency");
            THIS.CURRENCY_LABEL = CURRENCY.GET_CURRENCY_LABEL_BY_CURRENCY(currency);
            THIS.CURRENCY_UNIT = CURRENCY.GET_CHART_CURRENCY_UNIT_LABEL(currency);
        });
        THIS.UPDATE_CHART();
    }

    public ngOnInit() {
        THIS.SERVICE.START_SPINNER(THIS.SPINNER_ID);
    }

    public ngOnDestroy() {
        THIS.UNSUBSCRIBE_CHART_REFRESH();
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

            // Extracting prices, states, timestamps from the schedule array
            const { priceArray, stateArray, timestampArray, gridBuyArray, socArray } = {
                priceArray: SCHEDULE.MAP(entry => ENTRY.PRICE),
                stateArray: SCHEDULE.MAP(entry => ENTRY.STATE),
                timestampArray: SCHEDULE.MAP(entry => ENTRY.TIMESTAMP),
                gridBuyArray: SCHEDULE.MAP(entry => HISTORY_UTILS.VALUE_CONVERTER.NEGATIVE_AS_ZERO(ENTRY.GRID)),
                socArray: SCHEDULE.MAP(entry => ENTRY.SOC),
            };

            const scheduleChartData = Controller_Ess_TimeOfUseTariffUtils.getScheduleChartData(SCHEDULE.LENGTH, priceArray,
                stateArray, timestampArray, gridBuyArray, socArray, THIS.TRANSLATE, THIS.COMPONENT.PROPERTIES.CONTROL_MODE);

            THIS.COLORS = SCHEDULE_CHART_DATA.COLORS;
            THIS.LABELS = SCHEDULE_CHART_DATA.LABELS;

            THIS.DATASETS = SCHEDULE_CHART_DATA.DATASETS;
            THIS.LOADING = false;
            THIS.SET_LABEL();
            THIS.STOP_SPINNER();

        }).catch((reason) => {
            CONSOLE.ERROR(reason);
            THIS.INITIALIZE_CHART();
            return;

        }).finally(async () => {
            THIS.UNIT = YAXIS_TYPE.CURRENCY;
            await THIS.SET_OPTIONS(THIS.OPTIONS);
            THIS.APPLY_CONTROLLER_SPECIFIC_OPTIONS();
        });
    }

    protected setLabel() {
        THIS.OPTIONS = THIS.CREATE_DEFAULT_CHART_OPTIONS();
    }

    protected getChannelAddresses(): Promise<ChannelAddress[]> {
        return new Promise(() => { []; });
    }

    private applyControllerSpecificOptions() {
        const rightYaxisSoc: HISTORY_UTILS.Y_AXES = { position: "right", unit: YAXIS_TYPE.PERCENTAGE, yAxisId: CHART_AXIS.RIGHT, displayGrid: true };
        THIS.OPTIONS = NEW_ABSTRACT_HISTORY_CHART.GET_YAXIS_OPTIONS(THIS.OPTIONS, rightYaxisSoc, THIS.TRANSLATE, "line", THIS.DATASETS, true);

        const rightYAxisPower: HISTORY_UTILS.Y_AXES = { position: "right", unit: YAXIS_TYPE.POWER, yAxisId: ChartAxis.RIGHT_2 };
        THIS.OPTIONS = NEW_ABSTRACT_HISTORY_CHART.GET_YAXIS_OPTIONS(THIS.OPTIONS, rightYAxisPower, THIS.TRANSLATE, "line", THIS.DATASETS, true);

        THIS.OPTIONS.SCALES.X["time"].unit = calculateResolution(THIS.SERVICE, THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM, THIS.SERVICE.HISTORY_PERIOD.VALUE.TO).timeFormat;
        THIS.OPTIONS.SCALES.X["ticks"] = { source: "auto", autoSkip: false };
        THIS.OPTIONS.SCALES.X.TICKS.COLOR = getComputedStyle(DOCUMENT.DOCUMENT_ELEMENT).getPropertyValue("--ion-color-chart-xAxis-ticks");
        THIS.OPTIONS.SCALES.X.TICKS.MAX_TICKS_LIMIT = 30;
        THIS.OPTIONS.SCALES.X["offset"] = false;
        THIS.OPTIONS.SCALES.X.TICKS.CALLBACK = function (value) {
            const date = new Date(value);

            // Display the label only if the minutes are zero (full hour)
            return DATE.GET_MINUTES() === 0 ? DATE.GET_HOURS() + ":00" : "";
        };

        // OPTIONS.PLUGINS.
        THIS.OPTIONS.PLUGINS.TOOLTIP.MODE = "index";
        THIS.OPTIONS.PLUGINS.TOOLTIP.CALLBACKS.LABEL_COLOR = (item: CHART.TOOLTIP_ITEM<any>) => {
            if (!item) {
                return;
            }
            return {
                borderColor: COLOR_UTILS.CHANGE_OPACITY_FROM_RGBA(ITEM.DATASET.BORDER_COLOR, 1),
                backgroundColor: ITEM.DATASET.BACKGROUND_COLOR,
            };
        };

        THIS.OPTIONS.PLUGINS.TOOLTIP.CALLBACKS.LABEL = (item: CHART.TOOLTIP_ITEM<any>) => {

            const label = ITEM.DATASET.LABEL;
            const value = ITEM.DATASET.DATA[ITEM.DATA_INDEX];

            return TIME_OF_USE_TARIFF_UTILS.GET_LABEL(value, label, THIS.TRANSLATE, THIS.CURRENCY_LABEL);
        };

        THIS.DATASETS = THIS.DATASETS.MAP((el) => {
            const opacity = EL.TYPE === "line" ? 0.2 : 0.5;

            if (EL.BACKGROUND_COLOR && EL.BORDER_COLOR) {
                EL.BACKGROUND_COLOR = COLOR_UTILS.CHANGE_OPACITY_FROM_RGBA(EL.BACKGROUND_COLOR.TO_STRING(), opacity);
                EL.BORDER_COLOR = COLOR_UTILS.CHANGE_OPACITY_FROM_RGBA(EL.BORDER_COLOR.TO_STRING(), 1);
            }
            return el;
        });

        THIS.DATASETS = THIS.DATASETS.MAP((el: CHART.CHART_DATASET) => {

            // align particular dataset element to right yAxis
            if (EL.LABEL == THIS.TRANSLATE.INSTANT("GENERAL.GRID_BUY_ADVANCED")) {
                el["yAxisID"] = ChartAxis.RIGHT_2;
            } else if (EL.LABEL == THIS.TRANSLATE.INSTANT("GENERAL.SOC")) {
                el["yAxisID"] = CHART_AXIS.RIGHT;
            }

            return el;
        });
        const leftYAxis: HISTORY_UTILS.Y_AXES = { position: "left", unit: THIS.UNIT, yAxisId: CHART_AXIS.LEFT, customTitle: THIS.CURRENCY_UNIT, scale: { dynamicScale: true } };
        [rightYaxisSoc, rightYAxisPower].forEach((element) => {
            THIS.OPTIONS = NEW_ABSTRACT_HISTORY_CHART.GET_YAXIS_OPTIONS(THIS.OPTIONS, element, THIS.TRANSLATE, "line", THIS.DATASETS, true);
        });

        THIS.OPTIONS.SCALES[CHART_AXIS.LEFT] = {
            ...THIS.OPTIONS.SCALES[CHART_AXIS.LEFT],
            ...ChartConstants.DEFAULT_Y_SCALE_OPTIONS(leftYAxis, THIS.TRANSLATE, "bar", THIS.DATASETS.FILTER(el => el["yAxisID"] === CHART_AXIS.LEFT), true),
        };
        THIS.OPTIONS.SCALES[CHART_AXIS.RIGHT].GRID.DISPLAY = false;
        THIS.OPTIONS.SCALES[ChartAxis.RIGHT_2].suggestedMin = 0;
        THIS.OPTIONS.SCALES[ChartAxis.RIGHT_2].suggestedMax = 1;
        THIS.OPTIONS.SCALES[ChartAxis.RIGHT_2].GRID.DISPLAY = false;
        THIS.OPTIONS["animation"] = false;
    }
}
