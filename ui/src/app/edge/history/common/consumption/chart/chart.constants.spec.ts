import { DummyConfig } from "src/app/shared/edge/edgeconfig.spec";
import { OeTester } from "src/app/shared/genericComponents/shared/testing/common";
import { OeChartTester } from "src/app/shared/genericComponents/shared/testing/tester";
import { EdgeConfig } from "src/app/shared/shared";
import { removeFunctions, TestContext } from "src/app/shared/test/utils.spec";

import { ChartComponent } from "./chart";

export function expectView(config: EdgeConfig, testContext: TestContext, chartType: 'line' | 'bar', channels: OeTester.Types.Channels, view: OeChartTester.View): void {

  expect(removeFunctions(OeChartTester
    .apply(ChartComponent
      .getChartData(
        DummyConfig.convertDummyEdgeConfigToRealEdgeConfig(config),
        testContext.translate), chartType, channels, testContext, config)))
    .toEqual(removeFunctions(view));
}
