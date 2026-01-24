import { FormControl, FormGroup } from "@angular/forms";
import { DummyConfig, SVG_LINE as IMAGE_LINE, LINE_INFO, LINE_RADIO_BUTTONS_FROM_FORM_CONTROL } from "src/app/shared/components/edge/edgeconfig.spec";
import { OeFormlyViewTester } from "src/app/shared/components/shared/testing/tester";

import { TestContext, TestingUtils } from "src/app/shared/components/shared/testing/utils.spec";
import { Edge, EdgeConfig } from "src/app/shared/shared";
import { EvsePhaseSwitchingComponent, PhaseSwitching } from "./phase-switching";

function expectView(component: EdgeConfig.Component, edge: Edge, viewContext: OeFormlyViewTester.Context, testContext: TestContext, view: OeFormlyViewTester.View, formGroup: FormGroup): void {
    const generatedView = OeFormlyViewTester.apply(EvsePhaseSwitchingComponent.generateView(testContext.translate, component, edge), viewContext, formGroup);
    expect(generatedView).toEqual(view);
}

describe("EVSE phase switching", () => {
    let TEST_CONTEXT: TestContext;
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
                    IMAGE_LINE({
                        url: "assets/img/phasenumschaltung.svg", width: 100, style: {
                            maxWidth: "30rem", justifySelf: "center",
                            paddingBottom: "var(--ion-padding)",
                        },
                    }),
                    LINE_INFO(TEST_CONTEXT.translate.instant("EDGE.INDEX.WIDGETS.EVCS.PHASE_SWITCHING_INFO"), "font-weight: bold; text-align: center; font-size: 1rem; padding-bottom: calc(var(--ion-padding) * 4)"),
                    LINE_RADIO_BUTTONS_FROM_FORM_CONTROL("phase-switching", EvsePhaseSwitchingComponent.formControlName,
                        [
                            {
                                name: TEST_CONTEXT.translate.instant("EDGE.INDEX.WIDGETS.EVCS.FORCE_SINGLE_PHASE"),
                                value: PhaseSwitching.FORCE_SINGLE_PHASE,
                                style: {
                                    "color": "red",
                                    "fontWeight": "bold",
                                },
                            },
                            {
                                name: TEST_CONTEXT.translate.instant("EDGE.INDEX.WIDGETS.EVCS.FORCE_THREE_PHASE"),
                                value: PhaseSwitching.FORCE_THREE_PHASE,
                            },
                            // {
                            //     name: TEST_CONTEXT.translate.instant("EDGE.INDEX.WIDGETS.EVCS.AUTOMATIC_SWITCHING"),
                            //     value: PhaseSwitching.AUTOMATIC_SWITCHING, // not implemented yet
                            //     disabled: true,
                            // },
                        ]),
                ], title: "evseChargePoint0",
            }, new FormGroup({
                manualEnergySessionLimit: new FormControl(1000),
            }));
        }
    });
});
