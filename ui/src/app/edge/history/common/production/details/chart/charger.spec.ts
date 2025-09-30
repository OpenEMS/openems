// @ts-strict-ignore
import { ActivatedRoute } from "@angular/router";
import { DummyConfig } from "src/app/shared/components/edge/EDGECONFIG.SPEC";
import { OeTester } from "src/app/shared/components/shared/testing/common";
import { OeChartTester } from "src/app/shared/components/shared/testing/tester";
import { TestContext, TestingUtils } from "src/app/shared/components/shared/testing/UTILS.SPEC";
import { EdgeConfig } from "src/app/shared/shared";
import { DATA, LABELS } from "../../../energy/chart/CHART.CONSTANTS.SPEC";
import { History } from "./CHANNELS.SPEC";
import { ChargerChartDetailsComponent } from "./charger";

describe("History Production Details - chargers", () => {
    const defaultEMS = DUMMY_CONFIG.FROM(
        DUMMY_CONFIG.COMPONENT.GOODWE_CHARGER_MPPT_TWO_STRING("charger0", "MPPT 1"),
    );

    let TEST_CONTEXT: TestContext & { route: ActivatedRoute };
    beforeEach(async () => {
        TEST_CONTEXT = await TESTING_UTILS.SETUP_WITH_ACTIVATED_ROUTE("charger0");
    });

    it("#getChartData()", () => {
        {
            expectView(defaultEMS, TEST_CONTEXT, "line", HISTORY.DAY,
                {
                    datasets: {
                        data: [
                            DATA("MPPT 1: 15,9 kWh", [0.124, 0, null, 0, 0.173, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, 0, 0, 0.11, 0.113, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.145, 0, 0, 0, 0, 0, 0, null, null, null, 0, 0, 0, 0, 0, 0, 0, 0, 0.113, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, null, 0.113, 0, 0, null, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, 0, 0, 0, 0, 0.13, 0, 0, 0, 0, 0, null, null, null, null, 0, 0, 0, 0, 0, null, 0, null, 0.14, null, null, null, 2.127, 0.175, 0.176, null, 0.18, 0.18, 0.185, 0.18, null, 0.185, 0.19, 0.18, 0.18, 0.176, 0.176, 0.17, 0.175, 0.17, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null]),
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
                        data: [
                            DATA("MPPT 1: 21,6 kWh", [0.016, 0.014, 0.016, 0.015, 0.016, 0.015, 0.015, 0.015, 0.016, 0.017, 0.018, 0.014, 0.016, 0.017, 0.016, 0.015, 0.014, 0.017, 0.015, 0.016, 0.017, 0.016, 0.015, 0.016, 0.014, 0.015, 0.014, 0.016, 0.014, null, null]),
                        ],
                        labels: LABELS(HISTORY.MONTH.ENERGY_PER_PERIOD_CHANNEL_WITH_VALUES.RESULT.TIMESTAMPS),
                        options: OE_TESTER.CHART_OPTIONS.BAR_CHART_OPTIONS("day", "bar", {}),
                    },
                });
        }
    });
});

export function expectView(config: EdgeConfig, testContext: TestContext & { route: ActivatedRoute }, chartType: "line" | "bar", channels: OE_TESTER.TYPES.CHANNELS, view: OE_CHART_TESTER.VIEW): void {
    expect(TESTING_UTILS.REMOVE_FUNCTIONS(OeChartTester
        .apply(ChargerChartDetailsComponent
            .getChartData(
                DUMMY_CONFIG.CONVERT_DUMMY_EDGE_CONFIG_TO_REAL_EDGE_CONFIG(config), TEST_CONTEXT.ROUTE,
                TEST_CONTEXT.TRANSLATE), chartType, channels, testContext, config)))
        .toEqual(TESTING_UTILS.REMOVE_FUNCTIONS(view));
}
