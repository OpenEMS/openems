// @ts-strict-ignore
import { ChartDataset } from "CHART.JS";
import { History } from "src/app/edge/history/common/energy/chart/CHANNELS.SPEC";

import { ChartAxis, HistoryUtils, YAxisType } from "../../utils/utils";
import { ChartConstants } from "./CHART.CONSTANTS";

describe("Chart constants", () => {
  it("#calculateStepSize", () => {
    expect(CHART_CONSTANTS.CALCULATE_STEP_SIZE(0, 10)).toEqual(2);
    expect(CHART_CONSTANTS.CALCULATE_STEP_SIZE(0, null)).toEqual(null);
    expect(CHART_CONSTANTS.CALCULATE_STEP_SIZE(-10, 0)).toEqual(2);
    expect(CHART_CONSTANTS.CALCULATE_STEP_SIZE(undefined, 0)).toEqual(null);

    // min higher than max
    expect(CHART_CONSTANTS.CALCULATE_STEP_SIZE(10, 0)).toEqual(null);
  });

  it("#getScaleOptions", () => {
    const yAxis: HISTORY_UTILS.Y_AXES = { unit: YAXIS_TYPE.ENERGY, position: "left", yAxisId: CHART_AXIS.LEFT };
    const datasets: ChartDataset[] = [
      {
        data: HISTORY.DAY.DATA_CHANNEL_WITH_VALUES.RESULT.DATA["_sum/ConsumptionActivePower"],
        label: "consumption",
        yAxisID: CHART_AXIS.LEFT,
      },
    ];

    expect(CHART_CONSTANTS.GET_SCALE_OPTIONS([], yAxis, "line")).toEqual({ min: null, max: null, stepSize: null });
    expect(CHART_CONSTANTS.GET_SCALE_OPTIONS(datasets, yAxis, "line")).toEqual({ min: 0, max: 1892, stepSize: 378.4 });
    expect(CHART_CONSTANTS.GET_SCALE_OPTIONS(null, yAxis, "line")).toEqual({ min: null, max: null, stepSize: null });
    expect(CHART_CONSTANTS.GET_SCALE_OPTIONS(null, null, "line")).toEqual({ min: null, max: null, stepSize: null });
  });
});
