import { FormControl, FormGroup } from "@angular/forms";
import { DummyConfig } from "src/app/shared/components/edge/edgeconfig.spec";
import { TestContext, TestingUtils } from "src/app/shared/components/shared/testing/utils.spec";
import { ChannelAddress, CurrentData, EdgeConfig } from "src/app/shared/shared";
import globalEn from "src/assets/i18n/en.json";
import { environment } from "src/environments";
import heatEn from "../i18n/en.json";
import { ControllerHeatSettingsComponent, Mode } from "./settings";

function createComponent(component: EdgeConfig.Component): any {
    const instance = Object.create(ControllerHeatSettingsComponent.prototype);

    instance.routeService = {
        getRouteParam: () => component.id,
    };
    instance.service = {
        getConfig: async () => ({
            getComponentSafely: () => component,
        }),
    };
    instance.settingsForm = new FormGroup({
        [ControllerHeatSettingsComponent.FORM_CONTROL_NAME]: new FormControl<Mode | null>(null),
    });
    instance.form = instance["getFormGroup"]();
    instance.component = null;
    instance.modeChannel = null;
    instance.skipCurrentData = false;

    return instance;
}

describe("ControllerHeatSettingsComponent", () => {
    let testContext: TestContext;

    beforeEach(async () => {
        testContext = await TestingUtils.sharedSetup();
        const mergedEn = { ...globalEn, ...heatEn };
        testContext.translate.setTranslation("en", mergedEn,true);
        testContext.translate.use("en");
    });

    it("+generateView()", () => {
        const component = new EdgeConfig.Component("heat0", "ASKOMA", true, false, "Heat.Askoma", {});
        const edge = DummyConfig.dummyEdge({});

        const view = ControllerHeatSettingsComponent.generateView(component, edge, testContext.translate);

        expect(view.title).toBe("ASKOMA");
        expect(view.component).toBe(component);
        expect(view.edge).toBe(edge);
        expect(view.lines.length).toBe(2);
        expect(view.lines[0]).toEqual({
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
        });
        expect(view.lines[1]).toEqual({
            type: "radio-buttons-from-form-control-line",
            name: "select-mode",
            controlName: "mode",
            buttons: [
                {
                    value: Mode.FAST_HEAT,
                    name: "Quick heating",
                    description: "The Askoma heating rod automatically selects the equivalent heating level.\nThe maximum power duration is 10 hours.\nAfter that, it makes 1h pause.",
                    icon: { color: "success", name: "oe-consumption", size: "medium" },

                },
                {
                    value: Mode.SURPLUS,
                    name: "PV surplus",
                    description: "Enables even heating of different buffer tanks. Activation happens automatically, perfectly adapted to your excess and available PV energy.",
                    icon: { color: "primary", name: "oe-production", size: "medium" },
                },
                {
                    value: Mode.OFF,
                    name: "No heating",
                },
            ],
        });
        expect(testContext.translate).toBeDefined();
    });

    it("+generateView() for read-only Askoma hides writable settings controls", () => {
        const component = new EdgeConfig.Component("heat0", "ASKOMA", true, false, "Heat.Askoma", { readOnly: true });
        const edge = DummyConfig.dummyEdge({});

        const view = ControllerHeatSettingsComponent.generateView(component, edge, testContext.translate);

        expect(view.lines.find(line => line.type === "image-line")).toBeDefined();
        expect(view.lines.find(line => line.type === "radio-buttons-from-form-control-line")).toBeUndefined();
    });

    it("#getChannelAddresses() subscribes to _PropertyMode only for Askoma", async () => {
        const askomaComponent = new EdgeConfig.Component("heat0", "ASKOMA", true, false, "Heat.Askoma", {});
        const askomaInstance = createComponent(askomaComponent);

        const askomaChannels = await askomaInstance["getChannelAddresses"]();

        expect(askomaChannels).toEqual([new ChannelAddress("heat0", "_PropertyMode")]);

        const otherComponent = new EdgeConfig.Component("heat1", "Heat", true, false, "Heat.MyPv", {});
        const otherInstance = createComponent(otherComponent);

        await expectAsync(otherInstance["getChannelAddresses"]()).toBeResolvedTo([]);
    });

    it("#onCurrentData() writes _PropertyMode into the form control", () => {
        const component = new EdgeConfig.Component("heat0", "ASKOMA", true, false, "Heat.Askoma", {});
        const instance = createComponent(component);
        instance.component = component;
        const currentData: CurrentData = {
            allComponents: {
                "heat0/_PropertyMode": Mode.SURPLUS,
            },
        };

        instance["onCurrentData"](currentData);

        expect(instance.form.controls[ControllerHeatSettingsComponent.FORM_CONTROL_NAME].value).toBe(Mode.SURPLUS);
        expect(instance.form.controls[ControllerHeatSettingsComponent.FORM_CONTROL_NAME].pristine).toBeTrue();
    });
});
