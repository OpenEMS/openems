// @ts-strict-ignore
import { FormControl, FormGroup } from "@angular/forms";
import { DummyConfig, RANGE_BUTTONS_FROM_FORM_CONTROL_LINE } from "src/app/shared/components/edge/edgeconfig.spec";
import { OeFormlyViewTester } from "src/app/shared/components/shared/testing/tester";

import { TestContext, TestingUtils } from "src/app/shared/components/shared/testing/utils.spec";
import { Edge, EdgeConfig } from "src/app/shared/shared";
import { EvseEnergyLimitComponent } from "./energy-limit";

function expectView(component: EdgeConfig.Component, edge: Edge, viewContext: OeFormlyViewTester.Context, testContext: TestContext, view: OeFormlyViewTester.View, formGroup: FormGroup): void {
    const generatedView = OeFormlyViewTester.apply(EvseEnergyLimitComponent.generateView(testContext.translate, component, edge), viewContext, formGroup);
    expect(generatedView).toEqual(view);
}

describe("EVSE Settings", () => {
    let TEST_CONTEXT;
    beforeEach(async () => TEST_CONTEXT = await TestingUtils.sharedSetup());

    it("+generateView()", () => {
        {
            const component = DummyConfig.from(
                DummyConfig.Component.EVSE_CHARGEPOINT_KEBA_UDP("evseChargePoint0"),
            ).getComponent("evseChargePoint0");
            const edge = DummyConfig.dummyEdge({});
            const VIEW_CONTEXT: OeFormlyViewTester.Context = {};

            expectView(component, edge, VIEW_CONTEXT, TEST_CONTEXT, {
                lines: [
                    RANGE_BUTTONS_FROM_FORM_CONTROL_LINE<number>("manualEnergySessionLimit", 1000, { tickMin: 0, tickMax: 100000, step: 1000 }),
                ], title: "evseChargePoint0",
            }, new FormGroup({
                manualEnergySessionLimit: new FormControl(1000),
            }));
        }
    });
});
