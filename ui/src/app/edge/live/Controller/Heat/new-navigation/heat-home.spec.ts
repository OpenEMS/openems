import { TimeLineChartComponent } from "src/app/shared/components/chart/timeline-chart/timeline-chart";
import { EnergySchedulerV2 } from "src/app/shared/components/edge/config-components/energy/energy";
import { GetSchedule } from "src/app/shared/components/edge/config-components/energy/getSchedule";
import { CHANNEL_LINE, CHART_LINE, DummyConfig, LINE_INFO } from "src/app/shared/components/edge/edgeconfig.spec";
import { OeFormlyViewTester } from "src/app/shared/components/shared/testing/tester";
import { TestContext, TestingUtils } from "src/app/shared/components/shared/testing/utils.spec";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";
import globalEn from "src/assets/i18n/en.json";
import { environment } from "../../../../../../themes/openems/environments/edge-dev";
import heatEn from "../i18n/en.json";
import { ChannelMode, HeatStatus } from "../shared/shared";
import { HeatModeChartComponent } from "./chart/heat-mode-chart";
import { HeatPowerChartComponent } from "./chart/heat-power-chart";
import { HeatStatusChartComponent } from "./chart/heat-status-chart";
import { ControllerHeatHomeComponent } from "./heat-home";

const VIEW_CONTEXT = (properties?: {}): OeFormlyViewTester.Context => ({
    "heat0/Status": 1,
    "heat0/ActivePower": 1000,
    "heat0/Temperature": 230,
    "heat0/Mode": 2,
    ...properties,
});

function expectView(
    component: EdgeConfig.Component,
    edge: any,
    viewContext: OeFormlyViewTester.Context,
    testContext: TestContext,
    view: OeFormlyViewTester.View,
    energyScheduler: EnergySchedulerV2 = mockEnergySchedulerWithSchedule(),
): void {
    expect(
        OeFormlyViewTester.apply(
            ControllerHeatHomeComponent.generateView(testContext.translate, component, edge, energyScheduler),
            viewContext,
        ),
    ).toEqual(view);
}

function mockEnergySchedulerWithSchedule(): EnergySchedulerV2 {
    return {
        schedule: {},
    } as EnergySchedulerV2;
}

function createComponent(testContext: TestContext, component: EdgeConfig.Component): any {
    const edge = DummyConfig.dummyEdge({});
    const config = {
        getComponentSafely: () => component,
    };

    edge.getCurrentConfig = () => config as any;

    const instance = Object.create(ControllerHeatHomeComponent.prototype);
    instance.translate = testContext.translate;
    instance.routeService = {
        getRouteParam: () => component.id,
    };
    instance.service = {
        currentEdge: () => edge,
    };

    return instance;
}

describe("ControllerHeatHomeComponent", () => {
    let testContext: TestContext;

    beforeEach(async () => {
        testContext = await TestingUtils.sharedSetup();
        const mergedEn = { ...globalEn, ...heatEn };
        testContext.translate.setTranslation("en", mergedEn, true);
        testContext.translate.use("en");
    });

    describe("Askoma", () => {
        it("+generateView() shows temperature and charts", () => {
            const component = new EdgeConfig.Component("heat0", "ASKOMA", true, false, "Heat.Askoma", {});
            const edge = DummyConfig.dummyEdge({});

            expectView(component, edge, VIEW_CONTEXT(), testContext, {
                title: "ASKOMA",
                lines: [
                    CHANNEL_LINE("Current temperature", "23 °C"),
                    CHART_LINE(TimeLineChartComponent),
                    CHANNEL_LINE("Status", "Heating is running"),
                    CHART_LINE(HeatStatusChartComponent),
                    CHANNEL_LINE("Heating output", "1.000 W"),
                    CHART_LINE(HeatPowerChartComponent),
                    CHANNEL_LINE("Mode", "Fast heat"),
                    CHART_LINE(HeatModeChartComponent),
                ],
            });
        });

        it("#getChannelAddresses() resolves shared channel subscriptions for the routed component", async () => {
            const component = new EdgeConfig.Component("heat0", "ASKOMA", true, false, "Heat.Askoma", {});
            const instance = createComponent(testContext, component);

            const channelAddresses = await instance["getChannelAddresses"]();

            await expect(channelAddresses).toEqual([
                new ChannelAddress("heat0", "Status"),
                new ChannelAddress("heat0", "ActivePower"),
                new ChannelAddress("heat0", "Temperature"),
                new ChannelAddress("heat0", "Mode"),
                new ChannelAddress("heat0", "_PropertyMode"),
            ]);
        });
    });

    describe("MyPV", () => {
        it("+generateView() uses deprecated legacy lines for read-only AC-Thor 9s when EnergyScheduler is unavailable", () => {
            const component = new EdgeConfig.Component("heat1", "my-PV", true, false, "Heat.MyPv.AcThor9s", {
                readOnly: true,
            });
            const edge = DummyConfig.dummyEdge({});

            expectView(
                component,
                edge,
                VIEW_CONTEXT({
                    "heat1/Status": HeatStatus.NO_CONTROL_SIGNAL,
                    "heat1/ActivePower": 0,
                    "heat1/Temperature": 740,
                    "heat1/Mode": null,
                }),
                testContext,
                {
                    title: "my-PV",
                    lines: [
                        CHANNEL_LINE("Status", "No heating"),
                        CHANNEL_LINE("Heating output", "0 W"),
                        CHANNEL_LINE("Current temperature", "74 °C"),
                        LINE_INFO(
                            "You can make changes to the settings of your AC-Thor heating element in your MyPV app.",
                        ),
                        {
                            type: "image-line",
                            img: {
                                url: environment.images.HEAT.MYPV.HEATING_ELEMENT,
                                width: 50,
                                style: {
                                    maxWidth: "30rem",
                                    justifySelf: "center",
                                    paddingBottom: "var(--ion-padding)",
                                },
                            },
                        },
                    ],
                },
                {
                    schedule: GetSchedule.Response.empty,
                } as EnergySchedulerV2,
            );
        });

        it("+generateView() for writable component shows lines and charts without the info line", () => {
            const component = new EdgeConfig.Component("heat1", "my-PV", true, false, "Heat.MyPv", { readOnly: false });
            const edge = DummyConfig.dummyEdge({});

            expectView(
                component,
                edge,
                VIEW_CONTEXT({
                    "heat1/Status": HeatStatus.EXCESS,
                    "heat1/ActivePower": 1000,
                    "heat1/Temperature": 230,
                    "heat1/Mode": ChannelMode.FAST_HEAT,
                }),
                testContext,
                {
                    title: "my-PV",
                    lines: [
                        CHANNEL_LINE("Current temperature", "23 °C"),
                        CHART_LINE(TimeLineChartComponent),
                        CHANNEL_LINE("Status", "Heating is running"),
                        CHART_LINE(HeatStatusChartComponent),
                        CHANNEL_LINE("Heating output", "1.000 W"),
                        CHART_LINE(HeatPowerChartComponent),
                        CHANNEL_LINE("Mode", "Fast heat"),
                        CHART_LINE(HeatModeChartComponent),
                    ],
                },
            );
        });

        it("+generateView() for read-only component keeps shared channel lines without the info line", () => {
            const component = new EdgeConfig.Component("heat1", "my-PV", true, false, "Heat.MyPv", { readOnly: true });
            const edge = DummyConfig.dummyEdge({});

            expectView(
                component,
                edge,
                VIEW_CONTEXT({
                    "heat1/Status": HeatStatus.EXCESS,
                    "heat1/ActivePower": 1000,
                    "heat1/Temperature": 230,
                    "heat1/Mode": ChannelMode.FAST_HEAT,
                }),
                testContext,
                {
                    title: "my-PV",
                    lines: [
                        CHANNEL_LINE("Current temperature", "23 °C"),
                        CHART_LINE(TimeLineChartComponent),
                        CHANNEL_LINE("Status", "Heating is running"),
                        CHART_LINE(HeatStatusChartComponent),
                        CHANNEL_LINE("Heating output", "1.000 W"),
                        CHART_LINE(HeatPowerChartComponent),
                        CHANNEL_LINE("Mode", "Fast heat"),
                        CHART_LINE(HeatModeChartComponent),
                    ],
                },
            );
        });
    });
});
