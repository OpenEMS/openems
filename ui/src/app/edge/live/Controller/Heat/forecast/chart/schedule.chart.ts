import { Component, Input, OnChanges, OnDestroy, OnInit, ChangeDetectionStrategy } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import * as Chart from "chart.js";
import { BaseChartDirective } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";
import { filter, take } from "rxjs/operators";
import { AbstractHistoryChart } from "src/app/edge/history/abstracthistorychart";
import { calculateResolution } from "src/app/edge/history/shared";
import { ChartConstants } from "src/app/shared/components/chart/chart.constants";
import { Formatter } from "src/app/shared/components/shared/formatter";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { ChannelAddress, Currency, Edge, EdgeConfig, Service, Websocket } from "src/app/shared/shared";
import { ColorUtils } from "src/app/shared/utils/color/color.utils";
import { ChartAxis, HistoryUtils, TimeOfUseTariffUtils, YAxisType } from "src/app/shared/utils/utils";
import { HistoryDataErrorModule } from "../../../../../../shared/components/history-data-error/history-data-error.module";
import { GetScheduleRequest } from "../../../../../../shared/jsonrpc/request/getScheduleRequest";
import { GetScheduleResponse } from "../../../../../../shared/jsonrpc/response/getScheduleResponse";
import { SharedControllerHeat } from "../../shared/shared";

@Component({
    selector: "scheduleChart",
    templateUrl: "../../../../../history/abstracthistorychart.html",
    standalone: true,
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [HistoryDataErrorModule, NgxSpinnerModule, BaseChartDirective],
})
export class ScheduleChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {
    @Input({ required: true }) public refresh!: boolean;
    @Input({ required: true }) public override edge!: Edge;
    @Input({ required: true }) public component!: EdgeConfig.Component;

    private currencyLabel!: Currency.Label; // Default
    private currencyUnit!: Currency.Unit; // Default

    constructor(
        protected override service: Service,
        protected override translate: TranslateService,
        private websocket: Websocket,
    ) {
        super("schedule-chart", service, translate);
    }

    public getChartHeight(): number {
        return TimeOfUseTariffUtils.getChartHeight(this.service.getIsSmartphoneResolution());
    }

    public ngOnChanges(): void {
        this.edge
            .getConfig(this.websocket)
            .pipe(
                filter((config) => !!config),
                take(1),
            )
            .subscribe((config) => {
                const meta: EdgeConfig.Component = config?.getComponent("_meta");
                const currency: string | null = config?.getPropertyFromComponent<string>(meta, "currency");
                this.currencyLabel = Currency.getCurrencyLabelByCurrency(currency);
                this.currencyUnit = Currency.getChartCurrencyUnitLabel(currency ?? "");
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
                const colors = scheduleChartColors(schedule.length, this.colors);

                // Extracting prices, states, timestamps from the schedule array
                const { priceArray, modeArray, timestampArray } = {
                    priceArray: schedule.map((entry) => (entry.price === null ? 10 : entry.price)), // TODO: Use different chart type when no prices
                    modeArray: schedule.map((entry) => entry.mode),
                    timestampArray: schedule.map((entry) => entry.timestamp),
                };

                const scheduleChartData = SharedControllerHeat.getScheduleChartData(
                    schedule.length,
                    priceArray,
                    modeArray,
                    timestampArray,
                    this.translate,
                );
                colors.splice(0, colors.length, ...scheduleChartData.colors);
                this.labels = scheduleChartData.labels;

                this.datasets = scheduleChartData.datasets as Chart.ChartDataset[];
                this.loading = false;
                this.setLabel();
                this.stopSpinner();
            })
            .catch((reason) => {
                console.error(reason);
                this.initializeChart();
                return;
            })
            .finally(async () => {
                this.unit = YAxisType.CURRENCY;
                const options = this.options;

                if (options != null) {
                    await this.setOptions(options);
                    this.applyControllerSpecificOptions();
                }
            });
    }

    protected setLabel() {
        this.options = this.createDefaultChartOptions();
    }

    protected getChannelAddresses(): Promise<ChannelAddress[]> {
        return new Promise(() => {
            [];
        });
    }

    private applyControllerSpecificOptions() {
        const options = this.options;

        if (options == null) {
            return;
        }

        const xScale = options.scales?.x as TimeScaleOptions | undefined;
        const scales = options.scales;
        const tooltipCallbacks = options.plugins?.tooltip?.callbacks;
        const tooltip = options.plugins?.tooltip;

        if (xScale == null || scales == null) {
            return;
        }

        xScale.time = {
            ...xScale.time,
            unit: calculateResolution(
                this.service,
                this.service.historyPeriod.value.from,
                this.service.historyPeriod.value.to,
            ).timeFormat,
        };
        xScale.ticks = {
            ...xScale.ticks,
            source: "auto",
            autoSkip: false,
            maxTicksLimit: 30,
            callback: (value) => {
                const date = new Date(value as string | number);

                return date.getMinutes() === 0 ? date.getHours() + ":00" : "";
            },
        };
        xScale.offset = false;

        if (tooltip != null) {
            tooltip.mode = "index";
        }

        if (tooltipCallbacks != null) {
            tooltipCallbacks.labelColor = (item: Chart.TooltipItem<any>): Chart.TooltipLabelStyle => {
                const backgroundColor: string = asColorString(
                    item.dataset.backgroundColor,
                    asColorString(item.dataset.borderColor, "rgba(0, 0, 0, 0.5)"),
                );
                const borderColor: string = withOpacity(backgroundColor, 1);

                return {
                    borderColor,
                    backgroundColor: backgroundColor,
                };
            };

            tooltipCallbacks.label = (item: Chart.TooltipItem<any>) => {
                const label = item.dataset.label;
                const value = item.dataset.data[item.dataIndex];

                return label + ": " + Formatter.FORMAT_CURRENCY_PER_KWH(value, this.currencyLabel);
            };
        }

        this.datasets = this.datasets.map((el) => {
            const opacity = el.type === "line" ? 0.2 : 0.5;
            const backgroundColor = typeof el.backgroundColor === "string" ? el.backgroundColor : undefined;
            const borderColor = typeof el.borderColor === "string" ? el.borderColor : undefined;

            if (backgroundColor != null) {
                el.backgroundColor = ColorUtils.changeOpacityFromRGBA(backgroundColor, opacity) ?? backgroundColor;
            }

            if (borderColor != null) {
                el.borderColor = ColorUtils.changeOpacityFromRGBA(borderColor, 1) ?? borderColor;
            }
            return el;
        });

        const leftYAxis: HistoryUtils.yAxes = {
            position: "left",
            unit: this.unit,
            yAxisId: ChartAxis.LEFT,
            customTitle: this.currencyUnit,
            scale: { dynamicScale: true },
        };

        scales[ChartAxis.LEFT] = {
            ...scales[ChartAxis.LEFT],
            ...ChartConstants.DEFAULT_Y_SCALE_OPTIONS(
                leftYAxis,
                this.translate,
                "bar",
                this.datasets.filter((el) => (el as ScheduleChartDataset).yAxisID === ChartAxis.LEFT),
                true,
            ),
        };
    }
}

function scheduleChartColors(_length: number, colors: unknown[]): ChartColor[] {
    return colors as ChartColor[];
}

function asColorString(color: unknown, fallback: string): string {
    if (Array.isArray(color)) {
        return asColorString(color[0], fallback);
    }

    return typeof color === "string" ? color : fallback;
}

function withOpacity(color: string, opacity: number): string {
    return ColorUtils.changeOpacityFromRGBA(color, opacity) || color;
}

type ChartColor = {
    backgroundColor: string;
    borderColor: string;
};

type ScheduleChartDataset =
    | (Chart.ChartDataset<"line", (number | null)[]> & { yAxisID?: string })
    | (Chart.ChartDataset<"bar", (number | null)[]> & { yAxisID?: string });

type TimeScaleOptions = Chart.TimeScaleOptions;
