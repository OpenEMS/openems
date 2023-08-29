import { EdgeConfig } from "src/app/shared/shared";
import { TestContext, removeFunctions } from "src/app/shared/test/utils.spec";
import { History } from "src/app/edge/history/common/energy/chart/channels.spec";

import { DummyConfig } from "src/app/shared/edge/edgeconfig.spec";
import { OeChartTester } from "../../../../../shared/genericComponents/shared/tester";
import { ChartComponent } from "./chart";

export function expectView(config: EdgeConfig, testContext: TestContext, chartType: 'line' | 'bar', channels: History.OeChannels, view: OeChartTester.View): void {
  expect(removeFunctions(OeChartTester
    .apply(ChartComponent
      .getChartData(DummyConfig.convertDummyEdgeConfigToRealEdgeConfig(config), chartType, testContext.translate), chartType, channels, testContext)))
    .toEqual(removeFunctions(view));
};