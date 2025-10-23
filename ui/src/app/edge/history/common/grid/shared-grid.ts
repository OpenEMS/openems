// @ts-strict-ignore
import { TranslateService } from "@ngx-translate/core";
import { BoxAnnotationOptions } from "chartjs-plugin-annotation";
import { RippleControlReceiverRestrictionLevel } from "src/app/shared/shared";
import { ChartAnnotationState } from "src/app/shared/type/general";
import { ChartAxis, HistoryUtils, YAxisType } from "src/app/shared/utils/utils";

/** Converts Ripple Control Receiver(RCR) values into corresponding restriction levels.
 *
 * The function maps received value indexes(0, 1, 2, 3) to predefined restriction levels
 * from the { @link RippleControlReceiverRestrictionLevel } enum.
 *
 * If the input value does not match any predefined mapping, it is returned unchanged.
 *
 * @param value - The received RCR value index(e.g., 0, 1, 2, 3 or null).
 * @returns The mapped restriction level, '0' for full restriction, 'null' for no restriction,
 * or the original value if no mapping applies.
*/
export function transformRcrValues(value: number): number | null {
    if (value === null) {
        return null;
    }
    const restrictionMapping: Record<number, number | null> = {
        [RippleControlReceiverRestrictionLevel.NO_RESTRICTION]: 0,
        [RippleControlReceiverRestrictionLevel.ZERO_PERCENT]: 100 - 0,
        [RippleControlReceiverRestrictionLevel.THIRTY_PERCENT]: 100 - 30,
        [RippleControlReceiverRestrictionLevel.SIXTY_PERCENT]: 100 - 60,
    };
    return restrictionMapping[value] ?? null;
}

export function normalizeRcrValue(value: number | null): number | null {
    if (value === null) {
        return null;
    }
    const scaledValue = value * 1000;
    if (scaledValue === 0) {
        return null;
    }
    return transformRcrValues(scaledValue);
}

export function getRestrictionEpochs(
    chartData: number[],
    type: "14a" | "rcr" | "offGrid",
    yAxes?: HistoryUtils.yAxes
): { start: number; end: number; yMin?: number; yMax?: number }[] {
    switch (type) {
        case "rcr":
            return getRcrRestrictionEpochs(chartData, yAxes);
        case "14a":
        case "offGrid":
        default:
            return getBinaryRestrictionEpochs(chartData);
    }
}

/**
 * Highlights active values as chartJS box annotations.
 * @param data - Array of active time values for limitations/off-grid periods.
 * @param labels - Array of ISO timestamps for the current day, split into 5-minute intervals.
 * @param getRestrictionEpochs - the corresponding function to fetch restriction epoch data.
 * @returns An array of chartJS box annotation objects.
*/
export function getAnnotations(
    data: number[],
    labels: Date[],
    type: "14a" | "rcr",
    yAxisId: ChartAxis,
    yAxes?: HistoryUtils.yAxes
): BoxAnnotationOptions[] {
    if (!data) { return []; }

    const limitationEpochs = getRestrictionEpochs(data, type, yAxes);

    return limitationEpochs.map(e => ({
        type: "box",
        borderWidth: 1,
        xScaleID: "x",
        yMin: e.yMin ?? null,
        yMax: e.yMax ?? null,
        xMin: labels[e.start].toISOString(),
        xMax: labels[e.end].toISOString(),
        yScaleID: yAxisId,
    }));
}

/**
 * Adds ControllerEssLimiter14a to chart axis array.
 * @param yAxes the yAxes.
 * @param chartType the chart type.
 * @param translate the translate service.
*/
export function createLimiter14aAxis(
    chartType: "bar" | "line",
    translate: TranslateService
): HistoryUtils.yAxes {
    const axis: HistoryUtils.yAxes = chartType === "bar"
        ? {
            unit: YAxisType.TIME,
            position: "right",
            yAxisId: ChartAxis.RIGHT,
            displayGrid: false,
        }
        : {
            unit: YAxisType.RELAY,
            position: "right",
            yAxisId: ChartAxis.RIGHT,
            customTitle: translate.instant("General.state"),
            displayGrid: false,
        };

    return axis;
}

/**
 * Adds ControllerEssRippleControlReceiver to chart.
 * @param yAxes the yAxes.
 * @param chartType the chart type.
 * @param translate the translate service.
*/
export function createRcrAxis(
    chartType: "bar" | "line"
): HistoryUtils.yAxes {
    const axis: HistoryUtils.yAxes = chartType === "bar"
        ? {
            unit: YAxisType.TIME,
            position: "right",
            yAxisId: ChartAxis.RIGHT_2,
            displayGrid: false,
        }
        : {
            unit: YAxisType.RESTRICTION,
            position: "right",
            yAxisId: ChartAxis.RIGHT_2,
            displayGrid: false,
        };

    return axis;
}

/**
 * Processes every received data into their respective format.
 * @param data the channel data.
 * @param chartType the chart type.
 * @returns the corresponding data.
*/
export function processRestrictionDatasets(
    data: HistoryUtils.ChannelData,
    chartType: string
): {
    restrictionData14a: number[] | null;
    offGridData: number[] | null;
    restrictionDataRcr: number[] | null;
} {
    let restrictionData14a: number[] | null = null;
    let offGridData: number[] | null = null;
    let restrictionDataRcr: number[] | null = null;

    if (chartType === "line") {
        restrictionData14a = data["Restriction14a"]?.map((value: number) =>
            value > 0 ? ChartAnnotationState.ON : ChartAnnotationState.OFF_HIDDEN
        ) ?? null;

        offGridData = data["OffGrid"]?.map((value: number) =>
            value * 1000 > 1 ? ChartAnnotationState.ON : ChartAnnotationState.OFF_HIDDEN
        ) ?? null;

        restrictionDataRcr = data["RestrictionRcr"]?.map((value: number | null) =>
            normalizeRcrValue(value)
        ) ?? null;
    } else {
        restrictionData14a = data["Restriction14a"]?.map((value: number) => value * 1000) ?? null;
        offGridData = data["OffGrid"]?.map((value: number) => value * 1000) ?? null;
        restrictionDataRcr = data["RestrictionRcr"]?.map((value: number) => value * 1000) ?? null;
    }

    return { restrictionData14a, offGridData, restrictionDataRcr };
}

export function buildAnnotations(
    data: number[],
    labels: Date[],
    type: "14a" | "rcr" | "offGrid",
    yAxisId: ChartAxis,
    yAxes?: HistoryUtils.yAxes
): BoxAnnotationOptions[] {
    if (!data) { return []; }

    const limitationEpochs = getRestrictionEpochs(data, type, yAxes);

    return limitationEpochs.map(e => ({
        type: "box",
        borderWidth: 1,
        xScaleID: "x",
        yMin: e.yMin ?? null,
        yMax: e.yMax ?? null,
        xMin: labels[e.start].toISOString(),
        xMax: labels[e.end].toISOString(),
        yScaleID: yAxisId,
    }));
}

// Checks if the controller has received at least one valid value this day.
export function hasData(enabled: boolean, data: number[] | null): boolean {
    return enabled && Array.isArray(data) && data.some(value => Number.isFinite(value));
}

/**
 * Identifies restriction periods (epochs) in a Ripple Control Receiver (RCR) signal.
 *
 * The input rawData contains encoded RCR values, which are transformed into standardized
 * restriction levels using transformRcrValues(). These levels typically represent:
 * - null → no restriction
 * - 0, 1, 2, ... → increasing levels of restriction (e.g., 0%, 30%, 60%)
 *
 * An epoch starts when a restriction level changes (e.g., from null to 0, or 1 to 2),
 * and ends when the signal becomes null (i.e., restriction lifted).
 *
 * Example:
 * Input:  [null, 0, 0, 1, 1, null, null, 2, 2]
 * Output: [
 *   { start: 1, end: 2 }, // Level 0
 *   { start: 3, end: 4 }, // Level 1
 *   { start: 7, end: 8 }  // Level 2
 * ]
 *
 * @param rawData - Array of raw RCR values over time.
 * @param yAxes - Required to indicate whether RCR values should be evaluated.
 *                If not provided, the method returns an empty result.
 * @returns An array of { start, end } objects representing active restriction periods.
 */
export function getRcrRestrictionEpochs(
    rawData: number[],
    yAxes?: HistoryUtils.yAxes
): { start: number; end: number; yMin?: number; yMax?: number }[] {
    if (!yAxes) { return []; }

    const transformed = rawData.map(transformRcrValues);

    return transformed.reduce<{ start: number; end: number; yMin?: number; yMax?: number }[]>((epochs, current, index, arr) => {
        const previous = index > 0 ? arr[index - 1] : null;

        const isNewEpoch = current !== null && current !== previous;
        const isEndOfEpoch = current === null && previous !== null;

        if (isNewEpoch) {
            epochs.push({ start: index, end: -1 });
        }

        if (isEndOfEpoch && epochs.length > 0 && epochs[epochs.length - 1].end === -1) {
            epochs[epochs.length - 1].end = index - 1;
        }

        return epochs;
    }, []).map(epoch => ({
        ...epoch,
        end: epoch.end === -1 ? rawData.length - 1 : epoch.end,
    }));
}

/**
 * Identifies active time periods (epochs) in a binary data series (e.g., relay ON/OFF states)
 * and returns them as time spans suitable for ChartJS box annotations.
 *
 * The input data represents a historic time series where each value indicates
 * the relay state at a specific timestamp:
 * - 1 (ChartAnnotationState.ON) → restriction active
 * - 0 or null (ChartAnnotationState.OFF) → restriction inactive
 *
 * An epoch starts when a value changes from OFF to ON and ends when the value
 * switches back to OFF or becomes null. These epochs are collected as
 * { start, end } index ranges.
 *
 * Example:
 * Input:  [0, 1, 1, 1, 0, 0, 1, 1]
 * Output: [{ start: 1, end: 3 }, { start: 6, end: 7 }]
 *
 * @param data - Array of binary values representing the time series (1 for ON, 0/null for OFF)
 * @returns An array of time span objects with start and end indices for each ON-period
 */
export function getBinaryRestrictionEpochs(data: number[]): { start: number; end: number; yMin?: number; yMax?: number }[] {
    const result: { start: number; end: number; yMin?: number; yMax?: number }[] = [];
    let start: number | null = null;

    data.forEach((val, idx) => {
        if (val === ChartAnnotationState.ON && start === null) {
            start = idx;
        } else if ((val === ChartAnnotationState.OFF || val === null) && start !== null) {
            result.push({ start, end: idx - 1 });
            start = null;
        }
    });

    if (start !== null) {
        result.push({ start, end: data.length - 1 });
    }

    return result;
}

