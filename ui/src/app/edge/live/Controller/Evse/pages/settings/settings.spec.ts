// @ts-strict-ignore
import { FormControl, FormGroup } from "@angular/forms";
import { DummyConfig, RANGE_BUTTONS_FROM_FORM_CONTROL_LINE } from "src/app/shared/components/edge/EDGECONFIG.SPEC";
import { OeFormlyViewTester } from "src/app/shared/components/shared/testing/tester";

import { TestContext, TestingUtils } from "src/app/shared/components/shared/testing/UTILS.SPEC";
import { Edge, EdgeConfig } from "src/app/shared/shared";
import { EvseSettingsComponent } from "./settings";

function expectView(component: EDGE_CONFIG.COMPONENT, edge: Edge, viewContext: OE_FORMLY_VIEW_TESTER.CONTEXT, testContext: TestContext, view: OE_FORMLY_VIEW_TESTER.VIEW, formGroup: FormGroup): void {
  const generatedView = OE_FORMLY_VIEW_TESTER.APPLY(EVSE_SETTINGS_COMPONENT.GENERATE_VIEW(TEST_CONTEXT.TRANSLATE, component, edge), viewContext, formGroup);
  expect(generatedView).toEqual(view);
}

describe("EVSE Settings", () => {
  let TEST_CONTEXT;
  beforeEach(async () => TEST_CONTEXT = await TESTING_UTILS.SHARED_SETUP());

  it("+generateView()", () => {
    {
      const component = DUMMY_CONFIG.FROM(
        DUMMY_CONFIG.COMPONENT.EVSE_CHARGEPOINT_KEBA_UDP("evseChargePoint0"),
      ).getComponent("evseChargePoint0");
      const edge = DUMMY_CONFIG.DUMMY_EDGE({});
      const VIEW_CONTEXT: OE_FORMLY_VIEW_TESTER.CONTEXT = {};

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
