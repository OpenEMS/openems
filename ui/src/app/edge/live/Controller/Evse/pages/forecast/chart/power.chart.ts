// @ts-strict-ignore
import { Component, Input, OnChanges, OnDestroy, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import * as Chart from "chart.js";
import { AbstractHistoryChart } from "src/app/edge/history/abstracthistorychart";
import { AbstractHistoryChart as NewAbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/chart.constants";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from "src/app/shared/shared";
import { ColorUtils } from "src/app/shared/utils/color/color.utils";
import { ChartAxis, HistoryUtils, TimeOfUseTariffUtils, Utils, YAxisType } from "src/app/shared/utils/utils";
import { GetScheduleRequest } from "../../../jsonrpc/getScheduleRequest";
import { GetScheduleResponse } from "../../../jsonrpc/getScheduleResponse";

@Component({
    selector: "powerChart",
    templateUrl: "../../../../../../history/abstracthistorychart.html",
    standalone: false,
})
export class SchedulePowerChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {

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
            const { gridBuyArray, gridSellArray, productionArray, consumptionArray, managedConsumptionArray, labels } = {
                gridBuyArray: schedule.map(entry => HistoryUtils.ValueConverter.NEGATIVE_AS_ZERO(entry.grid), 1000),
                gridSellArray: schedule.map(entry => HistoryUtils.ValueConverter.POSITIVE_AS_ZERO_AND_INVERT_NEGATIVE(entry.grid), 1000),
                productionArray: schedule.map(entry => entry.production, 1000),
                consumptionArray: schedule.map(entry => entry.consumption, 1000),
                managedConsumptionArray: schedule.map(entry => entry.managedConsumption, 1000),
                labels: schedule.map(entry => new Date(entry.timestamp)),
            };

            datasets.push({
                type: "line",
                label: this.translate.instant("GENERAL.GRID_BUY"),
                data: gridBuyArray.map(v => Utils.divideSafely(v, 1000)), // [W] to [kW]
                hidden: true,
                order: 1,
            });
            this.colors.push({
                backgroundColor: ColorUtils.rgbStringToRgba(ChartConstants.Colors.BLUE_GREY, 0.2),
                borderColor: ChartConstants.Colors.BLUE_GREY,
            });

            datasets.push({
                type: "line",
                label: this.translate.instant("GENERAL.GRID_SELL"),
                data: gridSellArray.map(v => Utils.divideSafely(v, 1000)), // [W] to [kW]
                hidden: true,
                order: 1,
            });
            this.colors.push({
                backgroundColor: ColorUtils.rgbStringToRgba(ChartConstants.Colors.PURPLE, 0.2),
                borderColor: ChartConstants.Colors.PURPLE,
            });

            datasets.push({
                type: "line",
                label: this.translate.instant("GENERAL.PRODUCTION"),
                data: productionArray.map(v => Utils.divideSafely(v, 1000)), // [W] to [kW]
                hidden: false,
                order: 1,
            });
            this.colors.push({
                backgroundColor: ColorUtils.rgbStringToRgba(ChartConstants.Colors.BLUE, 0.2),
                borderColor: ChartConstants.Colors.BLUE,
            });

            datasets.push({
                type: "line",
                label: "Consumption",
                data: consumptionArray.map(v => Utils.divideSafely(v, 1000)), // [W] to [kW]
                hidden: true,
                order: 1,
            });
            this.colors.push({
                backgroundColor: ColorUtils.rgbStringToRgba(ChartConstants.Colors.YELLOW, 0.2),
                borderColor: ChartConstants.Colors.YELLOW,
            });
            datasets.push({
                type: "line",
                label: "Managed Consumption",
                data: managedConsumptionArray.map(v => Utils.divideSafely(v, 1000)), // [W] to [kW]
                hidden: false,
                order: 1,
            });
            this.colors.push({
                backgroundColor: ColorUtils.rgbStringToRgba(ChartConstants.Colors.YELLOW, 0.2),
                borderColor: ChartConstants.Colors.ORANGE,
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
        const leftYAxis: HistoryUtils.yAxes = { position: "left", unit: YAxisType.POWER, yAxisId: ChartAxis.LEFT };

        this.options = NewAbstractHistoryChart.getYAxisOptions(this.options, leftYAxis, this.translate, "line", ChartConstants.EMPTY_DATASETS, true);

        this.options.scales.x["ticks"] = { source: "auto", autoSkip: false };
        this.options.scales.x.ticks.color = getComputedStyle(document.documentElement).getPropertyValue("--ion-color-chart-xAxis-ticks");
        this.options.scales.x.ticks.callback = function (value, index, values) {
            const date = new Date(value);

            // Display the label only if the minutes are zero (full hour)
            return date.getMinutes() === 0 ? date.getHours() + ":00" : "";
        };

        this.options.scales[ChartAxis.LEFT].suggestedMin = 0;
        this.options.scales[ChartAxis.LEFT].suggestedMax = 1;
    }

}
