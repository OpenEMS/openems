import { OeTester } from "src/app/shared/components/shared/testing/common";
import { OeChartTester } from "src/app/shared/components/shared/testing/tester";
import { TestContext, TestingUtils } from "src/app/shared/components/shared/testing/utils.spec";
import { EdgeConfig } from "src/app/shared/shared";
import { StorageTotalChartComponent } from "../chart/totalchart";
import { StorageEssChartComponent } from "../details/chart/esschart";

export function expectEssChartViewToEqual(testContext: TestContext, chartType: "line" | "bar", channels: OeTester.Types.Channels, view: OeChartTester.View, component: EdgeConfig.Component, config: EdgeConfig): void {
  OeChartTester
    .apply(StorageEssChartComponent
      .getChartData(testContext.translate, component, chartType, config), chartType, channels, testContext, config);
  expect(TestingUtils.removeFunctions(OeChartTester
    .apply(StorageEssChartComponent
      .getChartData(testContext.translate, component, chartType, config), chartType, channels, testContext, config)))
    .toEqual(TestingUtils.removeFunctions(view));
}

export function expectTotalChartViewToEqual(testContext: TestContext, chartType: "line" | "bar", channels: OeTester.Types.Channels, view: OeChartTester.View, essComponents: EdgeConfig.Component[], showPhases: boolean, phaseColors: string[], config: EdgeConfig): void {
  expect(TestingUtils.removeFunctions(OeChartTester
    .apply(StorageTotalChartComponent
      .getChartData(testContext.translate, chartType, config), chartType, channels, testContext, config)))
    .toEqual(TestingUtils.removeFunctions(view));
}
