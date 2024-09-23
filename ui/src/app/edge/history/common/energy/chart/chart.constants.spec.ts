import { DummyConfig } from "src/app/shared/components/edge/edgeconfig.spec";
import { OeTester } from "src/app/shared/components/shared/testing/common";
import { removeFunctions, TestContext } from "src/app/shared/components/shared/testing/utils.spec";
import { EdgeConfig } from "src/app/shared/shared";
import { OeChartTester } from "../../../../../shared/components/shared/testing/tester";
import { ChartComponent } from "./chart";

export function expectView(config: EdgeConfig, testContext: TestContext, chartType: "line" | "bar", channels: OeTester.Types.Channels, view: OeChartTester.View): void {
  expect(removeFunctions(OeChartTester
    .apply(ChartComponent
      .getChartData(DummyConfig.convertDummyEdgeConfigToRealEdgeConfig(config), chartType, testContext.translate), chartType, channels, testContext, config)))
    .toEqual(removeFunctions(view));
}

export const DATASET = (data: OeChartTester.Dataset.Data, labels: OeChartTester.Dataset.LegendLabel, options: OeChartTester.Dataset.Option) => ({
  data: data,
  labels: labels,
  options: options,
});

export const DATA = (name: string, value: (number | null)[]): OeChartTester.Dataset.Data => ({
  type: "data",
  label: name,
  value: value,
});

export const LABELS = (timestamps: string[]): OeChartTester.Dataset.LegendLabel => ({
  type: "label",
  timestamps: timestamps.map(element => new Date(element)),
});

export const OPTIONS = (options: OeChartTester.Dataset.Option): OeChartTester.Dataset.Option => options;
