import { Component, Input, OnChanges, OnDestroy, OnInit, ChangeDetectionStrategy } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import * as Chart from "chart.js";
import { BaseChartDirective } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";
import { AbstractHistoryChart } from "src/app/edge/history/abstracthistorychart";
import { AbstractHistoryChart as NewAbstractHistoryChart } from "src/app/shared/components/chart/abstracthistorychart";
import { ChartConstants } from "src/app/shared/components/chart/chart.constants";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from "src/app/shared/shared";
import { ColorUtils } from "src/app/shared/utils/color/color.utils";
import { ChartAxis, HistoryUtils, TimeOfUseTariffUtils, Utils, YAxisType } from "src/app/shared/utils/utils";
import { HistoryDataErrorModule } from "../../../../../../shared/components/history-data-error/history-data-error.module";
import { GetScheduleRequest } from "../../../../../../shared/jsonrpc/request/getScheduleRequest";
import { GetScheduleResponse } from "../../../../../../shared/jsonrpc/response/getScheduleResponse";

@Component({
    selector: "powerChart",
    templateUrl: "../../../../../history/abstracthistorychart.html",
    standalone: true,
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [HistoryDataErrorModule, NgxSpinnerModule, BaseChartDirective],
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
        return TimeOfUseTariffUtils.getChartHeight(this.service.getIsSmartphoneResolution());
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
        return Promise.resolve([]);
    }

    protected override updateChart() {
        this.autoSubscribeChartRefresh();
        this.service.startSpinner(this.spinnerId);
        this.loading = true;

        this.edge
            .sendRequest(
                this.websocket,
                new ComponentJsonApiRequest({
                    componentId: "heat0",
                    payload: new GetScheduleRequest({
                        componentId: this.component.id,
                    }),
                }),
            )
            .then((response) => {
                const result = (response as GetScheduleResponse).result;
                const schedule = result.schedule;
                const datasets: PowerChartDataset[] = [];
                const colors = this.colors as unknown as ChartColor[];

                // Extracting prices and states from the schedule array
                const {
                    gridBuyArray,
                    gridSellArray,
                    productionArray,
                    consumptionArray,
                    managedConsumptionArray,
                    labels,
                } = {
                    gridBuyArray: schedule.map((entry) => HistoryUtils.ValueConverter.NEGATIVE_AS_ZERO(entry.grid)),
                    gridSellArray: schedule.map((entry) =>
                        HistoryUtils.ValueConverter.POSITIVE_AS_ZERO_AND_INVERT_NEGATIVE(entry.grid),
                    ),
                    productionArray: schedule.map((entry) => entry.production),
                    consumptionArray: schedule.map((entry) => entry.consumption),
                    managedConsumptionArray: schedule.map((entry) => entry.managedConsumption),
                    labels: schedule.map((entry) => new Date(entry.timestamp)),
                };

                datasets.push({
                    type: "line",
                    label: this.translate.instant("GENERAL.GRID_BUY"),
                    data: gridBuyArray.map((v) => Utils.divideSafely(v, 1000)), // [W] to [kW]
                    hidden: true,
                    order: 1,
                });
                colors.push({
                    backgroundColor: ColorUtils.rgbStringToRgba(ChartConstants.Colors.BLUE_GREY, 0.2),
                    borderColor: ChartConstants.Colors.BLUE_GREY,
                });

                datasets.push({
                    type: "line",
                    label: this.translate.instant("GENERAL.GRID_SELL"),
                    data: gridSellArray.map((v) => Utils.divideSafely(v, 1000)), // [W] to [kW]
                    hidden: true,
                    order: 1,
                });
                colors.push({
                    backgroundColor: ColorUtils.rgbStringToRgba(ChartConstants.Colors.PURPLE, 0.2),
                    borderColor: ChartConstants.Colors.PURPLE,
                });

                datasets.push({
                    type: "line",
                    label: this.translate.instant("GENERAL.PRODUCTION"),
                    data: productionArray.map((v) => Utils.divideSafely(v, 1000)), // [W] to [kW]
                    hidden: false,
                    order: 1,
                });
                colors.push({
                    backgroundColor: ColorUtils.rgbStringToRgba(ChartConstants.Colors.BLUE, 0.2),
                    borderColor: ChartConstants.Colors.BLUE,
                });

                datasets.push({
                    type: "line",
                    label: this.translate.instant("GENERAL.CONSUMPTION"),
                    data: consumptionArray.map((v) => Utils.divideSafely(v, 1000)), // [W] to [kW]
                    hidden: true,
                    order: 1,
                });
                colors.push({
                    backgroundColor: ColorUtils.rgbStringToRgba(ChartConstants.Colors.YELLOW, 0.2),
                    borderColor: ChartConstants.Colors.YELLOW,
                });
                datasets.push({
                    type: "line",
                    label: this.translate.instant("GENERAL.MANAGED_CONSUMPTION"),
                    data: managedConsumptionArray.map((v) => Utils.divideSafely(v, 1000)), // [W] to [kW]
                    hidden: false,
                    order: 1,
                });
                colors.push({
                    backgroundColor: ColorUtils.rgbStringToRgba(ChartConstants.Colors.YELLOW, 0.2),
                    borderColor: ChartConstants.Colors.ORANGE,
                });

                this.datasets = datasets as Chart.ChartDataset[];
                this.loading = false;
                this.labels = labels;
                this.setLabel();
                this.stopSpinner();
            })
            .catch((reason) => {
                console.error(reason);
                this.initializeChart();
                return;
            })
            .finally(async () => {
                const options = this.options;

                if (options != null) {
                    await this.setOptions(options);
                    this.applyControllerSpecificOptions();
                }
            });
    }

    private applyControllerSpecificOptions() {
        const options = this.options;

        if (options == null) {
            return;
        }

        const leftYAxis: HistoryUtils.yAxes = {
            position: "left",
            unit: YAxisType.POWER,
            yAxisId: ChartAxis.LEFT,
        };
        this.options = NewAbstractHistoryChart.getYAxisOptions(
            options,
            leftYAxis,
            this.translate,
            "line",
            this.datasets,
            true,
        );

        const chartOptions = this.options;
        const xScale = chartOptions?.scales?.x;
        const leftScale = chartOptions?.scales?.[ChartAxis.LEFT];

        if (xScale != null) {
            xScale.ticks = {
                ...xScale.ticks,
                source: "auto",
                autoSkip: false,
                color: getComputedStyle(document.documentElement).getPropertyValue("--ion-color-chart-xAxis-ticks"),
                callback: (value) => {
                    const date = new Date(value as string | number);

                    return date.getMinutes() === 0 ? date.getHours() + ":00" : "";
                },
            };
        }

        if (leftScale != null) {
            leftScale.suggestedMin = 0;
            leftScale.suggestedMax = 1;
        }
    }
}

type ChartColor = {
    backgroundColor: string;
    borderColor: string;
};

type PowerChartDataset = Chart.ChartDataset<"line", (number | null)[]>;
