import { DummyConfig } from "src/app/shared/edge/edgeconfig.spec";
import { EdgeConfig } from "src/app/shared/shared";
import { removeFunctions, TestContext } from "src/app/shared/test/utils.spec";

import { OeChartTester } from "../../../../../shared/genericComponents/shared/tester";
import { History } from "./channels.spec";
import { ChartComponent } from "./chart";

export function expectView(config: EdgeConfig, testContext: TestContext, chartType: 'line' | 'bar', channels: History.OeChannels, view: OeChartTester.View, showPhases: boolean, phaseColors: string[]): void {

  let some = removeFunctions(OeChartTester
    .apply(ChartComponent
      .getChartData(
        DummyConfig.convertDummyEdgeConfigToRealEdgeConfig(config),
        testContext.translate, showPhases, phaseColors), chartType, channels, testContext));

  sessionStorage.setItem("result", JSON.stringify(some))
  debugger;
  expect(removeFunctions(OeChartTester
    .apply(ChartComponent
      .getChartData(
        DummyConfig.convertDummyEdgeConfigToRealEdgeConfig(config),
        testContext.translate, showPhases, phaseColors), chartType, channels, testContext)))

    .toEqual(removeFunctions(view));

  // debugger;
};