import { ChangeDetectionStrategy, Component, Input, OnChanges, OnDestroy, OnInit } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import * as Chart from "chart.js";
import { filter, take } from "rxjs/operators";
import { AbstractHistoryChart } from "src/app/edge/history/abstracthistorychart";
import { calculateResolution } from "src/app/edge/history/shared";
import { ChartConstants } from "src/app/shared/components/chart/chart.constants";
import { Formatter } from "src/app/shared/components/shared/formatter";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { ChannelAddress, Currency, Edge, EdgeConfig, Service, Websocket } from "src/app/shared/shared";
import { ColorUtils } from "src/app/shared/utils/color/color.utils";
import { ChartAxis, HistoryUtils, TimeOfUseTariffUtils, YAxisType } from "src/app/shared/utils/utils";
import { GetScheduleRequest } from "../../../../../../../shared/jsonrpc/request/getScheduleRequest";
import { GetScheduleResponse } from "../../../../../../../shared/jsonrpc/response/getScheduleResponse";
import { ControllerEvseSingleShared } from "../../../shared/shared";

@Component({
    selector: "scheduleChart",
    templateUrl: "../../../../../../history/abstracthistorychart.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class ScheduleChartComponent extends AbstractHistoryChart implements OnInit, OnChanges, OnDestroy {
    @Input({ required: true }) public refresh!: boolean;
    @Input({ required: true }) public override edge!: Edge;
    @Input({ required: true }) public component!: EdgeConfig.Component;

    private currencyLabel: Currency.Label = Currency.Label.CENT_PER_KWH; // Default
    private currencyUnit: Currency.Unit = Currency.Unit.CENT; // Default

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
                this.currencyLabel = Currency.getCurrencyLabelByCurrency(currency ?? "  ");
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
                    componentId: "ctrlEvseCluster0",
                    payload: new GetScheduleRequest({
                        componentId: this.component.id,
                    }),
                }),
            )
            .then((response) => {
                const result = (response as GetScheduleResponse).result;
                const schedule = result.schedule;

                // Extracting prices, states, timestamps from the schedule array
                const { priceArray, modeArray, timestampArray } = {
                    priceArray: schedule.map((entry) => (entry.price === null ? 10 : entry.price)), // TODO: Use different chart type when no prices
                    modeArray: schedule.map((entry) => entry.mode),
                    timestampArray: schedule.map((entry) => entry.timestamp),
                };

                const scheduleChartData = ControllerEvseSingleShared.getScheduleChartData(
                    schedule.length,
                    priceArray,
                    modeArray,
                    timestampArray,
                    this.translate,
                );

                this.colors = scheduleChartData.colors;
                this.labels = scheduleChartData.labels;

                this.datasets = scheduleChartData.datasets;
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
                }
                this.applyControllerSpecificOptions();
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

        const xScale = options.scales?.x as Chart.TimeScaleOptions | undefined;
        if (xScale != null) {
            xScale.time.unit = calculateResolution(
                this.service,
                this.service.historyPeriod.value.from,
                this.service.historyPeriod.value.to,
            ).timeFormat;
            xScale.ticks = { source: "auto", autoSkip: false } as typeof xScale.ticks;
            xScale.ticks.maxTicksLimit = 30;
            xScale.offset = false;
            xScale.ticks.callback = function (value) {
                const date = new Date(value as number | string);

                // Display the label only if the minutes are zero (full hour)
                return date.getMinutes() === 0 ? date.getHours() + ":00" : "";
            };
        }

        const tooltipCallbacks = options.plugins?.tooltip?.callbacks;
        if (options.plugins?.tooltip != null) {
            options.plugins.tooltip.mode = "index";
        }
        if (tooltipCallbacks != null) {
            tooltipCallbacks.labelColor = (item: Chart.TooltipItem<any>) => {
                if (!item) {
                    return;
                }
                return {
                    borderColor: ColorUtils.changeOpacityFromRGBA(item.dataset.borderColor, 1),
                    backgroundColor: item.dataset.backgroundColor,
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

            if (el.backgroundColor && el.borderColor) {
                el.backgroundColor = ColorUtils.changeOpacityFromRGBA(el.backgroundColor.toString(), opacity);
                el.borderColor = ColorUtils.changeOpacityFromRGBA(el.borderColor.toString(), 1);
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

        const scales = options.scales;
        if (scales != null) {
            scales[ChartAxis.LEFT] = {
                ...scales[ChartAxis.LEFT],
                ...ChartConstants.DEFAULT_Y_SCALE_OPTIONS(
                    leftYAxis,
                    this.translate,
                    "bar",
                    this.datasets.filter(
                        (el) => (el as Chart.ChartDataset & { yAxisID?: string }).yAxisID === ChartAxis.LEFT,
                    ),
                    true,
                ),
            };
        }
    }
}
