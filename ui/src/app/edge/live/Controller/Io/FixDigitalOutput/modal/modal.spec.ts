import { CHANNEL_LINE, DummyConfig, LINE_BUTTONS_FROM_FORM_CONTROL } from "src/app/shared/components/edge/edgeconfig.spec";
import { OeFormlyViewTester } from "src/app/shared/components/shared/testing/tester";
import { TestContext, TestingUtils } from "src/app/shared/components/shared/testing/utils.spec";

import { expectView } from "./constants.spec";

const VIEW_CONTEXT = (properties?: {}): OeFormlyViewTester.Context => ({
    "io0/Relay3": 1,
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

            expectView(EMS, edge, VIEW_CONTEXT({ outputChannelAddress: "io0/Relay3" }), TEST_CONTEXT, {
                title: "ctrlFixDigitalOutput0",
                lines: [
                    CHANNEL_LINE(TEST_CONTEXT.translate.instant("GENERAL.CURRENT_STATUS"), "1"),
                    LINE_BUTTONS_FROM_FORM_CONTROL("Modus", "isOn", [{
                        name: TEST_CONTEXT.translate.instant("GENERAL.ON"),
                        value: 1,
                        icon: { color: "success", name: "play-outline", size: "medium" },
                    },
                    {
                        name: TEST_CONTEXT.translate.instant("GENERAL.OFF"),
                        value: 0,
                        icon: { color: "danger", name: "power-outline", size: "medium" },
                    }]),
                ],
            });
        }

    });
});
