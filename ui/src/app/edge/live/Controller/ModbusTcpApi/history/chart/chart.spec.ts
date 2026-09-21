import { DATA, LABELS } from "src/app/edge/history/common/energy/chart/chart.constants.spec";
import { DummyConfig } from "src/app/shared/components/edge/edgeconfig.spec";
import { OeTester } from "src/app/shared/components/shared/testing/common";
import { OeChartTester } from "src/app/shared/components/shared/testing/tester";
import { TestContext, TestingUtils } from "src/app/shared/components/shared/testing/utils.spec";
import { EdgeConfig } from "src/app/shared/shared";
import { ChartAxis } from "src/app/shared/utils/utils";
import { History } from "./channels.spec";
import { ControllerModbusTcpApiChartComponent } from "./chart";

describe("History Modbus/TCP write/read", () => {
    const defaultEMS = DummyConfig.from(DummyConfig.Component.MODBUS_TCP_READWRITE("ctrlApiModbusTcp0"));

    let TEST_CONTEXT: TestContext;
    beforeEach(async () => {
        TEST_CONTEXT = await TestingUtils.sharedSetup();
    });

    it("#getChartData()", () => {
        {
            expectView(Object.values(defaultEMS.components)[0], defaultEMS, TEST_CONTEXT, "line", History.DAY, {
                datasets: {
                    // prettier-ignore
                    data: [
                            DATA("Vorgabe Wirkleistung", [null, null, null, 0.112, 0.262, 0.392, 0.24, 0.23, 0.229, 0.227, 0.317, 0.224, 0.133, 0.135, 0.133, 0.192, 0.209, 0.09, 0.095, 0.096, 0.164, 0.297, 0.184, 0.182, 0.183, 0.198, 0.333, 0.183, 0.093, 0.097, 0.098, 0.197, 0.266, 0.177, 0.144, 0.14, 0.173, 0.304, 0.305, 0.237, 0.232, 0.227, 0.283, 0.344, 0.135, 0.096, 0.095, null, null, null, null, 0.102, 0.129, 0.14, 0.301, 0.248, 0.267, 0.319, 0.31, 0.452, 0.451, 0.28, 0.234, 0.226, 0.249, 0.39, 0.242, 0.199, 0.179, 0.166, 0.28, 0.239, 0.192, 0.187, 0.187, 0.19, 0.303, 0.146, 0.062, 0.062, 0.064, 0.887, 1.119, 1.07, 1.057, 0.596, 0.138, 0.233, 0.152, 0.209, 0.192, 0.202, 0.308, 0.254, 0.175, 0.122, 0.108, 0.137, 0.216, 0.947, 0.599, 0.203, 0.232, 0.328, 0.299, 0.52, 1.213, 0.641, 1.03, 0.442, 0.374, 1.758, 0.249, 0.26, 0.346, 1.879, 0.23, 0.484, 1.26, 1.317, 1.488, 1.451, 1.892, 1.466, 1.332, 0.523, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null]),
                            DATA("Vorgabe maximaler Entladeleistung", [0.1]),
                            DATA("Vorgabe Blindleistung", [0.1, 0.5]),
                            DATA("Vorgabe minimaler Blindleistung", [0.1]),
                            DATA("Vorgabe maximaler Blindleistung", [0.1]),
                            DATA("Ladezustand", [10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 11, 11, 11, 11, 11, 11, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 11, 11, 12, 12, 12, 11, 11, 11, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10]), ],
                    labels: LABELS(History.DAY.dataChannelWithValues?.result?.timestamps ?? []),
                    options: OeTester.ChartOptions.MULTI_LINE_OPTIONS("hour", "line", {
                        [ChartAxis.RIGHT]: {
                            scale: { beginAtZero: true, title: { text: "%" }, grid: { display: true }, type: "linear" },
                        },
                    }),
                },
            });
        }

        {
            expectView(Object.values(defaultEMS.components)[0], defaultEMS, TEST_CONTEXT, "line", History.WEEK, {
                datasets: {
                    // prettier-ignore
                    data: [
                            DATA("Vorgabe Wirkleistung", [0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1]),
                            DATA("Vorgabe maximaler Beladeleistung", [0.1]),
                            DATA("Vorgabe maximaler Entladeleistung", [0.1]),
                            DATA("Vorgabe Blindleistung", [0.1]),
                            DATA("Vorgabe minimaler Blindleistung", [0.1]),
                            DATA("Vorgabe maximaler Blindleistung", [0.1]),
                            DATA("Ladezustand", [10]),
                        ],
                    labels: LABELS(History.WEEK.dataChannelWithValues?.result?.timestamps ?? []),
                    options: OeTester.ChartOptions.MULTI_LINE_OPTIONS("day", "line", {
                        [ChartAxis.RIGHT]: {
                            scale: { beginAtZero: true, title: { text: "%" }, grid: { display: true }, type: "linear" },
                        },
                    }),
                },
            });
        }

        {
            expectView(Object.values(defaultEMS.components)[0], defaultEMS, TEST_CONTEXT, "line", History.MONTH, {
                datasets: {
                    data: [
                        DATA(
                            "Vorgabe Wirkleistung",
                            [
                                0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1,
                                0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1,
                            ],
                        ),
                        DATA("Vorgabe maximaler Beladeleistung", [0.1]),
                        DATA("Vorgabe maximaler Entladeleistung", [0.1]),
                        DATA("Vorgabe Blindleistung", [0.1]),
                        DATA("Vorgabe minimaler Blindleistung", [0.1]),
                        DATA("Vorgabe maximaler Blindleistung", [0.1]),
                        DATA("Ladezustand", [10]),
                    ],
                    labels: LABELS(History.MONTH.dataChannelWithValues?.result?.timestamps ?? []),
                    options: OeTester.ChartOptions.MULTI_LINE_OPTIONS("day", "line", {
                        [ChartAxis.RIGHT]: {
                            scale: { beginAtZero: true, title: { text: "%" }, grid: { display: true }, type: "linear" },
                        },
                    }),
                },
            });
        }

        {
            expectView(Object.values(defaultEMS.components)[0], defaultEMS, TEST_CONTEXT, "line", History.YEAR, {
                datasets: {
                    data: [
                        DATA("Vorgabe Wirkleistung", [0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1]),
                        DATA("Vorgabe maximaler Beladeleistung", [0.1]),
                        DATA("Vorgabe maximaler Entladeleistung", [0.1]),
                        DATA("Vorgabe Blindleistung", [0.1]),
                        DATA("Vorgabe minimaler Blindleistung", [0.1]),
                        DATA("Vorgabe maximaler Blindleistung", [0.1]),
                        DATA("Ladezustand", [10]),
                    ],
                    labels: LABELS(History.YEAR.dataChannelWithValues?.result?.timestamps ?? []),
                    options: OeTester.ChartOptions.MULTI_LINE_OPTIONS("month", "line", {
                        [ChartAxis.RIGHT]: {
                            scale: { beginAtZero: true, title: { text: "%" }, grid: { display: true }, type: "linear" },
                        },
                    }),
                },
            });
        }

        const emptyEMS = DummyConfig.from(DummyConfig.Component.EMPPTY_MODBUS_TCP_READWRITE("ctrlApiModbusTcp0"));
        {
            expectView(Object.values(emptyEMS.components)[0], emptyEMS, TEST_CONTEXT, "line", History.YEAR_EMPTY, {
                datasets: {
                    data: [DATA("Ladezustand", [10])],
                    labels: LABELS(History.YEAR_EMPTY.dataChannelWithValues?.result?.timestamps ?? []),
                    options: OeTester.ChartOptions.MULTI_LINE_OPTIONS("month", "line", {
                        [ChartAxis.RIGHT]: {
                            scale: { beginAtZero: true, title: { text: "%" }, grid: { display: true }, type: "linear" },
                        },
                        [ChartAxis.LEFT]: { scale: { display: false } },
                    }),
                },
            });
        }
    });
});

export function expectView(
    component: EdgeConfig.Component,
    config: EdgeConfig,
    testContext: TestContext,
    chartType: "line" | "bar",
    channels: OeTester.Types.Channels,
    view: OeChartTester.View,
): void {
    expect(
        TestingUtils.removeFunctions(
            OeChartTester.apply(
                ControllerModbusTcpApiChartComponent.getChartData(
                    component,
                    DummyConfig.convertDummyEdgeConfigToRealEdgeConfig(config),
                    chartType,
                    testContext.translate,
                ),
                chartType,
                channels,
                testContext,
                config,
            ),
        ),
    ).toEqual(TestingUtils.removeFunctions(view));
}
