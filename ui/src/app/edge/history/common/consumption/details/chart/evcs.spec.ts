// @ts-strict-ignore
import { ActivatedRoute } from "@angular/router";
import { DummyConfig } from "src/app/shared/components/edge/edgeconfig.spec";
import { OeTester } from "src/app/shared/components/shared/testing/common";
import { OeChartTester } from "src/app/shared/components/shared/testing/tester";
import { removeFunctions, sharedSetupWithComponentIdRoute, TestContext } from "src/app/shared/components/shared/testing/utils.spec";
import { EdgeConfig } from "src/app/shared/shared";
import { DATA, LABELS } from "../../../energy/chart/chart.constants.spec";
import { History } from "./channels.spec";
import { EvcsChartDetailsComponent } from "./evcs";

describe("History Consumption Details - evcs", () => {
    const defaultEMS = DummyConfig.from(
        DummyConfig.Component.EVCS_KEBA_KECONTACT("evcs0", "Charging Station"),
    );

    let TEST_CONTEXT: TestContext & { route: ActivatedRoute };
    beforeEach(async () => {
        TEST_CONTEXT = await sharedSetupWithComponentIdRoute("evcs0");
    });

    it("#getChartData() - evcs", () => {
        {
            expectView(defaultEMS, TEST_CONTEXT, "line", History.DAY,
                {
                    datasets: {
                        data: [
                            DATA("Charging Station: 15,9 kWh", [0.124, 0, null, 0, 0.173, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, 0, 0, 0.11, 0.113, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.145, 0, 0, 0, 0, 0, 0, null, null, null, 0, 0, 0, 0, 0, 0, 0, 0, 0.113, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, null, 0.113, 0, 0, null, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, 0, 0, 0, 0, 0.13, 0, 0, 0, 0, 0, null, null, null, null, 0, 0, 0, 0, 0, null, 0, null, 0.14, null, null, null, 2.127, 0.175, 0.176, null, 0.18, 0.18, 0.185, 0.18, null, 0.185, 0.19, 0.18, 0.18, 0.176, 0.176, 0.17, 0.175, 0.17, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null]),
                        ],
                        labels: LABELS(History.DAY.dataChannelWithValues.result.timestamps),
                        options: OeTester.ChartOptions.LINE_CHART_OPTIONS("hour", "line", { "left": { scale: { beginAtZero: true } } },
                        ),
                    },
                });
        }
        {
            expectView(defaultEMS, TEST_CONTEXT, "bar", History.MONTH,
                {
                    datasets: {
                        data: [
                            DATA("Charging Station: 21,6 kWh", [0.016, 0.014, 0.016, 0.015, 0.016, 0.015, 0.015, 0.015, 0.016, 0.017, 0.018, 0.014, 0.016, 0.017, 0.016, 0.015, 0.014, 0.017, 0.015, 0.016, 0.017, 0.016, 0.015, 0.016, 0.014, 0.015, 0.014, 0.016, 0.014, null, null]),
                        ],
                        labels: LABELS(History.MONTH.energyPerPeriodChannelWithValues.result.timestamps),
                        options: OeTester.ChartOptions.BAR_CHART_OPTIONS("day", "bar", {}),
                    },
                });
        }
    });
});

export function expectView(config: EdgeConfig, testContext: TestContext & { route: ActivatedRoute }, chartType: "line" | "bar", channels: OeTester.Types.Channels, view: OeChartTester.View): void {
    expect(removeFunctions(OeChartTester
        .apply(EvcsChartDetailsComponent
            .getChartData(
                DummyConfig.convertDummyEdgeConfigToRealEdgeConfig(config), testContext.route,
                testContext.translate), chartType, channels, testContext, config)))
        .toEqual(removeFunctions(view));
}
