import { DummyConfig } from "src/app/shared/components/edge/edgeconfig.spec";
import { OeTester } from "src/app/shared/components/shared/testing/common";
import { OeChartTester } from "src/app/shared/components/shared/testing/tester";
import { TestContext, TestingUtils } from "src/app/shared/components/shared/testing/utils.spec";
import { EdgeConfig } from "src/app/shared/shared";
import { ChartComponent } from "./chart";

export function expectView(config: EdgeConfig, testContext: TestContext, chartType: "line" | "bar", channels: OeTester.Types.Channels, view: OeChartTester.View): void {

  expect(TestingUtils.removeFunctions(OeChartTester
    .apply(ChartComponent
      .getChartData(
        DummyConfig.convertDummyEdgeConfigToRealEdgeConfig(config),
        testContext.translate, DummyConfig.dummyEdge({ version: "2024.1.1" })), chartType, channels, testContext, config)))
    .toEqual(TestingUtils.removeFunctions(view));
}
