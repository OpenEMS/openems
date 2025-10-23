import { DummyConfig, LINE_BUTTONS_FROM_FORM_CONTROL, LINE_INFO } from "src/app/shared/components/edge/edgeconfig.spec";
import { OeFormlyViewTester } from "src/app/shared/components/shared/testing/tester";
import { TestContext, TestingUtils } from "src/app/shared/components/shared/testing/utils.spec";

import { expectView } from "./constants.spec";

const VIEW_CONTEXT = (properties?: {}): OeFormlyViewTester.Context => ({
    ...properties,
});

describe("FixDigitalOutput - Modal", () => {
    let TEST_CONTEXT: TestContext;
    beforeEach(async () => TEST_CONTEXT = await TestingUtils.sharedSetup());

    it("+generateView()", () => {
        {
            // No Meters
            const EMS = DummyConfig.from(
                DummyConfig.Component.CONTROLLER_IO_FIX_DIGITAL_OUTPUT("ctrlFixDigitalOutput0"),
            );;

            const edge = DummyConfig.dummyEdge({});

            expectView(EMS, edge, VIEW_CONTEXT(), TEST_CONTEXT, {
                title: "ctrlFixDigitalOutput0",
                lines: [
                    LINE_INFO("Modus"),
                    LINE_BUTTONS_FROM_FORM_CONTROL("Modus", "isOn", [{
                        name: TEST_CONTEXT.translate.instant("General.on"),
                        value: 1,
                        icon: { color: "success", name: "play-outline", size: "medium" },
                    },
                    {
                        name: TEST_CONTEXT.translate.instant("General.off"),
                        value: 0,
                        icon: { color: "danger", name: "power-outline", size: "medium" },
                    }]),
                ],
            });
        }

    });
});
