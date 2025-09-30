import { DummyConfig } from "src/app/shared/components/edge/EDGECONFIG.SPEC";
import { OeTester } from "src/app/shared/components/shared/testing/common";
import { OeChartTester } from "src/app/shared/components/shared/testing/tester";
import { TestContext, TestingUtils } from "src/app/shared/components/shared/testing/UTILS.SPEC";
import { EdgeConfig } from "src/app/shared/shared";
import { ChartComponent } from "./chart";

export function expectView(config: EdgeConfig, testContext: TestContext, chartType: "line" | "bar", channels: OE_TESTER.TYPES.CHANNELS, view: OE_CHART_TESTER.VIEW): void {

  expect(TESTING_UTILS.REMOVE_FUNCTIONS(OeChartTester
    .apply(ChartComponent
      .getChartData(
        DUMMY_CONFIG.CONVERT_DUMMY_EDGE_CONFIG_TO_REAL_EDGE_CONFIG(config),
        TEST_CONTEXT.TRANSLATE, DUMMY_CONFIG.DUMMY_EDGE({ version: "2024.1.1" })), chartType, channels, testContext, config)))
    .toEqual(TESTING_UTILS.REMOVE_FUNCTIONS(view));
}
