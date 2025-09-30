// @ts-strict-ignore
import { FormGroup } from "@angular/forms";
import * as Chart from "CHART.JS";
import { ChartDataset } from "CHART.JS";
import { QueryHistoricTimeseriesDataResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse";
import { QueryHistoricTimeseriesEnergyPerPeriodResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyPerPeriodResponse";
import { CurrentData, EdgeConfig } from "src/app/shared/shared";
import { FormUtils } from "src/app/shared/utils/form/FORM.UTILS";
import { ObjectUtils } from "src/app/shared/utils/object/OBJECT.UTILS";
import { HistoryUtils } from "src/app/shared/utils/utils";

import { AbstractHistoryChart } from "../../chart/abstracthistorychart";
import { XAxisType } from "../../chart/CHART.CONSTANTS";
import { ButtonLabel } from "../../modal/modal-button/modal-button";
import { ModalLineComponent, TextIndentation } from "../../modal/modal-line/modal-line";
import { Converter } from "../converter";
import { OeFormlyField, OeFormlyView } from "../oe-formly-component";
import { OeTester } from "./common";
import { TestContext } from "./UTILS.SPEC";

export class OeFormlyViewTester {

  public static apply(view: OeFormlyView, context: OE_FORMLY_VIEW_TESTER.CONTEXT, fg: FormGroup | null = null): OE_FORMLY_VIEW_TESTER.VIEW {
    return {
      title: VIEW.TITLE,
      lines: VIEW.LINES
        .map(line => OE_FORMLY_VIEW_TESTER.APPLY_FIELD(line, context, fg))
        .filter(line => line),
    };
  }

  private static applyField(field: OeFormlyField, context: OE_FORMLY_VIEW_TESTER.CONTEXT, fg: FormGroup): OE_FORMLY_VIEW_TESTER.FIELD {
    switch (FIELD.TYPE) {
      /**
       * OE_FORMLY_FIELD.LINE
       */
      case "children-line": {
        const tmp = OE_FORMLY_VIEW_TESTER.APPLY_LINE_WITH_CHILDREN(field, context);

        // Prepare result
        const result: OE_FORMLY_VIEW_TESTER.FIELD.CHILDREN_LINE = {
          type: FIELD.TYPE,
          name: TMP.VALUE,
        };

        // Apply properties if available
        if (FIELD.INDENTATION) {
          RESULT.INDENTATION = FIELD.INDENTATION;
        }

        // Recursive call for children
        if (FIELD.CHILDREN) {
          RESULT.CHILDREN = FIELD.CHILDREN
            ?.map(child => OE_FORMLY_VIEW_TESTER.APPLY_FIELD(child, context, null));
        }

        return result;
      }

      case "channel-line": {
        const tmp = OE_FORMLY_VIEW_TESTER.APPLY_LINE_OR_ITEM(field, context);
        if (tmp == null) {
          return null; // filter did not pass
        }

        // Read or generate name
        let name: string;
        if (typeof (FIELD.NAME) === "function") {
          name = FIELD.NAME(TMP.RAW_VALUE);
        } else {
          name = FIELD.NAME;
        }

        // Prepare result
        const result: OE_FORMLY_VIEW_TESTER.FIELD.CHANNEL_LINE = {
          type: FIELD.TYPE,
          name: name,
        };

        // Apply properties if available
        if (TMP.VALUE !== null) {
          RESULT.VALUE = TMP.VALUE;
        }
        if (FIELD.INDENTATION) {
          RESULT.INDENTATION = FIELD.INDENTATION;
        }

        // Recursive call for children

        return result;
      }


      /**
       * {@link OE_FORMLY_FIELD.VALUE_LINE_FROM_MULTIPLE_CHANNELS}
       */
      case "value-from-channels-line": {
        const tmp = OE_FORMLY_VIEW_TESTER.APPLY_VALUE_LINE_FROM_CHANNELS(field, context);
        if (tmp == null) {
          return null; // filter did not pass
        }

        // Read or generate name
        const name: string = FIELD.NAME;

        // Prepare result
        const result: OE_FORMLY_VIEW_TESTER.FIELD.VALUE_LINE = {
          type: FIELD.TYPE,
          name: name,
        };

        // Apply properties if available
        if (TMP.VALUE !== null) {
          RESULT.VALUE = TMP.VALUE;
        }
        if (FIELD.INDENTATION) {
          RESULT.INDENTATION = FIELD.INDENTATION;
        }

        return result;
      }

      /**
       * OE_FORMLY_FIELD.ITEM
       */
      case "item": {
        const tmp = OE_FORMLY_VIEW_TESTER.APPLY_LINE_OR_ITEM(field, context);
        if (tmp == null) {
          return null; // filter did not pass
        }

        return {
          type: FIELD.TYPE,
          value: TMP.VALUE,
        };
      }

      /**
       * OE_FORMLY_FIELD.INFO
       */
      case "info-line": {
        return {
          type: FIELD.TYPE,
          name: FIELD.NAME,
        };
      }

      /**
       * OE_FORMLY_FIELD.HORIZONTAL
       */
      case "horizontal-line": {
        return {
          type: FIELD.TYPE,
        };
      }
      /**
       * {@link OE_FORMLY_FIELD.BUTTONS_FROM_FORM_CONTROL_LINE}
       */
      case "buttons-from-form-control-line": {
        return {
          type: "buttons-from-form-control-line",
          name: FIELD.NAME,
          controlName: FIELD.CONTROL_NAME,
          buttons: FIELD.BUTTONS,
        };
      }

      /**
       * {@link OE_FORMLY_FIELD.RANGE_BUTTON_FROM_FORM_CONTROL_LINE}
       */
      case "range-button-from-form-control-line": {

        // Exlude properties, only testable per ui interaction test
        const properties = OBJECT_UTILS.EXCLUDE_PROPERTIES(FIELD.PROPERTIES, ["pinFormatter", "tickFormatter"]);
        const expectedValue = FORM_UTILS.FIND_FORM_CONTROLS_VALUE_SAFELY(fg, FIELD.CONTROL_NAME) ?? null;

        return {
          type: "range-button-from-form-control-line",
          controlName: FIELD.CONTROL_NAME,
          expectedValue: expectedValue,
          properties: properties,
        };
      }
    }
  }

  /**
   * Common method for Line and Item as they share some fields and logic.
   *
   * @param field the field
   * @param context the test context
   * @returns result or null
   */
  private static applyLineOrItem(field: OE_FORMLY_FIELD.CHANNEL_LINE | OE_FORMLY_FIELD.ITEM, context: OE_FORMLY_VIEW_TESTER.CONTEXT):
   /* result */ { rawValue: number | null, value: string }
   /* filter did not pass */ | null {

    // Read value from channels
    const rawValue = FIELD.CHANNEL && FIELD.CHANNEL in context ? context[FIELD.CHANNEL] : null;

    // Apply filter
    if (FIELD.FILTER && FIELD.FILTER(rawValue) === false) {
      return null;
    }

    // Apply converter
    const value: string = FIELD.CONVERTER
      ? FIELD.CONVERTER(rawValue)
      : rawValue === null ? null : "" + rawValue;

    return {
      rawValue: rawValue,
      value: value,
    };
  }
}

export namespace OeChartTester {

  export type Context = {
    energyChannel: { [id: string]: number[] }[]
    powerChannel: { [id: string]: number[] }[]
  }[];

  export type View = {
    datasets: {
      data: OE_CHART_TESTER.DATASET.DATA[],
      labels: OE_CHART_TESTER.DATASET.LEGEND_LABEL,
      options: OE_CHART_TESTER.DATASET.OPTION
    }
  };

  export type Dataset =
    | DATASET.DATA
    | DATASET.LEGEND_LABEL
    | DATASET.OPTION;

  export namespace Dataset {

    export type Data = {
      type: "data",
      label: string | Converter,
      value: (number | null)[]
    };

    export type LegendLabel = {
      type: "label",
      timestamps: Date[]
    };
    export type Option = {
      type: "option",
      options: CHART.CHART_OPTIONS
    };
  }
}

export class OeChartTester {

  public static apply(chartData: HISTORY_UTILS.CHART_DATA, chartType: "line" | "bar", channels: OE_TESTER.TYPES.CHANNELS, testContext: TestContext, config: EdgeConfig, xAxisScalingType: XAxisType = XAXIS_TYPE.TIMESERIES): OE_CHART_TESTER.VIEW {

    const channelData = OE_CHART_TESTER.GET_CHANNEL_DATA_BY_CHARTTYPE(chartType, channels);

    // Set historyPeriod manually with passed timestamps
    TEST_CONTEXT.SERVICE.HISTORY_PERIOD.NEXT({
      from: new Date(CHANNEL_DATA.RESULT.TIMESTAMPS[0] ?? 0),
      to: new Date(CHANNEL_DATA.RESULT.TIMESTAMPS.REVERSE()[0] ?? 0),
      getText: () => TEST_CONTEXT.SERVICE.HISTORY_PERIOD.VALUE.GET_TEXT(TEST_CONTEXT.TRANSLATE, TEST_CONTEXT.SERVICE),
      isWeekOrDay: () => TEST_CONTEXT.SERVICE.HISTORY_PERIOD.VALUE.IS_WEEK_OR_DAY(),
    });

    // Fill Data
    const configuration = ABSTRACT_HISTORY_CHART.FILL_CHART(chartType, chartData, channelData, CHANNELS.ENERGY_CHANNEL_WITH_VALUES);
    const data: OE_CHART_TESTER.DATASET.DATA[] = OE_CHART_TESTER.CONVERT_CHART_DATASETS_TO_DATASETS(CONFIGURATION.DATASETS);
    const labels: OE_CHART_TESTER.DATASET.LEGEND_LABEL = OE_CHART_TESTER.CONVERT_CHART_LABELS_TO_LEGEND_LABELS(CONFIGURATION.LABELS);
    const options: OE_CHART_TESTER.DATASET.OPTION = OE_CHART_TESTER.CONVERT_CHART_DATA_TO_OPTIONS(chartData, chartType, testContext, channels, TEST_CONTEXT.TRANSLATE.CURRENT_LANG, config, CONFIGURATION.DATASETS, xAxisScalingType, CONFIGURATION.LABELS);

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
   * @param labels the labels
   * @returns legendlabels
   */
  public static convertChartLabelsToLegendLabels(labels: Date[]): OE_CHART_TESTER.DATASET.LEGEND_LABEL {
    return {
      type: "label",
      timestamps: labels,
    };
  }

  /**
   * Converts chartData to Dataset
   *
   * @param datasets the datasets
   * @returns data from a chartData dataset
   */
  public static convertChartDatasetsToDatasets(datasets: ChartDataset[]): OE_CHART_TESTER.DATASET.DATA[] {
    const fields: OE_CHART_TESTER.DATASET.DATA[] = [];

    for (const dataset of datasets) {
      FIELDS.PUSH(
        {
          type: "data",
          label: DATASET.LABEL,
          value: DATASET.DATA as number[],
        });
    }

    return fields;
  }

  /**
   * Converts chartData to chartOptions
   *
   * @param chartObject the chartObject
   * @param chartType the chartType
   * @param testContext the testContext
   * @param channels the channels
   * @returns dataset options
   */
  public static convertChartDataToOptions(chartData: HISTORY_UTILS.CHART_DATA, chartType: "line" | "bar", testContext: TestContext, channels: OE_TESTER.TYPES.CHANNELS, locale: string, config: EdgeConfig, datasets: CHART.CHART_DATASET[], xAxisType: XAxisType = XAXIS_TYPE.TIMESERIES, labels: (Date | string)[] = []): OE_CHART_TESTER.DATASET.OPTION {

    const channelData: QueryHistoricTimeseriesDataResponse | QueryHistoricTimeseriesEnergyPerPeriodResponse = OE_CHART_TESTER.GET_CHANNEL_DATA_BY_CHARTTYPE(chartType, channels);

    const displayValues = CHART_DATA.OUTPUT(CHANNEL_DATA.RESULT.DATA);
    const legendOptions: any[] = [];

    DISPLAY_VALUES.FOR_EACH(displayValue => {
      const yAxis = CHART_DATA.Y_AXES.FIND(yaxis => yaxis?.yAxisId == (displayValue?.yAxisId ?? CHART_DATA.Y_AXES[0].yAxisId));
      const label = ABSTRACT_HISTORY_CHART.GET_TOOLTIPS_LABEL_NAME(DISPLAY_VALUE.NAME, yAxis?.unit, typeof DISPLAY_VALUE.NAME_SUFFIX == "function" ? DISPLAY_VALUE.NAME_SUFFIX(CHANNELS.ENERGY_CHANNEL_WITH_VALUES) : null);
      LEGEND_OPTIONS.PUSH(ABSTRACT_HISTORY_CHART.GET_LEGEND_OPTIONS(label, displayValue));
    });

    let options: CHART.CHART_OPTIONS = ABSTRACT_HISTORY_CHART.GET_OPTIONS(chartData, chartType, TEST_CONTEXT.SERVICE, TEST_CONTEXT.TRANSLATE, legendOptions, CHANNEL_DATA.RESULT, config, datasets, xAxisType, labels);
    options = prepareOptionsForTesting(options, chartData);

    return {
      type: "option",
      options: options,
    };
  }

  private static getChannelDataByCharttype(chartType: "line" | "bar", channels: OE_TESTER.TYPES.CHANNELS): QueryHistoricTimeseriesEnergyPerPeriodResponse | QueryHistoricTimeseriesDataResponse {
    switch (chartType) {
      case "line":
        return CHANNELS.DATA_CHANNEL_WITH_VALUES;
      case "bar":
        return CHANNELS.ENERGY_PER_PERIOD_CHANNEL_WITH_VALUES;
    }
  }
}

export namespace OeFormlyViewTester {

  export type Context = { [id: string]: number | null };

  export type View = {
    title: string,
    lines: Field[]
  };

  export type Field =
    | FIELD.INFO_LINE
    | FIELD.ITEM
    | FIELD.CHANNEL_LINE
    | FIELD.CHILDREN_LINE
    | FIELD.HORIZONTAL_LINE
    | FIELD.VALUE_LINE
    | FIELD.BUTTONS_FROM_FORM_CONTROL_LINE
    | FIELD.RANGE_BUTTON_FROM_FORM_CONTROL_LINE
    ;

  export namespace Field {

    export type InfoLine = {
      type: "info-line",
      name: string
    };

    export type Item = {
      type: "item",
      value: string
    };

    export type ChannelLine = {
      type: "channel-line",
      name: string,
      value?: string,
      indentation?: TextIndentation,
    };

    export type ValueLine = {
      type: "value-from-channels-line",
      name: string,
      value?: string,
      indentation?: TextIndentation,
    };

    export type ChildrenLine = {
      type: "children-line",
      name: string,
      indentation?: TextIndentation,
      children?: Field[]
    };

    export type HorizontalLine = {
      type: "horizontal-line",
    };
    export type ButtonsFromFormControlLine = {
      type: "buttons-from-form-control-line",
      name: string,
      controlName: string,
      buttons: ButtonLabel[],
    };
    export type RangeButtonFromFormControlLine<T = any> = {
      type: "range-button-from-form-control-line",
      controlName: string,
      expectedValue: T,
      properties: Partial<Extract<ModalLineComponent["control"], { type: "RANGE" }>["properties"]>,
    };
  }

  export function applyLineWithChildren(field: OE_FORMLY_FIELD.CHILDREN_LINE, context: Context): { rawValue: number | null, value: string }
    | null {

    let value: string | null = null;
    let rawValue: number | null = null;

    if (typeof FIELD.NAME == "object") {
      rawValue = typeof FIELD.NAME == "object" ? (FIELD.NAME.CHANNEL.TO_STRING() in context ? context[FIELD.NAME.CHANNEL.TO_STRING()] : null) : null;
      value = FIELD.NAME.CONVERTER(rawValue);
    }

    if (typeof (FIELD.NAME) === "string") {
      value = FIELD.NAME;
    }

    return {
      rawValue: rawValue,
      value: value,
    };
  }

  export function applyValueLineFromChannels(field: OE_FORMLY_FIELD.VALUE_FROM_CHANNELS_LINE, context: Context): { rawValues: number[] | null, value: string } {

    // Read values from channels
    const rawValues = FIELD.CHANNELS_TO_SUBSCRIBE.MAP(channel => channel && CHANNEL.TO_STRING() in context ? context[CHANNEL.TO_STRING()] : null);

    // Apply filter
    if (FIELD.FILTER && FIELD.FILTER(rawValues) === false) {
      return null;
    }

    const currentData: CurrentData = { allComponents: context };

    // Apply converter
    const value: string = FIELD.VALUE
      ? FIELD.VALUE(currentData)
      : rawValues === null ? null : "";

    return {
      rawValues: rawValues,
      value: value,
    };
  }
}

/** Exclude properties that dont need to be tested  */
function prepareOptionsForTesting(options: CHART.CHART_OPTIONS, chartData: HISTORY_UTILS.CHART_DATA): CHART.CHART_OPTIONS {
  OPTIONS.SCALES["x"]["ticks"] = OBJECT_UTILS.EXCLUDE_PROPERTIES(OPTIONS.SCALES["x"]["ticks"] as CHART.RADIAL_TICK_OPTIONS, ["color"]);
  OPTIONS.ELEMENTS.POINT.RADIUS = 0;
  CHART_DATA.Y_AXES.FILTER(axis => AXIS.UNIT != null).forEach(axis => {

    // Remove custom scale calculations from unittest, seperate unittest existing
    OPTIONS.SCALES[AXIS.Y_AXIS_ID] = OBJECT_UTILS.EXCLUDE_PROPERTIES(OPTIONS.SCALES[AXIS.Y_AXIS_ID], ["min", "max"]) as CHART.SCALE_OPTIONS_BY_TYPE<"radialLinear" | keyof CHART.CARTESIAN_SCALE_TYPE_REGISTRY>;
    OPTIONS.SCALES[AXIS.Y_AXIS_ID].ticks = OBJECT_UTILS.EXCLUDE_PROPERTIES(OPTIONS.SCALES[AXIS.Y_AXIS_ID].ticks as CHART.RADIAL_TICK_OPTIONS, ["stepSize"]);
    OPTIONS.SCALES[AXIS.Y_AXIS_ID]["title"] = OBJECT_UTILS.EXCLUDE_PROPERTIES(OPTIONS.SCALES[AXIS.Y_AXIS_ID]["title"] as CHART.RADIAL_TICK_OPTIONS, ["color"]);
  });

  delete OPTIONS.PLUGINS.TOOLTIP.CARET_PADDING;
  delete OPTIONS.LAYOUT;
  OPTIONS.PLUGINS.TOOLTIP = OBJECT_UTILS.EXCLUDE_PROPERTIES(OPTIONS.PLUGINS.TOOLTIP, ["boxHeight", "boxWidth", "boxPadding"]);
  OPTIONS.PLUGINS.LEGEND.LABELS = OBJECT_UTILS.EXCLUDE_PROPERTIES(OPTIONS.PLUGINS.LEGEND.LABELS, ["boxHeight", "boxWidth"]);

  return options;
}

