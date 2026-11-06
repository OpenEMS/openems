import { ChartDataset } from "chart.js";
import { History } from "src/app/edge/history/common/energy/chart/channels.spec";

import { ChartAxis, HistoryUtils, YAxisType } from "../../utils/utils";
import { ChartConstants } from "./chart.constants";

describe("Chart constants", () => {
    it("#calculateStepSize", () => {
        expect(ChartConstants.calculateStepSize(0, 10)).toEqual(2);
        // Deliberately bypass the input types to exercise the missing-value guards.
        expect(ChartConstants.calculateStepSize(0, null!)).toEqual(null);
        expect(ChartConstants.calculateStepSize(-10, 0)).toEqual(2);
        expect(ChartConstants.calculateStepSize(undefined!, 0)).toEqual(null);

        // min higher than max
        expect(ChartConstants.calculateStepSize(10, 0)).toEqual(null);
    });

    it("#getScaleOptions", () => {
        const yAxis: HistoryUtils.yAxes = { unit: YAxisType.ENERGY, position: "left", yAxisId: ChartAxis.LEFT };
        const datasets: ChartDataset[] = [
            {
                data: History.DAY.dataChannelWithValues!.result.data["_sum/ConsumptionActivePower"],
                label: "consumption",
                yAxisID: ChartAxis.LEFT,
            },
        ];

        // Empty datasets produce nullable fields despite the narrower production return type.
        type ScaleOptions = { min: number | null; max: number | null; stepSize: number | null } | null;

        expect<ScaleOptions>(ChartConstants.getScaleOptions([], yAxis, "line")).toEqual({ min: null, max: null, stepSize: null });
        expect(ChartConstants.getScaleOptions(datasets, yAxis, "line")).toEqual({ min: 0, max: 1892, stepSize: 378.4 });
        // Deliberately bypass the input types to exercise missing-dataset handling.
        expect<ScaleOptions>(ChartConstants.getScaleOptions(null!, yAxis, "line")).toEqual({ min: null, max: null, stepSize: null });
        expect<ScaleOptions>(ChartConstants.getScaleOptions(null!, null!, "line")).toEqual({ min: null, max: null, stepSize: null });
    });
});
