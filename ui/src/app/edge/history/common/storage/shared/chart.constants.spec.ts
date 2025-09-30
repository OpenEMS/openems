import { OeTester } from "src/app/shared/components/shared/testing/common";
import { OeChartTester } from "src/app/shared/components/shared/testing/tester";
import { TestContext, TestingUtils } from "src/app/shared/components/shared/testing/UTILS.SPEC";
import { EdgeConfig } from "src/app/shared/shared";
import { StorageTotalChartComponent } from "../chart/totalchart";
import { StorageEssChartComponent } from "../details/chart/esschart";

export function expectEssChartViewToEqual(testContext: TestContext, chartType: "line" | "bar", channels: OE_TESTER.TYPES.CHANNELS, view: OE_CHART_TESTER.VIEW, component: EDGE_CONFIG.COMPONENT, config: EdgeConfig): void {
  expect(TESTING_UTILS.REMOVE_FUNCTIONS(OeChartTester
    .apply(StorageEssChartComponent
      .getChartData(TEST_CONTEXT.TRANSLATE, component, chartType, config), chartType, channels, testContext, config)))
    .toEqual(TESTING_UTILS.REMOVE_FUNCTIONS(view));
}

export function expectTotalChartViewToEqual(testContext: TestContext, chartType: "line" | "bar", channels: OE_TESTER.TYPES.CHANNELS, view: OE_CHART_TESTER.VIEW, essComponents: EDGE_CONFIG.COMPONENT[], showPhases: boolean, phaseColors: string[], config: EdgeConfig): void {

  expect(TESTING_UTILS.REMOVE_FUNCTIONS(OeChartTester
    .apply(StorageTotalChartComponent
      .getChartData(TEST_CONTEXT.TRANSLATE, chartType, config), chartType, channels, testContext, config)))
    .toEqual(TESTING_UTILS.REMOVE_FUNCTIONS(view));
}
