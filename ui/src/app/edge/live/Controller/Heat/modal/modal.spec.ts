import { CHANNEL_LINE, DummyConfig, LINE_INFO } from "src/app/shared/components/edge/edgeconfig.spec";
import { OeFormlyViewTester } from "src/app/shared/components/shared/testing/tester";
import { TestContext, TestingUtils } from "src/app/shared/components/shared/testing/utils.spec";
import { EdgeConfig } from "src/app/shared/shared";
import globalEn from "src/assets/i18n/en.json";
import { environment } from "../../../../../../themes/openems/environments/edge-dev";
import heatEn from "../i18n/en.json";
import { ControllerHeatModalComponent } from "./modal";

const VIEW_CONTEXT = (properties?: {}): OeFormlyViewTester.Context => ({
    "heat0/Status": 1,
    "heat0/ActivePower": 1000,
    "heat0/Temperature": 230,
    "heat0/Mode": 2,
    ...properties,
});

function expectView(component: EdgeConfig.Component, edge: any,
    viewContext: OeFormlyViewTester.Context, testContext: TestContext, view: OeFormlyViewTester.View): void {
    expect(OeFormlyViewTester.apply(ControllerHeatModalComponent.generateView(testContext.translate, component, edge), viewContext))
        .toEqual(view);
}

describe("ControllerHeatModalComponent", () => {
    let testContext: TestContext;

    beforeEach(async () => {
        testContext = await TestingUtils.sharedSetup();
        // Load Heat module translations
        const mergedEn = { ...globalEn, ...heatEn };
        testContext.translate.setTranslation("en", mergedEn, true);
        testContext.translate.use("en");
    });

    it("+generateView() for writable Askoma includes shared lines and settings controls", () => {
        const component = new EdgeConfig.Component("heat0", "ASKOMA", true, false, "Heat.Askoma", {});
        const edge = DummyConfig.dummyEdge({});

        expectView(component, edge, VIEW_CONTEXT(), testContext, {
            title: "ASKOMA",
            lines: [
                CHANNEL_LINE("Status", "Heating is running"),
                CHANNEL_LINE("Heating output", "1.000 W"),
                CHANNEL_LINE("Current temperature", "23 °C"),
                CHANNEL_LINE("Selected mode", "Fast heat"),
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
            ],
        });
    });

    it("+generateView() for read-only Askoma hides writable settings controls", () => {
        const component = new EdgeConfig.Component("heat0", "ASKOMA", true, false, "Heat.Askoma", { readOnly: true });
        const edge = DummyConfig.dummyEdge({});

        expectView(component, edge, VIEW_CONTEXT({ "heat0/Mode": null }), testContext, {
            title: "ASKOMA",
            lines: [
                CHANNEL_LINE("Status", "Heating is running"),
                CHANNEL_LINE("Heating output", "1.000 W"),
                CHANNEL_LINE("Current temperature", "23 °C"),
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
            ],
        });
    });

    it("+generateView() for writable myPV includes shared lines and settings controls", () => {
        const component = new EdgeConfig.Component("heat0", "MyPV", true, false, "Heat.MyPv", {});
        const edge = DummyConfig.dummyEdge({});

        expectView(component, edge, VIEW_CONTEXT(), testContext, {
            title: "MyPV",
            lines: [
                CHANNEL_LINE("Status", "Heating is running"),
                CHANNEL_LINE("Heating output", "1.000 W"),
                CHANNEL_LINE("Current temperature", "23 °C"),
                CHANNEL_LINE("Selected mode", "Fast heat"),
                LINE_INFO("You can make changes to the settings of your AC-Thor heating element in your MyPV app."),
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
        });
    });

    it("+generateView() for read-only myPV hides writable settings controls", () => {
        const component = new EdgeConfig.Component("heat0", "MyPV", true, false, "Heat.MyPv.AcThor9s", { readOnly: true });
        const edge = DummyConfig.dummyEdge({});

        expectView(component, edge, VIEW_CONTEXT({ "heat0/Mode": null }), testContext, {
            title: "MyPV",
            lines: [
                CHANNEL_LINE("Status", "Heating is running"),
                CHANNEL_LINE("Heating output", "1.000 W"),
                CHANNEL_LINE("Current temperature", "23 °C"),
                LINE_INFO("You can make changes to the settings of your AC-Thor heating element in your MyPV app."),
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
        });
    });
});
