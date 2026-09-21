// @ts-strict-ignore
import { CommonModule } from "@angular/common";
import { Component, ElementRef, inject, Input, OnChanges, SimpleChanges, ChangeDetectionStrategy } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { Chart, ChartDataset, LegendItem, LineControllerDatasetOptions, TooltipItem } from "chart.js";
import { BaseChartDirective } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";
import { draw } from "patternomaly";
import { ChartData } from "src/app/edge/history/shared";
import { PlatFormService } from "src/app/platform.service";
import { Currency } from "src/app/shared/shared";
import { ColorUtils } from "src/app/shared/utils/color/color.utils";
import { NumberUtils } from "src/app/shared/utils/number/number-utils";
import { ChartAxis, HistoryUtils } from "src/app/shared/utils/utils";
import { GetSchedule } from "../../edge/config-components/energy/getSchedule";
import { Edge } from "../../edge/edge";
import { EdgeConfig } from "../../edge/edgeconfig";
import { HistoryDataErrorModule } from "../../history-data-error/history-data-error.module";
import { Converter } from "../../shared/converter";
import { AbstractHistoryChart } from "../abstracthistorychart";
import { ChartConstants } from "../chart.constants";
import { ChartComponentsModule } from "../chart.module";

Chart.register(ChartConstants.Plugins.SYNC_CHARTS());

@Component({
    selector: "oe-schedule-chart",
    templateUrl: "../abstracthistorychart.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [
        BaseChartDirective,
        ReactiveFormsModule,
        IonicModule,
        TranslateModule,
        ChartComponentsModule,
        HistoryDataErrorModule,
        NgxSpinnerModule,
        CommonModule,
    ],
})
export abstract class ScheduleChartComponent extends AbstractHistoryChart implements OnChanges {
    public static readonly OPACITY_DEFAULT = 0.2;
    public static readonly OPACITY_TRANSPARENT = 0.05;
    public static readonly OPACITY_NONE = 1;

    public static readonly BORDER_DASHED: [number, number] = [5, 2];

    @Input({ required: true }) public refresh!: boolean;
    @Input({ required: true }) public data!: GetSchedule.Response;
    @Input({ required: true }) public override edge!: Edge;

    protected numberFormat: ChartData["tooltip"]["formatNumber"] = ChartConstants.NumberFormat.NO_DECIMALS;
    private hasBooleanValues = false;

    private readonly platFormService = inject(PlatFormService);
    private readonly hostEl = inject(ElementRef<HTMLElement>);

    public ngOnChanges(changes: SimpleChanges): void {
        if (!this.config) {
            return;
        }

        if (changes["refresh"] || changes["edge"] || changes["data"]) {
            this.updateChart();
        }
    }

    protected override getChartData(): HistoryUtils.ChartData | null {
        return {
            input: [],
            output: (data) => [],
            tooltip: {
                formatNumber: this.numberFormat,
            },
            yAxes: [],
        };
    }

    protected override async loadChart(): Promise<void> {
        if (this.edge == null) {
            return;
        }

        this.labels = this.data.getLabels24h();
        this.errorResponse = null;
        this.loading = true;
        this.chartType = "line";

        this.options.scales.x = {
            type: "time",
            ticks: {
                ...this.options.scales.x.ticks,
                display: false,
                color: getComputedStyle(
                    document.documentElement,
                ).getPropertyValue("--ion-color-chart-xAxis-ticks"),
            },
            grid: {
                display: false,
            },
        };

        this.options.scales[ChartAxis.LEFT] = {
            grid: {
                display: false,
            },
            ticks: {
                color: getComputedStyle(
                    document.documentElement,
                ).getPropertyValue("--ion-color-chart-xAxis-ticks"),
            },
        };

        // Show Legend if any label is set
        this.options.plugins.legend.display = true;
        this.options.plugins.legend.labels.color = getComputedStyle(document.documentElement).getPropertyValue(
            "--ion-color-text",
        );
        this.options.plugins.legend.labels.generateLabels = this.generateLegendLabels.bind(this);

        this.options.plugins["syncChart"] = {
            group: 1,
        };

        this.datasets = this.fillDatasets();

        // For Charts with Boolean values: scale exactly [0;1]
        if (this.hasBooleanValues) {
            this.options.scales[ChartAxis.LEFT] = {
                ...this.options.scales[ChartAxis.LEFT],
                min: 0,
                max: 1.01, // slightly more than 1 to show full line
                ticks: {
                    ...this.options.scales[ChartAxis.LEFT]["ticks"],
                    display: false,
                },
            };
        }

        const leftAxisBounds = this.getLeftAxisBounds();
        if (Object.keys(leftAxisBounds).length > 0) {
            this.options.scales[ChartAxis.LEFT] = {
                ...this.options.scales[ChartAxis.LEFT],
                ...leftAxisBounds,
            };
        }

        /** Tooltips */
        this.options.plugins.tooltip.position = "bottom";
        this.options.plugins.tooltip.callbacks.title = () => null;
        const tooltipLabelCallback = this.getTooltipLabelCallback();
        if (tooltipLabelCallback != null) {
            this.options.plugins.tooltip.callbacks.label = tooltipLabelCallback;
        }
        this.options.plugins.tooltip.filter = (item, _index, items) => item.datasetIndex === items.at(-1)?.datasetIndex;

        this.stopSpinner();
        this.loading = false;
    }

    protected fillDatasets(): ChartDataset[] {
        const buildConf = this.buildDatasets();
        this.hasBooleanValues = buildConf.some((dataset) => dataset.data.some((value) => typeof value === "boolean"));

        const baseDataset = (d: ScheduleChartComponent.Dataset): ChartDataset => ({
            type: "line",
            label: d.label ?? null,
            data: d.data.map((value) => {
                if (typeof value === "boolean") {
                    return value ? 1 : 0;
                }
                return value;
            }),
            hidden: false,
            order: 1,
            yAxisID: ChartAxis.LEFT,
            backgroundColor:
                d.pattern == null
                    ? ColorUtils.rgbStringToRgba(d.color, d.opacity ?? ScheduleChartComponent.OPACITY_DEFAULT)
                    : [draw(d.pattern, d.color, "white", 5)],
            borderColor: d.color,
            borderWidth: d.borderWidth ?? 2,
            borderDash: d.borderDash,
            stepped: d.stepped,

            /** Tooltip marker */
            pointHoverRadius: 5,
            pointHoverBackgroundColor: d.color,
            pointHoverBorderColor: d.color,
            pointHoverBorderWidth: 1,
        });

        return buildConf.map((el) => baseDataset(el));
    }

    protected buildDatasets(): ScheduleChartComponent.Dataset[] {
        return [];
    }
    protected generateLegendLabels(chart: Chart): LegendItem[] {
        return Chart.defaults.plugins.legend.labels
            .generateLabels(chart)
            .filter((item) => item.text !== null)
            .map((item) => ({ ...item, fillStyle: item.strokeStyle }));
    }

    protected getTooltipLabelCallback(): (item: TooltipItem<any>) => string | string[] | void {
        return () => null;
    }

    protected getLeftAxisBounds(): Partial<{ min: number; max: number }> {
        return {};
    }

    protected override getChartHeight(): number | null {
        const device = this.platFormService.getDevice();
        const isSmartPhone = device.isSmartphone();
        const container = this.hostEl.nativeElement.closest("#formlyContainerWidth") as HTMLElement | null;
        const width = container?.getBoundingClientRect().width ?? window.innerWidth;

        if (isSmartPhone) {
            return NumberUtils.divideSafely(width, 2);
        }
        return NumberUtils.divideSafely(width, 6);
    }

    protected createDatasetPair(
        color: string,
        label: string,
        historyData: (number | boolean | null)[],
        predictionData: (number | boolean | null)[],
    ): ScheduleChartComponent.Dataset[] {
        const hasValues = (values: (number | boolean | null)[]): boolean => values.some((value) => value != null);

        if (!hasValues(historyData) && !hasValues(predictionData)) {
            return [];
        }

        return [
            {
                color,
                data: historyData,
                label,
                stepped: true,
                opacity: ScheduleChartComponent.OPACITY_NONE,
                borderWidth: 0,
            },
            {
                color,
                data: predictionData,
                label,
                stepped: true,
                opacity: ScheduleChartComponent.OPACITY_NONE,
                pattern: "plus",
                borderWidth: 0,
            },
        ];
    }
}

export namespace ScheduleChartComponent {
    export type Dataset = {
        label?: string;
        color: string;
        pattern?: Parameters<typeof draw>[0];
        data: (number | boolean | null)[];
        borderDash?: [number, number] | [];
        borderWidth?: number;
        stepped?: LineControllerDatasetOptions["stepped"] | false;
        opacity?: number;
    };

    /**
     * Use this function to split data in positive and abs(negative) values, ready for visualization in a
     * schedule-chart.
     */
    export function normalizePositiveNegativeLines(data: (number | null)[]): {
        positive: (number | null)[];
        negative: (number | null)[];
    } {
        const positive = data.map((el) => (el != null && el >= 0 ? el : null));
        const negative = data.map((el) => (el != null && el <= 0 ? Math.abs(el) : null));

        for (let i = 0; i < positive.length; i++) {
            /** When power is 'zero', decide which chart line (charge or discharge) should be visible */
            if (positive[i] == 0 && negative[i] == 0) {
                // Find 'zero' power values
                if (
                    i === 0 || // Fallback for first value -> prefer charge
                    positive[i - 1] != null // keep charge line visible
                ) {
                    negative[i] = null;
                } else {
                    positive[i] = null;
                }
            }

            /** Fill gaps when switching between charge and discharge lines */
            if (i > 0) {
                // Avoid index out of bounds
                if (positive[i - 1] != null && negative[i] != null) {
                    negative[i - 1] = 0;
                } else if (negative[i - 1] != null && positive[i] != null) {
                    positive[i - 1] = 0;
                }
            }
        }
        return { positive: positive, negative: negative };
    }

    /** Use this function to fill gaps for boolean values, ready for visualization in a schedule-chart. */
    export function normalizeBooleanLines(data: Record<string, boolean | null>[]): void {
        for (let i = data.length - 1; i >= 0; i--) {
            for (const key of Object.keys(data[i])) {
                if (i > 0 && (data[i][key] === null || data[i][key] === false) && data[i - 1][key] === true) {
                    // Keep boolean state charts continuous at state transitions.
                    data[i][key] = true;
                }
            }
        }
    }

    /** Tooltip for values in [kW]. */
    export function tooltipkW(): (item: TooltipItem<any>) => string {
        return (item: TooltipItem<any>) => Converter.POWER_IN_KILO_WATT_AS_KW(item.dataset.data[item.dataIndex]);
    }

    /** Tooltip for values in currency per kWh. */
    export function tooltipCurrency(config: EdgeConfig): (item: TooltipItem<any>) => string {
        const meta = config.getComponentSafely("_meta");
        const currency = config.getPropertyFromComponent<string>(meta, "currency");
        const currencyLabel: Currency.Label = Currency.getCurrencyLabelByCurrency(currency);

        return (item: TooltipItem<any>) =>
            Converter.CURRENCY_PER_KWH_TO_KWH(currencyLabel)(item.dataset.data[item.dataIndex]);
    }
}
