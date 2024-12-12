// @ts-strict-ignore
import { Component, Input, OnChanges, OnDestroy, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import * as Chart from "chart.js";
import { AbstractHistoryChart } from "src/app/edge/history/abstracthistorychart";
import { AbstractHistoryChart as NewAbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/chart.constants";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { ChartAxis, HistoryUtils, TimeOfUseTariffUtils, Utils, YAxisType } from "src/app/shared/service/utils";
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from "src/app/shared/shared";
import { ColorUtils } from "src/app/shared/utils/color/color.utils";
import { GetScheduleRequest } from "../../../../../../shared/jsonrpc/request/getScheduleRequest";
import { GetScheduleResponse } from "../../../../../../shared/jsonrpc/response/getScheduleResponse";

@Component({
    selector: "powerSocChart",
    templateUrl: "../../../../../history/abstracthistorychart.html",
})
export class SchedulePowerAndSocChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

    @Input({ required: true }) public refresh!: boolean;
    @Input({ required: true }) public override edge!: Edge;
    @Input({ required: true }) public component!: EdgeConfig.Component;


    constructor(
        protected override service: Service,
        protected override translate: TranslateService,
        private route: ActivatedRoute,
        private websocket: Websocket,
    ) {
        super("powerSoc-chart", service, translate);
    }

    public ngOnChanges() {
        this.updateChart();
    }

    public ngOnInit() {
        this.service.startSpinner(this.spinnerId);
    }

    public ngOnDestroy() {
        this.unsubscribeChartRefresh();
    }

    public getChartHeight(): number {
        return TimeOfUseTariffUtils.getChartHeight(this.service.isSmartphoneResolution);
    }

    protected setLabel() {
        this.options = this.createDefaultChartOptions();
        const translate = this.translate;
        this.options.plugins = {
            tooltip: {
                callbacks: {
                    label: function (item: Chart.TooltipItem<any>) {

                        const label = item.dataset.label;
                        const value = item.dataset.data[item.dataIndex];

                        return TimeOfUseTariffUtils.getLabel(value, label, translate);
                    },
                },
            },
        };
    }

    protected getChannelAddresses(): Promise<ChannelAddress[]> {
        return new Promise(() => { []; });
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
            const datasets = [];

            // Extracting prices and states from the schedule array
            const { gridBuyArray, gridSellArray, productionArray, consumptionArray, essDischargeArray, essChargeArray, socArray, labels } = {
                gridBuyArray: schedule.map(entry => HistoryUtils.ValueConverter.NEGATIVE_AS_ZERO(entry.grid), 1000),
                gridSellArray: schedule.map(entry => HistoryUtils.ValueConverter.POSITIVE_AS_ZERO_AND_INVERT_NEGATIVE(entry.grid), 1000),
                productionArray: schedule.map(entry => entry.production, 1000),
                consumptionArray: schedule.map(entry => entry.consumption, 1000),
                essDischargeArray: schedule.map(entry => HistoryUtils.ValueConverter.NEGATIVE_AS_ZERO(entry.ess), 1000),
                essChargeArray: schedule.map(entry => HistoryUtils.ValueConverter.POSITIVE_AS_ZERO_AND_INVERT_NEGATIVE(entry.ess), 1000),
                socArray: schedule.map(entry => entry.soc),
                labels: schedule.map(entry => new Date(entry.timestamp)),
            };

            datasets.push({
                type: "line",
                label: this.translate.instant("General.gridBuy"),
                data: gridBuyArray.map(v => Utils.divideSafely(v, 1000)), // [W] to [kW]
                hidden: true,
                order: 1,
            });
            this.colors.push({
                backgroundColor: ColorUtils.rgbStringToRGBA(ChartConstants.Colors.BLUE_GREY, 0.2),
                borderColor: ChartConstants.Colors.BLUE_GREY,
            });

            datasets.push({
                type: "line",
                label: this.translate.instant("General.gridSell"),
                data: gridSellArray.map(v => Utils.divideSafely(v, 1000)), // [W] to [kW]
                hidden: true,
                order: 1,
            });
            this.colors.push({
                backgroundColor: ColorUtils.rgbStringToRGBA(ChartConstants.Colors.PURPLE, 0.2),
                borderColor: ChartConstants.Colors.PURPLE,
            });

            datasets.push({
                type: "line",
                label: this.translate.instant("General.production"),
                data: productionArray.map(v => Utils.divideSafely(v, 1000)), // [W] to [kW]
                hidden: false,
                order: 1,
            });
            this.colors.push({
                backgroundColor: ColorUtils.rgbStringToRGBA(ChartConstants.Colors.BLUE, 0.2),
                borderColor: ChartConstants.Colors.BLUE,
            });

            datasets.push({
                type: "line",
                label: this.translate.instant("General.consumption"),
                data: consumptionArray.map(v => Utils.divideSafely(v, 1000)), // [W] to [kW]
                hidden: false,
                order: 1,
            });
            this.colors.push({
                backgroundColor: ColorUtils.rgbStringToRGBA(ChartConstants.Colors.YELLOW, 0.2),
                borderColor: ChartConstants.Colors.YELLOW,
            });

            datasets.push({
                type: "line",
                label: this.translate.instant("General.CHARGE"),
                data: essChargeArray.map(v => Utils.divideSafely(v, 1000)), // [W] to [kW]
                hidden: true,
                order: 1,
                unit: YAxisType.POWER,
            });
            this.colors.push({
                backgroundColor: ColorUtils.rgbStringToRGBA(ChartConstants.Colors.GREEN, 0.2),
                borderColor: ChartConstants.Colors.GREEN,
            });

            datasets.push({
                type: "line",
                label: this.translate.instant("General.DISCHARGE"),
                data: essDischargeArray.map(v => Utils.divideSafely(v, 1000)), // [W] to [kW]
                hidden: true,
                order: 1,
                unit: YAxisType.POWER,
            });
            this.colors.push({
                backgroundColor: ColorUtils.rgbStringToRGBA(ChartConstants.Colors.RED, 0.2),
                borderColor: ChartConstants.Colors.RED,
            });

            // State of charge data
            datasets.push({
                type: "line",
                label: this.translate.instant("General.soc"),
                data: socArray,
                hidden: false,
                yAxisID: ChartAxis.RIGHT,
                borderDash: [10, 10],
                order: 1,
                unit: YAxisType.PERCENTAGE,
            });
            this.colors.push({
                backgroundColor: "rgba(189, 195, 199,0.2)",
                borderColor: "rgba(189, 195, 199,1)",
            });

            this.datasets = datasets;
            this.loading = false;
            this.labels = labels;
            this.setLabel();
            this.stopSpinner();
        }).catch((reason) => {
            console.error(reason);
            this.initializeChart();
            return;
        }).finally(async () => {
            await this.setOptions(this.options);
            this.applyControllerSpecificOptions();
        });
    }

    private applyControllerSpecificOptions() {
        const rightYAxis: HistoryUtils.yAxes = { position: "right", unit: YAxisType.PERCENTAGE, yAxisId: ChartAxis.RIGHT };
        const leftYAxis: HistoryUtils.yAxes = { position: "left", unit: YAxisType.POWER, yAxisId: ChartAxis.LEFT };
        const locale = this.service.translate.currentLang;

        this.options = NewAbstractHistoryChart.getYAxisOptions(this.options, rightYAxis, this.translate, "line", locale, ChartConstants.EMPTY_DATASETS, true);
        this.options = NewAbstractHistoryChart.getYAxisOptions(this.options, leftYAxis, this.translate, "line", locale, ChartConstants.EMPTY_DATASETS, true);

        this.datasets = this.datasets.map((el: Chart.ChartDataset) => {

            // align particular dataset element to right yAxis
            if (el.label === this.translate.instant("General.soc")) {
                el["yAxisID"] = ChartAxis.RIGHT;
            }
            return el;
        });

        this.options.scales.x["ticks"] = { source: "auto", autoSkip: false };
        this.options.scales.x.ticks.color = getComputedStyle(document.documentElement).getPropertyValue("--ion-color-chart-xAxis-ticks");
        this.options.scales.x.ticks.callback = function (value, index, values) {
            const date = new Date(value);

            // Display the label only if the minutes are zero (full hour)
            return date.getMinutes() === 0 ? date.getHours() + ":00" : "";
        };

        this.options.scales[ChartAxis.RIGHT].grid.display = false;
        this.options.scales[ChartAxis.LEFT].suggestedMin = 0;
        this.options.scales[ChartAxis.LEFT].suggestedMax = 1;
    }

}
