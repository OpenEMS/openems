import { DummyConfig, LINE_BUTTONS_FROM_FORM_CONTROL, LINE_INFO } from "src/app/shared/components/edge/EDGECONFIG.SPEC";
import { OeFormlyViewTester } from "src/app/shared/components/shared/testing/tester";
import { TestContext, TestingUtils } from "src/app/shared/components/shared/testing/UTILS.SPEC";

import { expectView } from "./CONSTANTS.SPEC";

const VIEW_CONTEXT = (properties?: {}): OE_FORMLY_VIEW_TESTER.CONTEXT => ({
    ...properties,
});

describe("FixDigitalOutput - Modal", () => {
    let TEST_CONTEXT: TestContext;
    beforeEach(async () => TEST_CONTEXT = await TESTING_UTILS.SHARED_SETUP());

    it("+generateView()", () => {
        {
            // No Meters
            const EMS = DUMMY_CONFIG.FROM(
                DUMMY_CONFIG.COMPONENT.CONTROLLER_IO_FIX_DIGITAL_OUTPUT("ctrlFixDigitalOutput0"),
            );;

            const edge = DUMMY_CONFIG.DUMMY_EDGE({});

            expectView(EMS, edge, VIEW_CONTEXT(), TEST_CONTEXT, {
                title: "ctrlFixDigitalOutput0",
                lines: [
                    LINE_INFO("Modus"),
                    LINE_BUTTONS_FROM_FORM_CONTROL("Modus", "isOn", [{
                        name: TEST_CONTEXT.TRANSLATE.INSTANT("GENERAL.ON"),
                        value: 1,
                        icon: { color: "success", name: "power-outline", size: "medium" },
                    },
                    {
                        name: TEST_CONTEXT.TRANSLATE.INSTANT("GENERAL.OFF"),
                        value: 0,
                        icon: { color: "danger", name: "power-outline", size: "medium" },
                    }]),
                ],
            });
        }

    });
});
