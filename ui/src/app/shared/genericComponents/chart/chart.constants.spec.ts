// @ts-strict-ignore
import { ChartDataset } from "chart.js";
import { History } from "src/app/edge/history/common/energy/chart/channels.spec";

import { ChartAxis, HistoryUtils, YAxisTitle } from "../../service/utils";
import { ChartConstants } from "./chart.constants";

describe('Chart constants', () => {
  it('#calculateStepSize', () => {
    expect(ChartConstants.calculateStepSize(0, 10)).toEqual(2.5);
    expect(ChartConstants.calculateStepSize(0, null)).toEqual(null);
    expect(ChartConstants.calculateStepSize(-10, 0)).toEqual(2.5);
    expect(ChartConstants.calculateStepSize(undefined, 0)).toEqual(null);

    // min higher than max
    expect(ChartConstants.calculateStepSize(10, 0)).toEqual(null);
  });

  it('#getScaleOptions', () => {
    const yAxis: HistoryUtils.yAxes = { unit: YAxisTitle.ENERGY, position: 'left', yAxisId: ChartAxis.LEFT };
    const datasets: ChartDataset[] = [
      {
        data: History.DAY.dataChannelWithValues.result.data['_sum/ConsumptionActivePower'],
        label: 'consumption',
        yAxisID: ChartAxis.LEFT,
      },
    ];

    expect(ChartConstants.getScaleOptions([], yAxis)).toEqual({ min: null, max: null, stepSize: null });
    expect(ChartConstants.getScaleOptions(datasets, yAxis)).toEqual({ min: 0, max: 1892, stepSize: 473 });
    expect(ChartConstants.getScaleOptions(null, yAxis)).toEqual(null);
    expect(ChartConstants.getScaleOptions(null, null)).toEqual(null);
  });
});
