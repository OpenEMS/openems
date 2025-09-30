import { DummyConfig } from "src/app/shared/components/edge/EDGECONFIG.SPEC";
import { OeTester } from "src/app/shared/components/shared/testing/common";
import { TestContext, TestingUtils } from "src/app/shared/components/shared/testing/UTILS.SPEC";
import { EdgeConfig } from "src/app/shared/shared";
import { OeChartTester } from "../../../../../shared/components/shared/testing/tester";
import { ChartComponent } from "./chart";

export function expectView(config: EdgeConfig, testContext: TestContext, chartType: "line" | "bar", channels: OE_TESTER.TYPES.CHANNELS, view: OE_CHART_TESTER.VIEW): void {
  expect(TESTING_UTILS.REMOVE_FUNCTIONS(OeChartTester
    .apply(ChartComponent
      .getChartData(DUMMY_CONFIG.CONVERT_DUMMY_EDGE_CONFIG_TO_REAL_EDGE_CONFIG(config), chartType, TEST_CONTEXT.TRANSLATE), chartType, channels, testContext, config)))
    .toEqual(TESTING_UTILS.REMOVE_FUNCTIONS(view));
}

export const DATASET = (data: OE_CHART_TESTER.DATASET.DATA, labels: OE_CHART_TESTER.DATASET.LEGEND_LABEL, options: OE_CHART_TESTER.DATASET.OPTION) => ({
  data: data,
  labels: labels,
  options: options,
});

export const DATA = (name: string, value: (number | null)[]): OE_CHART_TESTER.DATASET.DATA => ({
  type: "data",
  label: name,
  value: value,
});

export const LABELS = (timestamps: string[]): OE_CHART_TESTER.DATASET.LEGEND_LABEL => ({
  type: "label",
  timestamps: TIMESTAMPS.MAP(element => new Date(element)),
});

export const OPTIONS = (options: OE_CHART_TESTER.DATASET.OPTION): OE_CHART_TESTER.DATASET.OPTION => options;
