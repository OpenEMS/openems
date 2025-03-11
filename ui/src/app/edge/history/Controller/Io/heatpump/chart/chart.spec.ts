// @ts-strict-ignore
import { DATA, LABELS } from "src/app/edge/history/common/energy/chart/chart.constants.spec";

import { DummyConfig } from "src/app/shared/components/edge/edgeconfig.spec";
import { OeTester } from "src/app/shared/components/shared/testing/common";
import { TestContext, TestingUtils } from "src/app/shared/components/shared/testing/utils.spec";
import { ChartAxis } from "src/app/shared/service/utils";
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
                        options: OeTester.ChartOptions.LINE_CHART_OPTIONS("hour", "line", {
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
