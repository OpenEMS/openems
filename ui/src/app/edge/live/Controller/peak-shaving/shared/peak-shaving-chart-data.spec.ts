import { DATA, LABELS, } from "src/app/edge/history/common/energy/chart/chart.constants.spec";
import { DummyConfig } from "src/app/shared/components/edge/edgeconfig.spec";
import { OeTester } from "src/app/shared/components/shared/testing/common";
import { OeChartTester } from "src/app/shared/components/shared/testing/tester";
import { TestContext, TestingUtils, } from "src/app/shared/components/shared/testing/utils.spec";
import { QueryHistoricTimeseriesDataResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse";
import { QueryHistoricTimeseriesEnergyResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse";
import { EdgeConfig } from "src/app/shared/shared";

import { ChartAxis } from "src/app/shared/utils/utils";
import { PeakShavingChartDataBuilder, PeakShavingChartDataOptions, } from "./peak-shaving-chart-data";

function expectView(
    config: EdgeConfig,
    component: EdgeConfig.Component,
    testContext: TestContext,
    channels: OeTester.Types.Channels,
    options: PeakShavingChartDataOptions,
    view: OeChartTester.View,
): void {
    const chartData = PeakShavingChartDataBuilder.build(
        DummyConfig.convertDummyEdgeConfigToRealEdgeConfig(config),
        component,
        testContext.translate,
        options,
    );

    const actual = TestingUtils.removeFunctions(
        OeChartTester.apply(chartData, "line", channels, testContext, config),
    );
    const expected = TestingUtils.removeFunctions(view);

    expect(actual.datasets.data).toEqual(expected.datasets.data);
    expect(actual.datasets.labels).toEqual(expected.datasets.labels);
}

function peakShavingComponent(
    id: string,
    meterId?: string,
): EdgeConfig.Component {
    return new EdgeConfig.Component(
        id,
        id,
        false,
        false,
        "Controller.PeakShaving.Symmetric",
        meterId != null ? { "meter.id": meterId } : {},
        {},
    );
}

describe("PeakShavingChartDataBuilder", () => {
    let TEST_CONTEXT: TestContext;

    beforeEach(async () => (TEST_CONTEXT = await TestingUtils.sharedSetup()));

    it("expectView single-phase", () => {
        const component = peakShavingComponent("ctrlPeakShaving0", "meter0");
        const config = DummyConfig.from(
            DummyConfig.Component.SOCOMEC_GRID_METER("meter0", "Grid"),
        );

        const channels: OeTester.Types.Channels = {
            energyChannelWithValues: new QueryHistoricTimeseriesEnergyResponse(
                "0",
                { data: {} },
            ),
            dataChannelWithValues: new QueryHistoricTimeseriesDataResponse(
                "0",
                {
                    data: {
                        "meter0/ActivePower": [0, 0, 0],
                        "_sum/EssSoc": [0, null, 0],
                        "_sum/EssActivePower": [0, 0, 0],
                        "ctrlPeakShaving0/_PropertyRechargePower": [0, 0, 0],
                        "ctrlPeakShaving0/_PropertyPeakShavingPower": [0, 0, 0],
                    },
                    timestamps: [
                        "2024-01-01T00:00:00Z",
                        "2024-01-01T00:05:00Z",
                        "2024-01-01T00:10:00Z",
                    ],
                },
            ),
        };

        expectView(
            config,
            component,
            TEST_CONTEXT,
            channels,
            { activePowerMode: "single" },
            {
                datasets: {
                    data: [
                        DATA(
                            TEST_CONTEXT.translate.instant(
                                "GENERAL.GRID_BUY_ADVANCED",
                            ),
                            [0, 0, 0],
                        ),
                        DATA(
                            TEST_CONTEXT.translate.instant(
                                "EDGE.INDEX.WIDGETS.PEAKSHAVING.RECHARGE_POWER",
                            ),
                            [0, 0, 0],
                        ),
                        DATA(
                            TEST_CONTEXT.translate.instant(
                                "EDGE.INDEX.WIDGETS.PEAKSHAVING.PEAKSHAVING_POWER",
                            ),
                            [0, 0, 0],
                        ),
                        DATA(
                            TEST_CONTEXT.translate.instant("GENERAL.CHARGE"),
                            [0, 0, 0],
                        ),
                        DATA(
                            TEST_CONTEXT.translate.instant("GENERAL.DISCHARGE"),
                            [0, 0, 0],
                        ),
                        DATA(TEST_CONTEXT.translate.instant("GENERAL.SOC"), [
                            0,
                            null,
                            0,
                        ]),
                    ],
                    labels: LABELS(
                        channels.dataChannelWithValues?.result.timestamps ?? [],
                    ),
                    options: OeTester.ChartOptions.LINE_CHART_OPTIONS(
                        "hour",
                        "line",
                        {
                            [ChartAxis.LEFT]: { scale: { beginAtZero: true } },
                        },
                    ),
                },
            },
        );
    });

    it("expectView three-phase", () => {
        const component = peakShavingComponent("ctrlPeakShaving0", "meter0");
        const config = DummyConfig.from(
            DummyConfig.Component.SOCOMEC_GRID_METER("meter0", "Grid"),
        );

        const channels: OeTester.Types.Channels = {
            energyChannelWithValues: new QueryHistoricTimeseriesEnergyResponse(
                "0",
                { data: {} },
            ),
            dataChannelWithValues: new QueryHistoricTimeseriesDataResponse(
                "0",
                {
                    data: {
                        "meter0/ActivePowerL1": [0, 0],
                        "meter0/ActivePowerL2": [0, 0],
                        "meter0/ActivePowerL3": [0, 0],
                        "_sum/EssSoc": [0, 0],
                        "_sum/EssActivePower": [0, 0],
                        "ctrlPeakShaving0/_PropertyRechargePower": [0, 0],
                        "ctrlPeakShaving0/_PropertyPeakShavingPower": [0, 0],
                    },
                    timestamps: [
                        "2024-01-01T00:00:00Z",
                        "2024-01-01T00:05:00Z",
                    ],
                },
            ),
        };

        expectView(
            config,
            component,
            TEST_CONTEXT,
            channels,
            { activePowerMode: "three-phase" },
            {
                datasets: {
                    data: [
                        DATA("Phase L1", [0, 0]),
                        DATA("Phase L2", [0, 0]),
                        DATA("Phase L3", [0, 0]),
                        DATA(
                            TEST_CONTEXT.translate.instant(
                                "EDGE.INDEX.WIDGETS.PEAKSHAVING.RECHARGE_POWER",
                            ),
                            [0, 0],
                        ),
                        DATA(
                            TEST_CONTEXT.translate.instant(
                                "EDGE.INDEX.WIDGETS.PEAKSHAVING.PEAKSHAVING_POWER",
                            ),
                            [0, 0],
                        ),
                        DATA(
                            TEST_CONTEXT.translate.instant("GENERAL.CHARGE"),
                            [0, 0],
                        ),
                        DATA(
                            TEST_CONTEXT.translate.instant("GENERAL.DISCHARGE"),
                            [0, 0],
                        ),
                        DATA(
                            TEST_CONTEXT.translate.instant("GENERAL.SOC"),
                            [0, 0],
                        ),
                    ],
                    labels: LABELS(
                        channels.dataChannelWithValues?.result.timestamps ?? [],
                    ),
                    options: OeTester.ChartOptions.LINE_CHART_OPTIONS(
                        "hour",
                        "line",
                        {
                            [ChartAxis.LEFT]: { scale: { beginAtZero: true } },
                        },
                    ),
                },
            },
        );
    });

    it("expectView single-phase with non-zero values and converter behavior", () => {
        const component = peakShavingComponent("ctrlPeakShaving0", "meter0");
        const config = DummyConfig.from(
            DummyConfig.Component.SOCOMEC_GRID_METER("meter0", "Grid"),
        );

        const channels: OeTester.Types.Channels = {
            energyChannelWithValues: new QueryHistoricTimeseriesEnergyResponse(
                "0",
                { data: {} },
            ),
            dataChannelWithValues: new QueryHistoricTimeseriesDataResponse(
                "0",
                {
                    data: {
                        "meter0/ActivePower": [1000, 2000, -5000],
                        "_sum/EssSoc": [0.5, 0.6, 0.7],
                        "_sum/EssActivePower": [-3000, 4000, 0],
                        "ctrlPeakShaving0/_PropertyRechargePower": [
                            5000, 6000, 7000,
                        ],
                        "ctrlPeakShaving0/_PropertyPeakShavingPower": [
                            3000, 3000, 3000,
                        ],
                    },
                    timestamps: [
                        "2024-01-01T00:00:00Z",
                        "2024-01-01T00:05:00Z",
                        "2024-01-01T00:10:00Z",
                    ],
                },
            ),
        };

        expectView(
            config,
            component,
            TEST_CONTEXT,
            channels,
            { activePowerMode: "single" },
            {
                datasets: {
                    data: [
                        DATA(
                            TEST_CONTEXT.translate.instant(
                                "GENERAL.GRID_BUY_ADVANCED",
                            ),
                            [1, 2, 0],
                        ),
                        DATA(
                            TEST_CONTEXT.translate.instant(
                                "EDGE.INDEX.WIDGETS.PEAKSHAVING.RECHARGE_POWER",
                            ),
                            [5, 6, 7],
                        ),
                        DATA(
                            TEST_CONTEXT.translate.instant(
                                "EDGE.INDEX.WIDGETS.PEAKSHAVING.PEAKSHAVING_POWER",
                            ),
                            [3, 3, 3],
                        ),
                        DATA(
                            TEST_CONTEXT.translate.instant("GENERAL.CHARGE"),
                            [3, 0, 0],
                        ),
                        DATA(
                            TEST_CONTEXT.translate.instant("GENERAL.DISCHARGE"),
                            [0, 4, 0],
                        ),
                        DATA(
                            TEST_CONTEXT.translate.instant("GENERAL.SOC"),
                            [0.5, 0.6, 0.7],
                        ),
                    ],
                    labels: LABELS(
                        channels.dataChannelWithValues?.result.timestamps ?? [],
                    ),
                    options: OeTester.ChartOptions.LINE_CHART_OPTIONS(
                        "hour",
                        "line",
                        {
                            [ChartAxis.LEFT]: { scale: { beginAtZero: true } },
                        },
                    ),
                },
            },
        );
    });
});
