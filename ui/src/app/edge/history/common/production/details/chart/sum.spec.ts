// @ts-strict-ignore
import { ActivatedRoute } from "@angular/router";
import { DummyConfig } from "src/app/shared/components/edge/edgeconfig.spec";
import { OeTester } from "src/app/shared/components/shared/testing/common";
import { OeChartTester } from "src/app/shared/components/shared/testing/tester";
import { removeFunctions, sharedSetupWithComponentIdRoute, TestContext } from "src/app/shared/components/shared/testing/utils.spec";
import { EdgeConfig } from "src/app/shared/shared";
import { DATA, LABELS } from "../../../energy/chart/chart.constants.spec";
import { History } from "./channels.spec";
import { SumChartDetailsComponent } from "./sum";

describe("History Production Details - _sum", () => {
    const defaultEMS = DummyConfig.from(
        DummyConfig.Component.SUM("_sum", "Gesamt"),
        DummyConfig.Component.SOLAR_EDGE_PV_INVERTER("meter0"),
    );

    let TEST_CONTEXT: TestContext & { route: ActivatedRoute };
    beforeEach(async () => {
        TEST_CONTEXT = await sharedSetupWithComponentIdRoute("_sum");
    });

    it("#getChartData() - asymmetricMeter && no essDcCharger configured", () => {
        {
            expectView(defaultEMS, TEST_CONTEXT, "line", History.DAY,
                {
                    datasets: {
                        data: [
                            DATA("Gesamt: 15,9 kWh", [0.124, 0, null, 0, 0.173, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, 0, 0, 0.11, 0.113, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.145, 0, 0, 0, 0, 0, 0, null, null, null, 0, 0, 0, 0, 0, 0, 0, 0, 0.113, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, null, 0.113, 0, 0, null, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, 0, 0, 0, 0, 0.13, 0, 0, 0, 0, 0, null, null, null, null, 0, 0, 0, 0, 0, null, 0, null, 0.14, null, null, null, 2.127, 0.175, 0.176, null, 0.18, 0.18, 0.185, 0.18, null, 0.185, 0.19, 0.18, 0.18, 0.176, 0.176, 0.17, 0.175, 0.17, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null]),
                            DATA("Phase L1", [0.041, 0, null, 0, 0.058, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, 0, 0, 0.037, 0.038, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.048, 0, 0, 0, 0, 0, 0, null, null, null, 0, 0, 0, 0, 0, 0, 0, 0, 0.038, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, null, 0.038, 0, 0, null, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, 0, 0, 0, 0, 0.043, 0, 0, 0, 0, 0, null, null, null, null, 0, 0, 0, 0, 0, null, 0, null, 0.047, null, null, null, 0.709, 0.058, 0.059, null, 0.06, 0.06, 0.062, 0.06, null, 0.062, 0.063, 0.06, 0.06, 0.059, 0.059, 0.057, 0.058, 0.057, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null]),
                            DATA("Phase L2", [0.041, 0, null, 0, 0.058, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, 0, 0, 0.037, 0.038, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.048, 0, 0, 0, 0, 0, 0, null, null, null, 0, 0, 0, 0, 0, 0, 0, 0, 0.038, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, null, 0.038, 0, 0, null, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, 0, 0, 0, 0, 0.043, 0, 0, 0, 0, 0, null, null, null, null, 0, 0, 0, 0, 0, null, 0, null, 0.047, null, null, null, 0.709, 0.058, 0.059, null, 0.06, 0.06, 0.062, 0.06, null, 0.062, 0.063, 0.06, 0.06, 0.059, 0.059, 0.057, 0.058, 0.057, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null]),
                            DATA("Phase L3", [0.041, 0, null, 0, 0.058, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, 0, 0, 0.037, 0.038, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.048, 0, 0, 0, 0, 0, 0, null, null, null, 0, 0, 0, 0, 0, 0, 0, 0, 0.038, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, null, 0.038, 0, 0, null, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, 0, 0, 0, 0, 0.043, 0, 0, 0, 0, 0, null, null, null, null, 0, 0, 0, 0, 0, null, 0, null, 0.047, null, null, null, 0.709, 0.058, 0.059, null, 0.06, 0.06, 0.062, 0.06, null, 0.062, 0.063, 0.06, 0.06, 0.059, 0.059, 0.057, 0.058, 0.057, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null]),
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
                            DATA("Gesamt: 21,6 kWh", [0.016, 0.014, 0.016, 0.015, 0.016, 0.015, 0.015, 0.015, 0.016, 0.017, 0.018, 0.014, 0.016, 0.017, 0.016, 0.015, 0.014, 0.017, 0.015, 0.016, 0.017, 0.016, 0.015, 0.016, 0.014, 0.015, 0.014, 0.016, 0.014, null, null]),
                        ],
                        labels: LABELS(History.MONTH.energyPerPeriodChannelWithValues.result.timestamps),
                        options: OeTester.ChartOptions.BAR_CHART_OPTIONS("day", "bar", {}),
                    },
                });
        }
    });

    it("#getChartData() - essDcCharger configured", () => {
        {
            const defaultEMS = DummyConfig.from(
                DummyConfig.Component.SUM("_sum", "Gesamt"),
                DummyConfig.Component.GOODWE_CHARGER_MPPT_TWO_STRING("charger0"),
            );
            expectView(defaultEMS, TEST_CONTEXT, "line", History.DAY,
                {
                    datasets: {
                        data: [
                            DATA("Gesamt: 15,9 kWh", [0.124, 0, null, 0, 0.173, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, 0, 0, 0.11, 0.113, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.145, 0, 0, 0, 0, 0, 0, null, null, null, 0, 0, 0, 0, 0, 0, 0, 0, 0.113, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, null, 0.113, 0, 0, null, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, 0, 0, 0, 0, 0.13, 0, 0, 0, 0, 0, null, null, null, null, 0, 0, 0, 0, 0, null, 0, null, 0.14, null, null, null, 2.127, 0.175, 0.176, null, 0.18, 0.18, 0.185, 0.18, null, 0.185, 0.19, 0.18, 0.18, 0.176, 0.176, 0.17, 0.175, 0.17, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null]),
                            DATA("Phase L1", [0.049, 0, null, 0, 0.08233333333333334, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, 0, 0, 0.07366666666666666, 0.07566666666666666, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.09633333333333333, 0, 0, 0, 0, 0, 0, null, null, null, 0, 0, 0, 0, 0, 0, 0, 0, 0.07566666666666666, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, null, 0.07566666666666666, 0, 0, null, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, 0, 0, 0, 0, 0.08633333333333333, 0, 0, 0, 0, 0, null, null, null, null, 0, 0, 0, 0, 0, null, 0, null, 0.09366666666666668, null, null, null, 1.418, 0.11633333333333333, 0.11766666666666667, null, 0.12, 0.12, 0.12366666666666667, 0.12, null, 0.12366666666666667, 0.12633333333333335, 0.12, 0.12, 0.11766666666666667, 0.11766666666666667, 0.11366666666666667, 0.11633333333333333, 0.11366666666666667, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null]),
                            DATA("Phase L2", [0.049, 0, null, 0, 0.08233333333333334, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, 0, 0, 0.07366666666666666, 0.07566666666666666, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.09633333333333333, 0, 0, 0, 0, 0, 0, null, null, null, 0, 0, 0, 0, 0, 0, 0, 0, 0.07566666666666666, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, null, 0.07566666666666666, 0, 0, null, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, 0, 0, 0, 0, 0.08633333333333333, 0, 0, 0, 0, 0, null, null, null, null, 0, 0, 0, 0, 0, null, 0, null, 0.09366666666666668, null, null, null, 1.418, 0.11633333333333333, 0.11766666666666667, null, 0.12, 0.12, 0.12366666666666667, 0.12, null, 0.12366666666666667, 0.12633333333333335, 0.12, 0.12, 0.11766666666666667, 0.11766666666666667, 0.11366666666666667, 0.11633333333333333, 0.11366666666666667, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null]),
                            DATA("Phase L3", [0.049, 0, null, 0, 0.08233333333333334, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, 0, 0, 0.07366666666666666, 0.07566666666666666, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.09633333333333333, 0, 0, 0, 0, 0, 0, null, null, null, 0, 0, 0, 0, 0, 0, 0, 0, 0.07566666666666666, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, null, 0.07566666666666666, 0, 0, null, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, 0, 0, 0, 0, 0.08633333333333333, 0, 0, 0, 0, 0, null, null, null, null, 0, 0, 0, 0, 0, null, 0, null, 0.09366666666666668, null, null, null, 1.418, 0.11633333333333333, 0.11766666666666667, null, 0.12, 0.12, 0.12366666666666667, 0.12, null, 0.12366666666666667, 0.12633333333333335, 0.12, 0.12, 0.11766666666666667, 0.11766666666666667, 0.11366666666666667, 0.11633333333333333, 0.11366666666666667, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null])],
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
                            DATA("Gesamt: 21,6 kWh", [0.016, 0.014, 0.016, 0.015, 0.016, 0.015, 0.015, 0.015, 0.016, 0.017, 0.018, 0.014, 0.016, 0.017, 0.016, 0.015, 0.014, 0.017, 0.015, 0.016, 0.017, 0.016, 0.015, 0.016, 0.014, 0.015, 0.014, 0.016, 0.014, null, null]),
                        ],
                        labels: LABELS(History.MONTH.energyPerPeriodChannelWithValues.result.timestamps),
                        options: OeTester.ChartOptions.BAR_CHART_OPTIONS("day", "bar", {}),
                    },
                });
        }
    });
    it("#getChartData() - no essDcCharger & no assymetric meter configured", () => {
        {
            const defaultEMS = DummyConfig.from(
                DummyConfig.Component.SUM("_sum", "Gesamt"),
            );
            expectView(defaultEMS, TEST_CONTEXT, "line", History.DAY,
                {
                    datasets: {
                        data: [
                            DATA("Gesamt: 15,9 kWh", [0.124, 0, null, 0, 0.173, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, 0, 0, 0.11, 0.113, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.145, 0, 0, 0, 0, 0, 0, null, null, null, 0, 0, 0, 0, 0, 0, 0, 0, 0.113, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, null, 0.113, 0, 0, null, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, 0, 0, 0, 0, 0.13, 0, 0, 0, 0, 0, null, null, null, null, 0, 0, 0, 0, 0, null, 0, null, 0.14, null, null, null, 2.127, 0.175, 0.176, null, 0.18, 0.18, 0.185, 0.18, null, 0.185, 0.19, 0.18, 0.18, 0.176, 0.176, 0.17, 0.175, 0.17, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null])],
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
                            DATA("Gesamt: 21,6 kWh", [0.016, 0.014, 0.016, 0.015, 0.016, 0.015, 0.015, 0.015, 0.016, 0.017, 0.018, 0.014, 0.016, 0.017, 0.016, 0.015, 0.014, 0.017, 0.015, 0.016, 0.017, 0.016, 0.015, 0.016, 0.014, 0.015, 0.014, 0.016, 0.014, null, null]),
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
        .apply(SumChartDetailsComponent
            .getChartData(
                DummyConfig.convertDummyEdgeConfigToRealEdgeConfig(config), testContext.route,
                testContext.translate), chartType, channels, testContext, config)))
        .toEqual(removeFunctions(view));
}
