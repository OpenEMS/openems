// @ts-strict-ignore
import { TimeUnit } from "chart.js";
import { DATA, LABELS } from "src/app/edge/history/common/energy/chart/chart.constants.spec";

import { DummyConfig } from "src/app/shared/components/edge/edgeconfig.spec";
import { OeTester } from "src/app/shared/components/shared/testing/common";
import { OeChartTester } from "src/app/shared/components/shared/testing/tester";
import { TestContext, TestingUtils } from "src/app/shared/components/shared/testing/utils.spec";
import { ChartAxis } from "src/app/shared/service/utils";
import { ChartConstants } from "src/app/shared/shared";
import { History, expectView } from "./chart.constants.spec";

describe("History Heatpump", () => {

    const config = DummyConfig.from(
        DummyConfig.Component.HEAT_PUMP_SG_READY("ctrlIoHeatPump0", "Wärmepumpe"),
    );

    let TEST_CONTEXT: TestContext;
    beforeEach(async () =>
        TEST_CONTEXT = await TestingUtils.sharedSetup(),
    );

    it("#getChartData()", () => {
        {
            // Line-Chart
            expectView(config, Object.values(config.components)[0], TEST_CONTEXT, "line", History.DAY,
                {
                    datasets: {
                        data: [
                            DATA("Zustand", [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 1, 1, 1, 1, 2, 1, 2, 2, 2, 1, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1]),
                        ],
                        labels: LABELS(History.DAY.dataChannelWithValues.result.timestamps),
                        options: LINE_CHART_OPTIONS("hour", "line", {
                            [ChartAxis.LEFT]: { scale: { beginAtZero: true } },
                        }),
                    },
                });
        }
        {
            // Line-Chart
            expectView(config, Object.values(config.components)[0], TEST_CONTEXT, "bar", History.MONTH,
                {
                    datasets: {
                        data: [
                            DATA("Sperre", [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, null]),
                            DATA("Normalbetrieb", [86353, 86353, 86354, 86353, 86352, 71428, 86353, 86352, 86354, 85866, 77027, 86353, 86352, 86352, 76953, 74238, 85915, 86352, 86352, 86354, 82752, 86353, 86352, 86353, 73268, 86352, 89950, 86353, 86352, 35419, null]),
                            DATA("Einschaltempfehlung", [0, 0, 0, 0, 0, 5396, 0, 0, 0, 0, 5395, 0, 0, 0, 3597, 3597, 0, 0, 0, 0, 1798, 0, 0, 0, 7194, 0, 0, 0, 0, 0, null]),
                            DATA("Einschaltbefehl", [0, 0, 0, 0, 0, 9519, 0, 0, 0, 0, 3921, 0, 0, 0, 5795, 8510, 0, 0, 0, 0, 1798, 0, 0, 0, 5883, 0, 0, 0, 0, 0, null]),
                        ],
                        labels: LABELS(History.MONTH.energyPerPeriodChannelWithValues.result.timestamps),
                        options: OeTester.ChartOptions.BAR_CHART_OPTIONS("day", "bar", {}, "Aktive Zeit"),
                    },
                });
        }
    });
});

export const LINE_CHART_OPTIONS = (period: string, chartType: "line" | "bar", options: { [key: string]: { scale: { min?: number, max?: number, beginAtZero?: boolean }, ticks?: { stepSize: number; min?: number, max?: number }; }; }): OeChartTester.Dataset.Option => ({
    type: "option",
    options: {
        "responsive": true, "maintainAspectRatio": false, "elements": { "point": { "radius": 0, "hitRadius": 0, "hoverRadius": 0 }, "line": { "stepped": false, "fill": true } }, "datasets": { "bar": {}, "line": {} }, "plugins": {
            "colors": { "enabled": false }, "legend": { "display": true, "position": "bottom", "labels": { "color": "" } }, "tooltip": { "intersect": false, "mode": "index", "callbacks": {}, "enabled": true }, "annotation": { "annotations": {} }, "datalabels": {
                display: false,
            },
        }, "scales": {
            "x": { "stacked": true, "offset": false, "type": "time", "ticks": { "source": "auto", "maxTicksLimit": 31 }, "bounds": "ticks", "adapters": { "date": { "locale": { "code": "de", "formatLong": {}, "localize": {}, "match": {}, "options": { "weekStartsOn": 1, "firstWeekContainsDate": 4 } } } }, "time": { "unit": period as TimeUnit, "displayFormats": { "datetime": "yyyy-MM-dd HH:mm:ss", "millisecond": "SSS [ms]", "second": "HH:mm:ss a", "minute": "HH:mm", "hour": "HH:00", "day": "dd", "week": "ll", "month": "MM", "quarter": "[Q]Q - YYYY", "year": "yyyy" } } },
            "left": {
                "stacked": false,
                "beginAtZero": false, ...options["left"]?.scale, ...(chartType === "line" ? { stacked: false } : {}), "title": { "text": "kW", "display": false, "padding": 5, "font": { "size": 11 } }, "position": "left", "grid": { "display": true },
                "ticks": {
                    ...options["left"]?.ticks,
                    "color": "",
                    "padding": 5,
                    "maxTicksLimit": ChartConstants.NUMBER_OF_Y_AXIS_TICKS,
                },
            },
        },
    },
});
