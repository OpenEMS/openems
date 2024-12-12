// @ts-strict-ignore
import { Component, Input, OnChanges, OnDestroy, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import * as Chart from "chart.js";
import { filter, take } from "rxjs/operators";
import { AbstractHistoryChart } from "src/app/edge/history/abstracthistorychart";
import { calculateResolution } from "src/app/edge/history/shared";
import { AbstractHistoryChart as NewAbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/chart.constants";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { ChartAxis, HistoryUtils, TimeOfUseTariffUtils, YAxisType } from "src/app/shared/service/utils";
import { ChannelAddress, Currency, Edge, EdgeConfig, Service, Websocket } from "src/app/shared/shared";
import { ColorUtils } from "src/app/shared/utils/color/color.utils";
import { GetScheduleRequest } from "../../../../../../shared/jsonrpc/request/getScheduleRequest";
import { GetScheduleResponse } from "../../../../../../shared/jsonrpc/response/getScheduleResponse";
import { Controller_Ess_TimeOfUseTariff } from "../Ess_TimeOfUseTariff";

@Component({
    selector: "statePriceChart",
    templateUrl: "../../../../../history/abstracthistorychart.html",
})
export class ScheduleStateAndPriceChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

    @Input({ required: true }) public refresh!: boolean;
    @Input({ required: true }) public override edge!: Edge;
    @Input({ required: true }) public component!: EdgeConfig.Component;

    private currencyLabel: Currency.Label; // Default
    private currencyUnit: Currency.Unit; // Default

    constructor(
        protected override service: Service,
        protected override translate: TranslateService,
        private route: ActivatedRoute,
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
            const { priceArray, stateArray, timestampArray, gridBuyArray, socArray } = {
                priceArray: schedule.map(entry => entry.price),
                stateArray: schedule.map(entry => entry.state),
                timestampArray: schedule.map(entry => entry.timestamp),
                gridBuyArray: schedule.map(entry => HistoryUtils.ValueConverter.NEGATIVE_AS_ZERO(entry.grid)),
                socArray: schedule.map(entry => entry.soc),
            };

            const scheduleChartData = Controller_Ess_TimeOfUseTariff.getScheduleChartData(schedule.length, priceArray,
                stateArray, timestampArray, gridBuyArray, socArray, this.translate, this.component.properties.controlMode);

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
        const locale = this.service.translate.currentLang;
        const rightYaxisSoc: HistoryUtils.yAxes = { position: "right", unit: YAxisType.PERCENTAGE, yAxisId: ChartAxis.RIGHT, displayGrid: true };
        this.options = NewAbstractHistoryChart.getYAxisOptions(this.options, rightYaxisSoc, this.translate, "line", locale, this.datasets, true);

        const rightYAxisPower: HistoryUtils.yAxes = { position: "right", unit: YAxisType.POWER, yAxisId: ChartAxis.RIGHT_2 };
        this.options = NewAbstractHistoryChart.getYAxisOptions(this.options, rightYAxisPower, this.translate, "line", locale, this.datasets, true);

        this.options.scales.x["time"].unit = calculateResolution(this.service, this.service.historyPeriod.value.from, this.service.historyPeriod.value.to).timeFormat;
        this.options.scales.x["ticks"] = { source: "auto", autoSkip: false };
        this.options.scales.x.ticks.color = getComputedStyle(document.documentElement).getPropertyValue("--ion-color-chart-xAxis-ticks");
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

        this.datasets = this.datasets.map((el: Chart.ChartDataset) => {

            // align particular dataset element to right yAxis
            if (el.label == this.translate.instant("General.gridBuy")) {
                el["yAxisID"] = ChartAxis.RIGHT_2;
            } else if (el.label == this.translate.instant("General.soc")) {
                el["yAxisID"] = ChartAxis.RIGHT;
            }

            return el;
        });
        const leftYAxis: HistoryUtils.yAxes = { position: "left", unit: this.unit, yAxisId: ChartAxis.LEFT, customTitle: this.currencyUnit, scale: { dynamicScale: true } };
        [rightYaxisSoc, rightYAxisPower].forEach((element) => {
            this.options = NewAbstractHistoryChart.getYAxisOptions(this.options, element, this.translate, "line", locale, this.datasets, true);
        });

        this.options.scales[ChartAxis.LEFT] = {
            ...this.options.scales[ChartAxis.LEFT],
            ...ChartConstants.DEFAULT_Y_SCALE_OPTIONS(leftYAxis, this.translate, "line", this.datasets.filter(el => el["yAxisID"] === ChartAxis.LEFT), true),
        };
        this.options.scales[ChartAxis.RIGHT].grid.display = false;
        this.options.scales[ChartAxis.RIGHT_2].suggestedMin = 0;
        this.options.scales[ChartAxis.RIGHT_2].suggestedMax = 1;
        this.options.scales[ChartAxis.RIGHT_2].grid.display = false;
        this.options["animation"] = false;
    }
}
