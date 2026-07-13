import { CHANNEL_LINE, DummyConfig, LINE_HORIZONTAL, LINE_INPUT_FROM_FORM_CONTROL, } from "src/app/shared/components/edge/edgeconfig.spec";
import { OeFormlyViewTester } from "src/app/shared/components/shared/testing/tester";
import { TestContext, TestingUtils, } from "src/app/shared/components/shared/testing/utils.spec";
import { ChannelAddress, CurrentData, EdgeConfig } from "src/app/shared/shared";

import { Role } from "src/app/shared/type/role";
import { SharedControllerPeakShaving } from "../../shared/shared";
import { expectView } from "./constants.spec";
import { ControllerPeakShavingSymmetricSettingsComponent } from "./settings";

const VIEW_CONTEXT = (properties?: {}): OeFormlyViewTester.Context => ({
    "meter0/ActivePower": 1000,
    ...properties,
});

function peakShavingComponent(
    id: string,
    meterId: string | null = "meter0",
): EdgeConfig.Component {
    return new EdgeConfig.Component(
        id,
        "Peak Shaving",
        false,
        false,
        "Controller.PeakShaving.Symmetric",
        meterId == null ? {} : { "meter.id": meterId },
        {},
    );
}

function createComponent(component: EdgeConfig.Component): any {
    const instance = Object.create(
        ControllerPeakShavingSymmetricSettingsComponent.prototype,
    );

    const config = jasmine.createSpyObj<EdgeConfig>("EdgeConfig", [
        "getComponentSafely",
    ]);
    config.getComponentSafely.and.returnValue(component);

    const edge = DummyConfig.dummyEdge({});
    spyOn(edge, "getCurrentConfig").and.returnValue(config);

    instance.routeService = {
        getRouteParam: () => component.id,
    };
    instance.service = {
        currentEdge: () => edge,
    };
    instance.translate = {
        instant: (key: string) => key,
    };
    instance.form = instance["getFormGroup"]();
    instance.component = null;
    instance.skipCurrentData = false;

    return instance;
}

describe("ControllerPeakShavingSymmetricSettingsComponent", () => {
    let TEST_CONTEXT: TestContext;

    beforeEach(async () => {
        TEST_CONTEXT = await TestingUtils.sharedSetup();
    });

    it("+getFormlyGeneralView() owner: builds expected view with input lines", () => {
        const component = peakShavingComponent("ctrlPeakShaving0");
        const edge = DummyConfig.dummyEdge({});

        expectView(component, edge, VIEW_CONTEXT(), TEST_CONTEXT, {
            title: "Peak Shaving",
            lines: [
                CHANNEL_LINE("Gemessener Wert", "1.000 W"),
                LINE_HORIZONTAL,
                LINE_INPUT_FROM_FORM_CONTROL(
                    TEST_CONTEXT.translate.instant(
                        "EDGE.INDEX.WIDGETS.PEAKSHAVING.PEAKSHAVING_POWER",
                    ),
                    "peakShavingPower",
                    "W",
                    null,
                ),
                LINE_INPUT_FROM_FORM_CONTROL(
                    TEST_CONTEXT.translate.instant(
                        "EDGE.INDEX.WIDGETS.PEAKSHAVING.RECHARGE_POWER",
                    ),
                    "rechargePower",
                    "W",
                    null,
                ),
            ],
        });
    });

    it("+getFormlyGeneralView() non-owner: builds expected view without input lines", () => {
        const component = peakShavingComponent("ctrlPeakShaving0");
        const edge = DummyConfig.dummyEdge({ role: Role.GUEST });

        expect(edge.roleIsAtLeast(Role.OWNER)).toBeFalse();

        expectView(component, edge, VIEW_CONTEXT(), TEST_CONTEXT, {
            title: "Peak Shaving",
            lines: [
                CHANNEL_LINE("Gemessener Wert", "1.000 W"),
                LINE_HORIZONTAL,
            ],
        });
    });

    it("#onCurrentData() writes peak shaving and recharge power into the form", () => {
        const component = peakShavingComponent("ctrlPeakShaving0");
        const instance = createComponent(component);
        instance.component = component;

        const currentData: CurrentData = {
            allComponents: {
                "ctrlPeakShaving0/_PropertyPeakShavingPower": 3500,
                "ctrlPeakShaving0/_PropertyRechargePower": 2200,
            },
        };

        instance["onCurrentData"](currentData);

        expect(instance.form.controls["peakShavingPower"].value).toBe(3500);
        expect(instance.form.controls["rechargePower"].value).toBe(2200);
        expect(instance.form.controls["peakShavingPower"].pristine).toBeTrue();
        expect(instance.form.controls["rechargePower"].pristine).toBeTrue();
    });

    it("#getFormGroup() creates the expected controls", () => {
        const instance = createComponent(
            peakShavingComponent("ctrlPeakShaving0"),
        );

        const form = instance["getFormGroup"]();

        expect(Object.keys(form.controls).sort()).toEqual([
            "peakShavingPower",
            "rechargePower",
        ]);
        expect(form.getRawValue()).toEqual({
            peakShavingPower: null,
            rechargePower: null,
        });
    });

    it("#getChannelAddresses() delegates to the shared helper", async () => {
        const component = peakShavingComponent("ctrlPeakShaving0");
        const instance = createComponent(component);
        const expectedChannels = [
            new ChannelAddress("ctrlPeakShaving0", "_PropertyPeakShavingPower"),
        ];

        spyOn(SharedControllerPeakShaving, "getChannelAddresses").and.resolveTo(
            expectedChannels,
        );

        const channels = await instance["getChannelAddresses"]();

        expect(
            SharedControllerPeakShaving.getChannelAddresses,
        ).toHaveBeenCalledWith(instance.service, instance.routeService);
        expect(channels).toEqual(expectedChannels);
    });
});
