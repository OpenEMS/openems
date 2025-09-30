// @ts-strict-ignore
import { Component, Input, OnChanges, OnDestroy, OnInit } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import * as Chart from "CHART.JS";
import { filter, take } from "rxjs/operators";
import { AbstractHistoryChart } from "src/app/edge/history/abstracthistorychart";
import { calculateResolution } from "src/app/edge/history/shared";
import { ChartConstants } from "src/app/shared/components/chart/CHART.CONSTANTS";
import { Formatter } from "src/app/shared/components/shared/formatter";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { ChannelAddress, Currency, Edge, EdgeConfig, Service, Websocket } from "src/app/shared/shared";
import { ColorUtils } from "src/app/shared/utils/color/COLOR.UTILS";
import { ChartAxis, HistoryUtils, TimeOfUseTariffUtils, YAxisType } from "src/app/shared/utils/utils";
import { GetScheduleRequest } from "../../../jsonrpc/getScheduleRequest";
import { GetScheduleResponse } from "../../../jsonrpc/getScheduleResponse";
import { ControllerEvseSingleShared } from "../../../shared/shared";

@Component({
    selector: "scheduleChart",
    templateUrl: "../../../../../../history/ABSTRACTHISTORYCHART.HTML",
    standalone: false,
})
export class ScheduleChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

    @Input({ required: true }) public refresh!: boolean;
    @Input({ required: true }) public override edge!: Edge;
    @Input({ required: true }) public component!: EDGE_CONFIG.COMPONENT;

    private currencyLabel: CURRENCY.LABEL; // Default
    private currencyUnit: CURRENCY.UNIT; // Default

    constructor(
        protected override service: Service,
        protected override translate: TranslateService,
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
            const { priceArray, modeArray, timestampArray } = {
                priceArray: SCHEDULE.MAP(entry => ENTRY.PRICE),
                modeArray: SCHEDULE.MAP(entry => ENTRY.MODE),
                timestampArray: SCHEDULE.MAP(entry => ENTRY.TIMESTAMP),
            };

            const scheduleChartData = CONTROLLER_EVSE_SINGLE_SHARED.GET_SCHEDULE_CHART_DATA(SCHEDULE.LENGTH, priceArray,
                modeArray, timestampArray, THIS.TRANSLATE);

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

        THIS.OPTIONS.SCALES.X["time"].unit = calculateResolution(THIS.SERVICE, THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM, THIS.SERVICE.HISTORY_PERIOD.VALUE.TO).timeFormat;
        THIS.OPTIONS.SCALES.X["ticks"] = { source: "auto", autoSkip: false };
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

            return label + ": " + Formatter.FORMAT_CURRENCY_PER_KWH(value, THIS.CURRENCY_LABEL);
        };

        THIS.DATASETS = THIS.DATASETS.MAP((el) => {
            const opacity = EL.TYPE === "line" ? 0.2 : 0.5;

            if (EL.BACKGROUND_COLOR && EL.BORDER_COLOR) {
                EL.BACKGROUND_COLOR = COLOR_UTILS.CHANGE_OPACITY_FROM_RGBA(EL.BACKGROUND_COLOR.TO_STRING(), opacity);
                EL.BORDER_COLOR = COLOR_UTILS.CHANGE_OPACITY_FROM_RGBA(EL.BORDER_COLOR.TO_STRING(), 1);
            }
            return el;
        });

        const leftYAxis: HISTORY_UTILS.Y_AXES = { position: "left", unit: THIS.UNIT, yAxisId: CHART_AXIS.LEFT, customTitle: THIS.CURRENCY_UNIT, scale: { dynamicScale: true } };

        THIS.OPTIONS.SCALES[CHART_AXIS.LEFT] = {
            ...THIS.OPTIONS.SCALES[CHART_AXIS.LEFT],
            ...ChartConstants.DEFAULT_Y_SCALE_OPTIONS(leftYAxis, THIS.TRANSLATE, "bar", THIS.DATASETS.FILTER(el => el["yAxisID"] === CHART_AXIS.LEFT), true),
        };
    }
}
