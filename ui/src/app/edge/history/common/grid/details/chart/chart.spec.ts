// @ts-strict-ignore
import { ActivatedRoute } from "@angular/router";
import { DummyConfig } from "src/app/shared/components/edge/EDGECONFIG.SPEC";
import { OeTester } from "src/app/shared/components/shared/testing/common";
import { OeChartTester } from "src/app/shared/components/shared/testing/tester";
import { TestContext, TestingUtils } from "src/app/shared/components/shared/testing/UTILS.SPEC";
import { EdgeConfig } from "src/app/shared/shared";
import { DATA, LABELS } from "../../../energy/chart/CHART.CONSTANTS.SPEC";
import { History } from "./CHANNELS.SPEC";
import { ChartComponent } from "./chart";

describe("History Grid Details - _sum", () => {
    const defaultEMS = DUMMY_CONFIG.FROM(
        DUMMY_CONFIG.COMPONENT.SUM("_sum"),
    );

    let TEST_CONTEXT: TestContext & { route: ActivatedRoute };
    beforeEach(async () => {
        TEST_CONTEXT = await TESTING_UTILS.SETUP_WITH_ACTIVATED_ROUTE("_sum");
    });

    it("#getChartData()", () => {
        {
            expectView(defaultEMS, TEST_CONTEXT, "line", HISTORY.DAY,
                {
                    datasets: {
                        data: [
                            DATA("Gesamt", [0.124, 0, null, 0, 0.173, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, 0, 0, 0.11, 0.113, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.145, 0, 0, 0, 0, 0, 0, null, null, null, 0, 0, 0, 0, 0, 0, 0, 0, 0.113, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, null, 0.113, 0, 0, null, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, 0, 0, 0, 0, 0.13, 0, 0, 0, 0, 0, null, null, null, null, 0, 0, 0, 0, 0, null, 0, null, 0.14, null, null, null, 2.127, 0.175, 0.176, null, 0.18, 0.18, 0.185, 0.18, null, 0.185, 0.19, 0.18, 0.18, 0.176, 0.176, 0.17, 0.175, 0.17, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null]),
                            DATA("Phase L1", [0.041, 0, null, 0, 0.058, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, 0, 0, 0.037, 0.038, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.048, 0, 0, 0, 0, 0, 0, null, null, null, 0, 0, 0, 0, 0, 0, 0, 0, 0.038, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, null, 0.038, 0, 0, null, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, 0, 0, 0, 0, 0.043, 0, 0, 0, 0, 0, null, null, null, null, 0, 0, 0, 0, 0, null, 0, null, 0.047, null, null, null, 0.709, 0.058, 0.059, null, 0.06, 0.06, 0.062, 0.06, null, 0.062, 0.063, 0.06, 0.06, 0.059, 0.059, 0.057, 0.058, 0.057, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null]),
                            DATA("Phase L2", [0.041, 0, null, 0, 0.058, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, 0, 0, 0.037, 0.038, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.048, 0, 0, 0, 0, 0, 0, null, null, null, 0, 0, 0, 0, 0, 0, 0, 0, 0.038, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, null, 0.038, 0, 0, null, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, 0, 0, 0, 0, 0.043, 0, 0, 0, 0, 0, null, null, null, null, 0, 0, 0, 0, 0, null, 0, null, 0.047, null, null, null, 0.709, 0.058, 0.059, null, 0.06, 0.06, 0.062, 0.06, null, 0.062, 0.063, 0.06, 0.06, 0.059, 0.059, 0.057, 0.058, 0.057, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null]),
                            DATA("Phase L3", [0.041, 0, null, 0, 0.058, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, 0, 0, 0.037, 0.038, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.048, 0, 0, 0, 0, 0, 0, null, null, null, 0, 0, 0, 0, 0, 0, 0, 0, 0.038, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, null, 0.038, 0, 0, null, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, 0, 0, 0, 0, 0.043, 0, 0, 0, 0, 0, null, null, null, null, 0, 0, 0, 0, 0, null, 0, null, 0.047, null, null, null, 0.709, 0.058, 0.059, null, 0.06, 0.06, 0.062, 0.06, null, 0.062, 0.063, 0.06, 0.06, 0.059, 0.059, 0.057, 0.058, 0.057, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null]),
                        ],
                        labels: LABELS(HISTORY.DAY.DATA_CHANNEL_WITH_VALUES.RESULT.TIMESTAMPS),
                        options: OE_TESTER.CHART_OPTIONS.LINE_CHART_OPTIONS("hour", "line", { "left": { scale: { beginAtZero: true } } },
                        ),
                    },
                });
        }
        {
            expectView(defaultEMS, TEST_CONTEXT, "bar", HISTORY.MONTH,
                {
                    datasets: {
                        data: [],
                        labels: LABELS(HISTORY.MONTH.ENERGY_PER_PERIOD_CHANNEL_WITH_VALUES.RESULT.TIMESTAMPS),
                        options: OE_TESTER.CHART_OPTIONS.BAR_CHART_OPTIONS("hour", "bar", {}),
                    },
                });
        }
    });
});

export function expectView(config: EdgeConfig, testContext: TestContext & { route: ActivatedRoute }, chartType: "line" | "bar", channels: OE_TESTER.TYPES.CHANNELS, view: OE_CHART_TESTER.VIEW): void {
    expect(TESTING_UTILS.REMOVE_FUNCTIONS(OeChartTester
        .apply(ChartComponent
            .getChartData(
                TEST_CONTEXT.TRANSLATE), chartType, channels, testContext, config)))
        .toEqual(TESTING_UTILS.REMOVE_FUNCTIONS(view));
}
