import { CHANNEL_LINE, DummyConfig, LINE_INFO } from "src/app/shared/components/edge/edgeconfig.spec";
import { OeFormlyViewTester } from "src/app/shared/components/shared/testing/tester";
import { TestContext, TestingUtils } from "src/app/shared/components/shared/testing/utils.spec";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";
import globalEn from "src/assets/i18n/en.json";
import {environment} from "../../../../../../themes/openems/environments/edge-dev";
import heatEn from "../i18n/en.json";
import { ControllerHeatHomeComponent } from "./new-navigation";

const VIEW_CONTEXT = (properties?: {}): OeFormlyViewTester.Context => ({
    "heat0/Status": 0,
    "heat0/ActivePower": 1000,
    "heat0/Temperature": 230,
    ...properties,
});

function expectView(component: EdgeConfig.Component, edge: any, isMyPv: boolean, isAskoma: boolean,
    viewContext: OeFormlyViewTester.Context, testContext: TestContext, view: OeFormlyViewTester.View): void {
    expect(OeFormlyViewTester.apply(ControllerHeatHomeComponent.generateView(testContext.translate, component, edge, isMyPv, isAskoma), viewContext))
        .toEqual(view);
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

    it("+generateView() for Askoma includes Askoma icon and shared channel lines", () => {
        const component = new EdgeConfig.Component("heat0", "ASKOMA", true, false, "Heat.Askoma", {});
        const edge = DummyConfig.dummyEdge({});

        expectView(component, edge, false, true, VIEW_CONTEXT(), testContext, {
            title: "ASKOMA",
            lines: [
                {
                    type: "image-line",
                    img: {
                        url: environment.images.HEAT.ASKOMA.HEATING_ELEMENT,
                        width: 50,
                        style: {
                            maxWidth: "30rem",
                            justifySelf: "center",
                            paddingBottom: "var(--ion-padding)",
                        },
                    },
                },
                CHANNEL_LINE("Status", "Heating is running"),
                CHANNEL_LINE("Heating output", "1.000 W"),
                CHANNEL_LINE("Current temperature", "23 °C"),
                CHANNEL_LINE("Selected mode", ""),
            ],
        });
    });

    it("+generateView() for MyPV shows only the MyPV info line", () => {
        const component = new EdgeConfig.Component("heat1", "Heat", true, false, "Heat.MyPv.AcThor9s", {});
        const edge = DummyConfig.dummyEdge({});

        expectView(component, edge, true, false, VIEW_CONTEXT(), testContext, {
            title: "Heat",
            lines: [
                LINE_INFO("You can make changes to the settings of your AC-Thor heating element in your MyPV app."),
            ],
        });
    });

    it("#getChannelAddresses() resolves shared channel subscriptions for the routed Askoma component", async () => {
        const component = new EdgeConfig.Component("heat0", "ASKOMA", true, false, "Heat.Askoma", {});
        const instance = createComponent(testContext, component);

        const channelAddresses = await instance["getChannelAddresses"]();

        await expect(channelAddresses).toEqual([
            new ChannelAddress("heat0", "Status"),
            new ChannelAddress("heat0", "ActivePower"),
            new ChannelAddress("heat0", "Temperature"),
            new ChannelAddress("heat0", "_PropertyMode"),
        ]);
    });
});
