// @ts-strict-ignore
import { DATA, LABELS } from "src/app/edge/history/common/energy/chart/CHART.CONSTANTS.SPEC";

import { DummyConfig } from "src/app/shared/components/edge/EDGECONFIG.SPEC";
import { OeTester } from "src/app/shared/components/shared/testing/common";
import { TestContext, TestingUtils } from "src/app/shared/components/shared/testing/UTILS.SPEC";
import { ChartAxis } from "src/app/shared/utils/utils";
import { History, expectView } from "./CHART.CONSTANTS.SPEC";

describe("History Heatpump", () => {

    const config = DUMMY_CONFIG.FROM(
        DUMMY_CONFIG.COMPONENT.HEAT_PUMP_SG_READY("ctrlIoHeatPump0", "Wärmepumpe"),
    );

    let TEST_CONTEXT: TestContext;
    beforeEach(async () =>
        TEST_CONTEXT = await TESTING_UTILS.SHARED_SETUP(),
    );

    it("#getChartData()", () => {
        {
            // Line-Chart
            expectView(config, OBJECT.VALUES(CONFIG.COMPONENTS)[0], TEST_CONTEXT, "line", HISTORY.DAY,
                {
                    datasets: {
                        data: [
                            DATA("Zustand", [
                                2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 1, 2, 2, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 2, 2, 2, 2, 2, 2, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 2, 2, 2, 2, 3, 2, 3, 3, 3, 2, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
                            ]),
                        ],
                        labels: LABELS(HISTORY.DAY.DATA_CHANNEL_WITH_VALUES.RESULT.TIMESTAMPS),
                        options: OE_TESTER.CHART_OPTIONS.LINE_CHART_OPTIONS("hour", "line", {
                            [CHART_AXIS.LEFT]: { scale: { beginAtZero: true }, title: "Zustand" },
                        }),
                    },
                });
        }
        {
            // Line-Chart
            expectView(config, OBJECT.VALUES(CONFIG.COMPONENTS)[0], TEST_CONTEXT, "bar", HISTORY.MONTH,
                {
                    datasets: {
                        data: [
                            DATA("Sperre", [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, null]),
                            DATA("Normalbetrieb", [86353, 86353, 86354, 86353, 86352, 71428, 86353, 86352, 86354, 85866, 77027, 86353, 86352, 86352, 76953, 74238, 85915, 86352, 86352, 86354, 82752, 86353, 86352, 86353, 73268, 86352, 89950, 86353, 86352, 35419, null]),
                            DATA("Einschaltempfehlung", [0, 0, 0, 0, 0, 5396, 0, 0, 0, 0, 5395, 0, 0, 0, 3597, 3597, 0, 0, 0, 0, 1798, 0, 0, 0, 7194, 0, 0, 0, 0, 0, null]),
                            DATA("Einschaltbefehl", [0, 0, 0, 0, 0, 9519, 0, 0, 0, 0, 3921, 0, 0, 0, 5795, 8510, 0, 0, 0, 0, 1798, 0, 0, 0, 5883, 0, 0, 0, 0, 0, null]),
                        ],
                        labels: LABELS(HISTORY.MONTH.ENERGY_PER_PERIOD_CHANNEL_WITH_VALUES.RESULT.TIMESTAMPS),
                        options: OE_TESTER.CHART_OPTIONS.BAR_CHART_OPTIONS("day", "bar", {}, "Aktive Zeit"),
                    },
                });
        }
    });
});
