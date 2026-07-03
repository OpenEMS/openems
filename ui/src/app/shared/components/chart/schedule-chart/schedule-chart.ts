// @ts-strict-ignore
import { CommonModule } from "@angular/common";
import { Component, ElementRef, inject, Input, OnChanges, SimpleChanges, } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { Chart, ChartDataset, LineControllerDatasetOptions } from "chart.js";
import { BaseChartDirective } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";
import { ChartData } from "src/app/edge/history/shared";
import { PlatFormService } from "src/app/platform.service";
import { ColorUtils } from "src/app/shared/utils/color/color.utils";
import { NumberUtils } from "src/app/shared/utils/number/number-utils";
import { ChartAxis, HistoryUtils } from "src/app/shared/utils/utils";
import { GetSchedule } from "../../edge/config-components/energy/getSchedule";
import { Edge } from "../../edge/edge";
import { HistoryDataErrorModule } from "../../history-data-error/history-data-error.module";
import { AbstractHistoryChart } from "../abstracthistorychart";
import { ChartConstants } from "../chart.constants";
import { ChartComponentsModule } from "../chart.module";

@Component({
    selector: "oe-schedule-chart",
    templateUrl: "../abstracthistorychart.html",
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
export abstract class ScheduleChartComponent
    extends AbstractHistoryChart
    implements OnChanges
{
    @Input({ required: true }) public refresh!: boolean;
    @Input({ required: true }) public data!: GetSchedule.Response;
    @Input({ required: true }) public override edge!: Edge;

    protected numberFormat: ChartData["tooltip"]["formatNumber"] =
        ChartConstants.NumberFormat.NO_DECIMALS;
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

        this.options.plugins.tooltip.enabled = false;

        this.options.scales.x = {
            type: "time",
            ticks: {
                ...this.options.scales.x.ticks,
                display: false,
            },
            grid: {
                display: false,
            },
        };

        this.options.scales[ChartAxis.LEFT] = {
            grid: {
                display: false,
            },
        };

        // Show Legend if any label is set
        this.options.plugins.legend.display = true;
        this.options.plugins.legend.labels.color = getComputedStyle(
            document.documentElement,
        ).getPropertyValue("--ion-color-text");
        this.options.plugins.legend.labels.generateLabels = (chart: Chart) =>
            Chart.defaults.plugins.legend.labels
                .generateLabels(chart)
                .filter((item) => item.text !== null) //
                .map((item) => {
                    return {
                        ...item,
                        fillStyle: item.strokeStyle,
                    };
                });

        Chart.register(ChartConstants.Plugins.SYNC_CHARTS());
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

        this.stopSpinner();
        this.loading = false;
    }

    protected fillDatasets(): ChartDataset[] {
        const buildConf = this.buildDatasets();
        this.hasBooleanValues = buildConf.some((dataset) =>
            dataset.data.some((value) => typeof value === "boolean"),
        );

        const baseDataset = (
            d: ScheduleChartComponent.Dataset,
        ): ChartDataset => ({
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
            backgroundColor: ColorUtils.rgbStringToRgba(
                d.color,
                d.transparentBackground ? 0.05 : 0.2,
            ),
            borderColor: d.color,
            borderWidth: 2,
            borderDash: d.borderDash,
            stepped: d.stepped,
        });

        return buildConf.map((el) => baseDataset(el));
    }

    protected buildDatasets(): ScheduleChartComponent.Dataset[] {
        return [];
    }

    protected getLeftAxisBounds(): Partial<{ min: number; max: number }> {
        return {};
    }

    protected override getChartHeight(): number | null {
        const device = this.platFormService.getDevice();
        const isSmartPhone = device.isSmartphone();
        const container = this.hostEl.nativeElement.closest(
            "#formlyContainerWidth",
        ) as HTMLElement | null;
        const width =
            container?.getBoundingClientRect().width ?? window.innerWidth;

        if (isSmartPhone) {
            return NumberUtils.divideSafely(width, 2);
        }
        return NumberUtils.divideSafely(width, 5);
    }
}

export namespace ScheduleChartComponent {
    export type Dataset = {
        label?: string;
        color: string;
        data: (number | boolean | null)[];
        borderDash?: [number, number] | [];
        stepped?: LineControllerDatasetOptions["stepped"] | false;
        transparentBackground?: boolean;
    };

    /**
     * Use this function to split data in positive and abs(negative) values,
     * ready for visualization in a schedule-chart.
     */
    export function normalizePositiveNegativeLines(data: (number | null)[]): {
        positive: (number | null)[];
        negative: (number | null)[];
    } {
        const positive = data.map((el) =>
            el != null && el <= 0 ? Math.abs(el) : null,
        );
        const negative = data.map((el) => (el != null && el >= 0 ? el : null));

        for (let i = 0; i < positive.length; i++) {
            /**
             * When power is 'zero', decide which chart line (charge or
             * discharge) should be visible
             */
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

    /**
     * Use this function to fill gaps for boolean values, ready for
     * visualization in a schedule-chart.
     */
    export function normalizeBooleanLines(
        data: Record<string, boolean | null>[],
    ): void {
        for (let i = data.length - 1; i >= 0; i--) {
            for (const key of Object.keys(data[i])) {
                if (
                    i > 0 &&
                    (data[i][key] === null || data[i][key] === false) &&
                    data[i - 1][key] === true
                ) {
                    // Keep boolean state charts continuous at state transitions.
                    data[i][key] = true;
                }
            }
        }
    }
}
