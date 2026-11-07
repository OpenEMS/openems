import { FormGroup } from "@angular/forms";
import * as Chart from "chart.js";
import { ChartDataset } from "chart.js";
import { QueryHistoricTimeseriesDataResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse";
import { QueryHistoricTimeseriesEnergyPerPeriodResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyPerPeriodResponse";
import { CurrentData, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { FormUtils } from "src/app/shared/utils/form/form.utils";
import { ObjectUtils } from "src/app/shared/utils/object/object-utils";
import { HistoryUtils, YAxisType } from "src/app/shared/utils/utils";

import { AbstractHistoryChart } from "../../chart/abstracthistorychart";
import { XAxisType } from "../../chart/chart.constants";
import { ButtonLabel } from "../../modal/modal-button/modal-button";
import { TextIndentation } from "../../modal/modal-line/modal-line";
import { OeImageComponent } from "../../oe-img/oe-img";
import { Converter } from "../converter";
import { OeFormlyField, OeFormlyView } from "../oe-formly-component";
import { OeTester } from "./common";
import { TestContext } from "./utils.spec";

export class OeFormlyViewTester {
    public static apply(
        view: OeFormlyView,
        context: OeFormlyViewTester.Context,
        fg: FormGroup | null = null,
    ): OeFormlyViewTester.View {
        return {
            title: view.title,
            lines: view.lines
                .map((line) => OeFormlyViewTester.applyField(line, context, fg))
                .filter((line): line is OeFormlyViewTester.Field => line != null),
        };
    }

    private static applyField(
        field: OeFormlyField,
        context: OeFormlyViewTester.Context,
        fg: FormGroup | null,
    ): OeFormlyViewTester.Field | null | undefined {
        switch (field.type) {
            /** OeFormlyField.Line */
            case "children-line": {
                const tmp = OeFormlyViewTester.applyLineWithChildren(field, context);
                if (tmp == null) {
                    return null; // filter did not pass
                }
                // Prepare result
                const result: OeFormlyViewTester.Field.ChildrenLine = {
                    type: field.type,
                    name: tmp.value,
                };

                // Apply properties if available
                if (field.indentation) {
                    result.indentation = field.indentation;
                }

                // Recursive call for children
                if (field.children) {
                    result.children = field.children?.map((child) =>
                        OeFormlyViewTester.applyField(child, context, null),
                    );
                }

                return result;
            }

            case "channel-line": {
                const tmp = OeFormlyViewTester.applyLineOrItem(field, context);
                if (tmp == null) {
                    return null; // filter did not pass
                }

                // Read or generate name
                let name: string;
                if (typeof field.name === "function") {
                    name = field.name(tmp.rawValue);
                } else {
                    name = field.name;
                }

                // Prepare result
                const result: OeFormlyViewTester.Field.ChannelLine = {
                    type: field.type,
                    name: name,
                };

                // Apply properties if available
                if (tmp.value !== null) {
                    result.value = tmp.value;
                }
                if (field.indentation) {
                    result.indentation = field.indentation;
                }

                // Recursive call for children

                return result;
            }

            /** {@link OeFormlyField.ValueLineFromMultipleChannels} */
            case "value-from-channels-line": {
                const tmp = OeFormlyViewTester.applyValueLineFromChannels(field, context);
                if (tmp == null) {
                    return null; // filter did not pass
                }

                // Read or generate name
                let name: string | undefined;
                if (typeof field.name === "object") {
                    const channelValue =
                        field.name.channel != null && field.name.channel.toString() in context
                            ? context[field.name.channel.toString()]
                            : null;

                    name = field.name.converter(channelValue);
                } else {
                    name = field.name;
                }

                // Prepare result
                const result: OeFormlyViewTester.Field.ValueLine = {
                    type: field.type,
                    name: name,
                };

                // Apply properties if available
                if (tmp.value !== null) {
                    result.value = tmp.value;
                }
                if (field.indentation) {
                    result.indentation = field.indentation;
                }

                return result;
            }

            /** OeFormlyField.Item */
            case "item": {
                const tmp = OeFormlyViewTester.applyLineOrItem(field, context);
                if (tmp == null) {
                    return null; // filter did not pass
                }

                return {
                    type: field.type,
                    value: tmp.value,
                };
            }

            /** OeFormlyField.Info */
            case "info-line": {
                return {
                    type: field.type,
                    name: field.name,
                };
            }

            /** OeFormlyField.SvgLine */
            case "image-line": {
                return {
                    type: field.type,
                    img: field.img,
                };
            }

            /** OeFormlyField.Horizontal */
            case "horizontal-line": {
                return {
                    type: field.type,
                };
            }

            case "component-line": {
                return {
                    type: field.type,
                    component: field.component.name,
                };
            }
            case "reorder-group-line": {
                return {
                    type: field.type,
                    controlName: field.controlName,
                    items: field.items,
                };
            }
            /** {@link OeFormlyField.ButtonsFromFormControlLine} */
            case "buttons-from-form-control-line": {
                return {
                    type: "buttons-from-form-control-line",
                    name: field.name,
                    controlName: field.controlName,
                    buttons: field.buttons,
                };
            }

            /** {@link OeFormlyField.RadioButtonsFromFormControlLine} */
            case "radio-buttons-from-form-control-line": {
                return {
                    type: "radio-buttons-from-form-control-line",
                    name: field.name,
                    controlName: field.controlName,
                    buttons: field.buttons,
                };
            }

            /** {@link OeFormlyField.RangeButtonFromFormControlLine} */
            case "range-button-from-form-control-line": {
                // Exlude properties, only testable per ui interaction test
                const properties = excludeDefinedProperties(field.properties, ["pinFormatter", "tickFormatter"]);
                const expectedValue = fg
                    ? (FormUtils.findFormControlsValueSafely(fg, field.controlName) ?? null)
                    : null;

                return {
                    type: "range-button-from-form-control-line",
                    controlName: field.controlName,
                    expectedValue: expectedValue,
                    properties: properties,
                };
            }

            case "input-line": {
                const expectedValue = fg
                    ? (FormUtils.findFormControlsValueSafely<string>(fg, field.controlName) ?? null)
                    : null;

                return {
                    type: "input-line",
                    name: field.name,
                    controlName: field.controlName,
                    properties: field.properties,
                    expectedValue: expectedValue,
                };
            }
        }
        return undefined;
    }

    /**
     * Common method for Line and Item as they share some fields and logic.
     *
     * @param field The field
     * @param context The test context
     * @returns Result or null
     */
    private static applyLineOrItem(
        field: OeFormlyField.ChannelLine | OeFormlyField.Item,
        context: OeFormlyViewTester.Context,
    ): OeFormlyViewTester.ChannelValue | null {
        // Read value from channels
        const rawValue = field.channel && field.channel in context ? context[field.channel] : null;

        // Apply filter
        if (field.filter && field.filter(rawValue) === false) {
            return null;
        }

        // Apply converter
        const value = field.converter ? field.converter(rawValue) : rawValue === null ? null : "" + rawValue;

        return {
            rawValue: rawValue,
            value: value,
        };
    }
}

export namespace OeChartTester {
    export type Context = {
        energyChannel: { [id: string]: number[] }[];
        powerChannel: { [id: string]: number[] }[];
    }[];

    export type View = {
        datasets: {
            data: OeChartTester.Dataset.Data[];
            labels: OeChartTester.Dataset.LegendLabel;
            options: OeChartTester.Dataset.Option;
        };
    };

    export type Dataset = Dataset.Data | Dataset.LegendLabel | Dataset.Option;

    export namespace Dataset {
        export type Data = {
            type: "data";
            label: string | Converter | undefined;
            value: ChartDataset["data"];
        };

        export type LegendLabel = {
            type: "label";
            timestamps: Date[];
        };
        export type Option = {
            type: "option";
            options: Chart.ChartOptions;
        };
    }
}

export class OeChartTester {
    public static apply(
        chartData: HistoryUtils.ChartData,
        chartType: "line" | "bar",
        channels: OeTester.Types.Channels,
        testContext: TestContext,
        config: EdgeConfig,
        xAxisScalingType: XAxisType = XAxisType.TIMESERIES,
    ): OeChartTester.View {
        const channelData = OeChartTester.getChannelDataByCharttype(chartType, channels);

        // Set historyPeriod manually with passed timestamps
        testContext.service.historyPeriod.next({
            from: new Date(channelData.result.timestamps[0] ?? 0),
            to: new Date(channelData.result.timestamps.reverse()[0] ?? 0),
            getText: () => testContext.service.historyPeriod.value.getText(testContext.translate, testContext.service),
            isWeekOrDay: () => testContext.service.historyPeriod.value.isWeekOrDay(),
        });

        // Fill Data
        const configuration = AbstractHistoryChart.fillChart(
            chartType,
            chartData,
            channelData,
            channels.energyChannelWithValues,
        );
        const data: OeChartTester.Dataset.Data[] = OeChartTester.convertChartDatasetsToDatasets(configuration.datasets);
        const labels: OeChartTester.Dataset.LegendLabel = OeChartTester.convertChartLabelsToLegendLabels(
            configuration.labels,
        );
        const options: OeChartTester.Dataset.Option = OeChartTester.convertChartDataToOptions(
            chartData,
            chartType,
            testContext,
            channels,
            testContext.translate.currentLang,
            config,
            configuration.datasets,
            xAxisScalingType,
            configuration.labels,
        );

        return {
            datasets: {
                data: data,
                labels: labels,
                options: options,
            },
        };
    }

    /**
     * Converts chartLabels to legendLabels
     *
     * @param labels The labels
     * @returns Legendlabels
     */
    public static convertChartLabelsToLegendLabels(labels: Date[]): OeChartTester.Dataset.LegendLabel {
        return {
            type: "label",
            timestamps: labels,
        };
    }

    /**
     * Converts chartData to Dataset
     *
     * @param datasets The datasets
     * @returns Data from a chartData dataset
     */
    public static convertChartDatasetsToDatasets(datasets: ChartDataset[]): OeChartTester.Dataset.Data[] {
        const fields: OeChartTester.Dataset.Data[] = [];

        for (const dataset of datasets) {
            fields.push({
                type: "data",
                label: dataset.label,
                value: dataset.data,
            });
        }

        return fields;
    }

    /**
     * Converts chartData to chartOptions
     *
     * @param chartObject The chartObject
     * @param chartType The chartType
     * @param testContext The testContext
     * @param channels The channels
     * @returns Dataset options
     */
    public static convertChartDataToOptions(
        chartData: HistoryUtils.ChartData,
        chartType: "line" | "bar",
        testContext: TestContext,
        channels: OeTester.Types.Channels,
        locale: string,
        config: EdgeConfig,
        datasets: Chart.ChartDataset[],
        xAxisType: XAxisType = XAxisType.TIMESERIES,
        labels: (Date | string)[] = [],
    ): OeChartTester.Dataset.Option {
        const channelData: QueryHistoricTimeseriesDataResponse | QueryHistoricTimeseriesEnergyPerPeriodResponse =
            OeChartTester.getChannelDataByCharttype(chartType, channels);

        const displayValues = chartData.output(channelData.result.data);
        const legendOptions: ReturnType<typeof AbstractHistoryChart.getLegendOptions>[] = [];

        displayValues.forEach((displayValue) => {
            const yAxis = chartData.yAxes.find(
                (yaxis) => yaxis?.yAxisId == (displayValue?.yAxisId ?? chartData.yAxes[0].yAxisId),
            );
            const label = AbstractHistoryChart.getTooltipsLabelName(
                displayValue.name,
                yAxis?.unit ?? YAxisType.NONE,
                typeof displayValue.nameSuffix == "function"
                    ? (displayValue.nameSuffix(channels.energyChannelWithValues) ?? undefined)
                    : undefined,
            );
            legendOptions.push(AbstractHistoryChart.getLegendOptions(label, displayValue));
        });

        let options: Chart.ChartOptions = AbstractHistoryChart.getOptions(
            chartData,
            chartType,
            testContext.service,
            testContext.translate,
            legendOptions,
            channelData.result,
            config,
            datasets,
            xAxisType,
            labels,
        );
        options = prepareOptionsForTesting(options, chartData);

        return {
            type: "option",
            options: options,
        };
    }

    private static getChannelDataByCharttype(
        chartType: "line" | "bar",
        channels: OeTester.Types.Channels,
    ): QueryHistoricTimeseriesEnergyPerPeriodResponse | QueryHistoricTimeseriesDataResponse {
        const channelData =
            chartType === "line" ? channels.dataChannelWithValues : channels.energyPerPeriodChannelWithValues;
        AssertionUtils.assertIsDefined(channelData, `Missing channel data for ${chartType} chart`);
        return channelData;
    }
}

export namespace OeFormlyViewTester {
    export type Context = { [id: string]: number | null };

    export type ChannelValue = { rawValue: number | null; value: string | null };

    export type ChannelsValue = {
        rawValues: (number | null)[];
        value: string | null;
        currentData: CurrentData;
    };

    export type View = {
        title: string;
        lines: Field[];
    };

    export type Field =
        | Field.InfoLine
        | Field.Item
        | Field.ChannelLine
        | Field.ChildrenLine
        | Field.HorizontalLine
        | Field.ValueLine
        | Field.ButtonsFromFormControlLine
        | Field.RangeButtonFromFormControlLine
        | Field.RadioButtonsFromFormControlLine
        | Field.ImageLine
        | Field.ComponentLine
        | Field.ReorderGroupLine
        | Field.InputLine;

    export namespace Field {
        export type InfoLine = {
            type: "info-line";
            name: OeFormlyField.InfoLine["name"];
        };

        export type Item = {
            type: "item";
            value: string | null;
        };

        export type ChannelLine = {
            type: "channel-line";
            name: string;
            value?: string;
            indentation?: TextIndentation;
        };

        export type ValueLine = {
            type: "value-from-channels-line";
            name: string | undefined;
            value?: string;
            indentation?: TextIndentation;
            filter?: (currentData: CurrentData) => boolean;
        };

        export type ChildrenLine = {
            type: "children-line";
            name: string | null;
            indentation?: TextIndentation;
            children?: (Field | null | undefined)[];
        };

        export type HorizontalLine = {
            type: "horizontal-line";
        };
        export type ButtonsFromFormControlLine = {
            type: "buttons-from-form-control-line";
            name: OeFormlyField.ButtonsFromFormControlLine["name"];
            controlName: string;
            buttons: ButtonLabel[];
        };

        export type RadioButtonsFromFormControlLine = {
            type: "radio-buttons-from-form-control-line";
            name: string;
            controlName: string;
            buttons: ButtonLabel[];
        };
        export type RangeButtonFromFormControlLine<T = unknown> = {
            type: "range-button-from-form-control-line";
            controlName: string;
            expectedValue: T;
            properties: OeFormlyField.RangeLineProperties;
        };
        export type ImageLine = {
            type: "image-line";
            img: OeImageComponent["img"];
        };
        export type ComponentLine = {
            type: "component-line";
            component: string;
            inputs?: Record<string, unknown>;
        };
        export type ReorderGroupLine = {
            type: "reorder-group-line";
            controlName: string;
            items: { value: string; label: string }[];
        };
        export type InputLine = {
            type: "input-line";
            name: string;
            controlName: string;
            properties: {
                unit: string;
            };
            expectedValue?: number | string | null;
        };
    }

    export function applyLineWithChildren(field: OeFormlyField.ChildrenLine, context: Context): ChannelValue | null {
        // Apply filter
        if (field.filter && field.channel) {
            const rawValues = field.channel.toString() in context ? context[field.channel.toString()] : null;
            const isVisible = field.filter(rawValues) ?? null;

            if (isVisible == false) {
                return null;
            }
        }

        let value: string | null = null;
        let rawValue: number | null = null;

        if (typeof field.name == "object") {
            rawValue =
                typeof field.name == "object"
                    ? field.name.channel.toString() in context
                        ? context[field.name.channel.toString()]
                        : null
                    : null;
            value = field.name.converter(rawValue);
        }

        if (typeof field.name === "string") {
            value = field.name;
        }

        return {
            rawValue: rawValue,
            value: value,
        };
    }

    export function applyValueLineFromChannels(
        field: OeFormlyField.ValueFromChannelsLine,
        context: Context,
    ): ChannelsValue | null {
        // Read values from channels
        const rawValues = field.channelsToSubscribe.map((channel) =>
            channel && channel.toString() in context ? context[channel.toString()] : null,
        );
        const currentData: CurrentData = { allComponents: context };

        // Apply filter
        if (field.filter && field.filter(currentData) === false) {
            return null;
        }

        // Apply converter
        const value = field.value ? field.value(currentData) : rawValues === null ? null : "";

        return {
            rawValues: rawValues,
            value: value,
            currentData: currentData,
        };
    }
}

/** Exclude properties that dont need to be tested */
function prepareOptionsForTesting(options: Chart.ChartOptions, chartData: HistoryUtils.ChartData): Chart.ChartOptions {
    const scales = options.scales;
    const point = options.elements?.point;
    const plugins = options.plugins;
    const tooltip = plugins?.tooltip;
    const legend = plugins?.legend;
    AssertionUtils.assertIsDefined(scales, "Missing chart scales");
    AssertionUtils.assertIsDefined(scales.x, "Missing chart x scale");
    AssertionUtils.assertIsDefined(point, "Missing chart point options");
    AssertionUtils.assertIsDefined(plugins, "Missing chart plugins");
    AssertionUtils.assertIsDefined(tooltip, "Missing chart tooltip options");
    AssertionUtils.assertIsDefined(legend, "Missing chart legend options");

    scales.x.ticks = excludeDefinedProperties(scales.x.ticks, ["color"]);
    point.radius = 0;
    chartData.yAxes
        .filter((axis) => axis.unit != null)
        .forEach((axis) => {
            // Remove custom scale calculations from unittest, seperate unittest existing
            // getYAxisOptions builds linear Y scales, even when the type is omitted.
            const generatedScale = scales[axis.yAxisId];
            AssertionUtils.assertIsDefined(generatedScale, "Missing chart y scale");
            AssertionUtils.assertIsTrue(
                generatedScale.type == null || generatedScale.type === "linear",
                "Expected a linear chart y scale",
            );
            const scale = excludeDefinedProperties(generatedScale as GeneratedYAxis, ["min", "max"]);
            scales[axis.yAxisId] = scale;
            scale.ticks = excludeDefinedProperties(scale.ticks, ["stepSize"]);
            scale.title = excludeDefinedProperties(scale.title, ["color"]);
        });

    plugins.tooltip = excludeDefinedProperties(tooltip, ["caretPadding", "boxHeight", "boxWidth", "boxPadding"]);
    legend.labels = excludeDefinedProperties(legend.labels, ["boxHeight", "boxWidth"]);

    const pluginsWithSync: typeof plugins & { syncChart?: unknown } = plugins;
    return {
        ...excludeDefinedProperties(options, ["layout", "indexAxis"]),
        plugins: excludeDefinedProperties(pluginsWithSync, ["syncChart"]),
    };
}

type GeneratedYAxis = Extract<NonNullable<NonNullable<Chart.ChartOptions["scales"]>[string]>, { type?: "linear" }>;

/** Reuses the shared exclusion helper for required generated option objects. */
function excludeDefinedProperties<T extends object, K extends keyof T>(
    obj: T | null | undefined,
    keys: K[],
): Omit<T, K> {
    AssertionUtils.assertIsDefined(obj, "Missing options required by chart or form tester");
    const result = ObjectUtils.excludeProperties(obj, keys);
    AssertionUtils.assertIsDefined(result);
    return result;
}
