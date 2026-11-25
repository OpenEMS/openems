import { Component, signal } from "@angular/core";
import { Chart, ChartDataset, TooltipItem } from "chart.js";
import { color as chartColor } from "chart.js/helpers";
import { BaseChartDirective } from "ng2-charts";
import { NgxSpinnerModule } from "ngx-spinner";
import { Theme } from "src/app/edge/history/shared";
import { ColorUtils } from "src/app/shared/utils/color/color.utils";
import { DateTimeFormats, DateTimeUtils } from "src/app/shared/utils/datetime/datetime-utils";
import { ChartAxis } from "src/app/shared/utils/utils";
import { HistoryDataErrorModule } from "../../history-data-error/history-data-error.module";
import { ChartConstants } from "../chart.constants";
import { ScheduleChartComponent } from "../schedule-chart/schedule-chart";

Chart.register(ChartConstants.Plugins.SCHEDULE_MODE_BAND);

/** Data indices match the schedule; 1 marks an active mode, null an empty period. */
export type ModeDataset = {
    color: string;
    label: string;
    data: (1 | null)[];
};

function darkenColor(color: string): string {
    const parsedColor = chartColor(color);
    if (parsedColor.valid === false) {
        throw new Error(`Unsupported mode chart color: ${color}`);
    }
    const { r, g, b } = parsedColor.rgb;
    return chartColor({
        r: Math.round(r * 0.8),
        g: Math.round(g * 0.8),
        b: Math.round(b * 0.8),
        a: 1,
    }).rgbString();
}

export function createForecastPattern(color: string, backgroundColor: string): CanvasPattern {
    const [dashLength, gapLength] = ScheduleChartComponent.BORDER_DASHED;
    const tileSize = dashLength + gapLength;
    const canvas = document.createElement("canvas");
    canvas.width = tileSize;
    canvas.height = tileSize;

    const context = canvas.getContext("2d");
    if (context == null) {
        throw new Error("Could not create mode chart forecast pattern");
    }

    const shadedBackgroundColor = darkenColor(backgroundColor);
    const isLightTheme = document.documentElement.getAttribute("data-theme") === Theme.LIGHT;

    // Precompose the line-chart fill to keep adjacent bars opaque and seam-free.
    // Keep light backgrounds bright instead of shading white to grey.
    context.fillStyle = isLightTheme === true ? backgroundColor : shadedBackgroundColor;
    context.fillRect(0, 0, tileSize, tileSize);
    context.fillStyle = ColorUtils.rgbStringToRgba(
        isLightTheme === true ? color : darkenColor(color),
        ScheduleChartComponent.OPACITY_DEFAULT,
    );
    context.fillRect(0, 0, tileSize, tileSize);
    context.fillStyle = color;
    context.fillRect(0, 0, dashLength, tileSize);

    const pattern = context.createPattern(canvas, "repeat");
    if (pattern == null) {
        throw new Error("Could not create mode chart forecast pattern");
    }
    // Compensate for the diagonal projection to preserve horizontal dash/gap widths.
    pattern.setTransform(
        new DOMMatrix()
            .rotate(-45)
            .scale(Math.SQRT1_2)
            .translate(-dashLength / 2, 0),
    );
    return pattern;
}

/**
 * Shared mode timeline with schedule-aligned tooltips and forecast hatching. Subclasses provide device-specific labels,
 * colors and mutually exclusive mode datasets.
 */
@Component({
    selector: "oe-schedule-mode-chart",
    templateUrl: "../../../../edge/history/abstracthistorychart.html",
    imports: [BaseChartDirective, HistoryDataErrorModule, NgxSpinnerModule],
})
export abstract class ScheduleModeChartComponent extends ScheduleChartComponent {
    private static readonly BAND_HEIGHT = 48;

    private readonly modeChartHeight = signal(ScheduleModeChartComponent.BAND_HEIGHT * 2);

    protected override getChartHeight(): number | null {
        return this.modeChartHeight();
    }

    protected override getScheduleChartType(): "bar" {
        return "bar";
    }

    protected override configureScheduleChartOptions(): void {
        if (this.options == null || this.options.scales == null || this.options.plugins?.tooltip?.callbacks == null) {
            throw new Error("Schedule chart options are not initialized");
        }

        this.options.scales.x = {
            type: "time",
            stacked: true,
            offset: false,
            bounds: "data",
        };
        this.options.scales[ChartAxis.LEFT] = {
            type: "linear",
            min: 0,
            max: 1,
            stacked: true,
        };
        for (const scale of Object.values(this.options.scales)) {
            if (scale != null) {
                scale.display = false;
            }
        }
        this.options.interaction = { mode: "index", intersect: false, axis: "x" };
        this.options.animation = false;
        if (this.options.plugins.legend != null) {
            // Let wrapped legend rows determine their full height independently of the band.
            this.options.plugins.legend.maxHeight = Number.POSITIVE_INFINITY;
        }
        this.options.plugins.scheduleModeBand = {
            enabled: true,
            bandHeight: ScheduleModeChartComponent.BAND_HEIGHT,
            onHeightChange: (height) => {
                if (height !== this.modeChartHeight()) {
                    // Resize the container after Chart.js has finished its layout.
                    queueMicrotask(() => this.modeChartHeight.set(height));
                }
            },
        };
        // Show the active mode (1) for the hovered time slot instead of selecting by dataset order.
        this.options.plugins.tooltip.filter = (item) => item.raw === 1;
        this.options.plugins.tooltip.callbacks.title = (items) => {
            const entry = this.data.data24h[items[0]?.dataIndex ?? -1];
            return entry == null ? "" : (DateTimeUtils.format(entry.timestamp, DateTimeFormats.HOUR_MINUTE) ?? "");
        };
        this.options.plugins.tooltip.callbacks.labelColor = (item) => {
            const color = item.dataset.borderColor;
            if (typeof color !== "string") {
                throw new Error("Mode chart dataset color is not defined");
            }
            return { backgroundColor: color, borderColor: color, borderWidth: 0 };
        };
    }

    protected override fillDatasets(): ChartDataset[] {
        const historyIndex = this.data.data24hLastHistoryIndex;
        const backgroundRgb = getComputedStyle(document.documentElement)
            .getPropertyValue("--ion-background-color-rgb")
            .trim();
        if (backgroundRgb.length === 0) {
            throw new Error("Mode chart background color is not defined");
        }
        const backgroundColor = `rgb(${backgroundRgb})`;

        return this.buildModeDatasets().map<ChartDataset>((dataset) => {
            const hoverColor = darkenColor(dataset.color);
            const forecastPattern = createForecastPattern(dataset.color, backgroundColor);
            const hoverForecastPattern = createForecastPattern(hoverColor, backgroundColor);

            return {
                type: "bar",
                label: dataset.label,
                data: dataset.data,
                hidden: false,
                order: 1,
                yAxisID: ChartAxis.LEFT,
                stack: "mode",
                barThickness: "flex",
                inflateAmount: "auto",
                barPercentage: 1,
                categoryPercentage: 1,
                backgroundColor: ({ dataIndex }) => (dataIndex > historyIndex ? forecastPattern : dataset.color),
                hoverBackgroundColor: ({ dataIndex }) => (dataIndex > historyIndex ? hoverForecastPattern : hoverColor),
                borderColor: dataset.color,
                hoverBorderColor: hoverColor,
                borderWidth: 0,
                hoverBorderWidth: 0,
            };
        });
    }

    protected override getTooltipLabelCallback(): (item: TooltipItem<"bar">) => string {
        return (item) => item.dataset.label ?? "";
    }

    protected abstract buildModeDatasets(): ModeDataset[];
}
