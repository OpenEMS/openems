// @ts-strict-ignore
import { Component, Input, OnChanges, OnDestroy, OnInit } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import * as Chart from "chart.js";
import { filter, take } from "rxjs/operators";
import { AbstractHistoryChart } from "src/app/edge/history/abstracthistorychart";
import { calculateResolution } from "src/app/edge/history/shared";
import { ChartConstants } from "src/app/shared/components/chart/chart.constants";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { ChartAxis, HistoryUtils, TimeOfUseTariffUtils, YAxisType } from "src/app/shared/service/utils";
import { ChannelAddress, Currency, Edge, EdgeConfig, Service, Websocket } from "src/app/shared/shared";
import { ColorUtils } from "src/app/shared/utils/color/color.utils";
import { Controller_Evse_Single } from "../EvseSingle";
import { GetScheduleRequest } from "../jsonrpc/getScheduleRequest";
import { GetScheduleResponse } from "../jsonrpc/getScheduleResponse";

@Component({
    selector: "scheduleChart",
    templateUrl: "../../../../history/abstracthistorychart.html",
    standalone: false,
})
export class ScheduleChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

    @Input({ required: true }) public refresh!: boolean;
    @Input({ required: true }) public override edge!: Edge;
    @Input({ required: true }) public component!: EdgeConfig.Component;

    private currencyLabel: Currency.Label; // Default
    private currencyUnit: Currency.Unit; // Default

    constructor(
        protected override service: Service,
        protected override translate: TranslateService,
        private websocket: Websocket,
    ) {
        super("schedule-chart", service, translate);
    }

    public getChartHeight(): number {
        return TimeOfUseTariffUtils.getChartHeight(this.service.isSmartphoneResolution);
    }

    public async ngOnChanges() {
        this.edge.getConfig(this.websocket).pipe(filter(config => !!config), take(1)).subscribe(config => {
            const meta: EdgeConfig.Component = config?.getComponent("_meta");
            const currency: string = config?.getPropertyFromComponent<string>(meta, "currency");
            this.currencyLabel = Currency.getCurrencyLabelByCurrency(currency);
            this.currencyUnit = Currency.getChartCurrencyUnitLabel(currency);
        });
        this.updateChart();
    }

    public ngOnInit() {
        this.service.startSpinner(this.spinnerId);
    }

    public ngOnDestroy() {
        this.unsubscribeChartRefresh();
    }

    protected override updateChart() {
        this.autoSubscribeChartRefresh();
        this.service.startSpinner(this.spinnerId);
        this.loading = true;

        this.edge.sendRequest(
            this.websocket,
            new ComponentJsonApiRequest({ componentId: this.component.id, payload: new GetScheduleRequest() }),
        ).then(response => {
            const result = (response as GetScheduleResponse).result;
            const schedule = result.schedule;

            // Extracting prices, states, timestamps from the schedule array
            const { priceArray, modeArray, timestampArray } = {
                priceArray: schedule.map(entry => entry.price),
                modeArray: schedule.map(entry => entry.mode),
                timestampArray: schedule.map(entry => entry.timestamp),
            };

            const scheduleChartData = Controller_Evse_Single.getScheduleChartData(schedule.length, priceArray,
                modeArray, timestampArray, this.translate);

            this.colors = scheduleChartData.colors;
            this.labels = scheduleChartData.labels;

            this.datasets = scheduleChartData.datasets;
            this.loading = false;
            this.setLabel();
            this.stopSpinner();

        }).catch((reason) => {
            console.error(reason);
            this.initializeChart();
            return;

        }).finally(async () => {
            this.unit = YAxisType.CURRENCY;
            await this.setOptions(this.options);
            this.applyControllerSpecificOptions();
        });
    }

    protected setLabel() {
        this.options = this.createDefaultChartOptions();
    }

    protected getChannelAddresses(): Promise<ChannelAddress[]> {
        return new Promise(() => { []; });
    }

    private applyControllerSpecificOptions() {

        this.options.scales.x["time"].unit = calculateResolution(this.service, this.service.historyPeriod.value.from, this.service.historyPeriod.value.to).timeFormat;
        this.options.scales.x["ticks"] = { source: "auto", autoSkip: false };
        this.options.scales.x.ticks.maxTicksLimit = 30;
        this.options.scales.x["offset"] = false;
        this.options.scales.x.ticks.callback = function (value) {
            const date = new Date(value);

            // Display the label only if the minutes are zero (full hour)
            return date.getMinutes() === 0 ? date.getHours() + ":00" : "";
        };

        // options.plugins.
        this.options.plugins.tooltip.mode = "index";
        this.options.plugins.tooltip.callbacks.labelColor = (item: Chart.TooltipItem<any>) => {
            if (!item) {
                return;
            }
            return {
                borderColor: ColorUtils.changeOpacityFromRGBA(item.dataset.borderColor, 1),
                backgroundColor: item.dataset.backgroundColor,
            };
        };

        this.options.plugins.tooltip.callbacks.label = (item: Chart.TooltipItem<any>) => {

            const label = item.dataset.label;
            const value = item.dataset.data[item.dataIndex];

            return TimeOfUseTariffUtils.getLabel(value, label, this.translate, this.currencyLabel);
        };

        this.datasets = this.datasets.map((el) => {
            const opacity = el.type === "line" ? 0.2 : 0.5;

            if (el.backgroundColor && el.borderColor) {
                el.backgroundColor = ColorUtils.changeOpacityFromRGBA(el.backgroundColor.toString(), opacity);
                el.borderColor = ColorUtils.changeOpacityFromRGBA(el.borderColor.toString(), 1);
            }
            return el;
        });

        const leftYAxis: HistoryUtils.yAxes = { position: "left", unit: this.unit, yAxisId: ChartAxis.LEFT, customTitle: this.currencyUnit, scale: { dynamicScale: true } };

        this.options.scales[ChartAxis.LEFT] = {
            ...this.options.scales[ChartAxis.LEFT],
            ...ChartConstants.DEFAULT_Y_SCALE_OPTIONS(leftYAxis, this.translate, "bar", this.datasets.filter(el => el["yAxisID"] === ChartAxis.LEFT), true),
        };
    }
}
