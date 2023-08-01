import { BUTTONS_FROM_CHANNEL_LINE, ONLY_NAME_LINE } from "src/app/edge/live/common/grid/modal/constants.spec";
import { CONTROLLER_IO_FIX_DIGITAL_OUTPUT, DummyConfig } from "src/app/shared/edge/edgeconfig.spec";
import { OeFormlyViewTester } from "src/app/shared/genericComponents/shared/tester";
import { sharedSetup } from "src/app/shared/test/utils.spec";
import { Role } from "src/app/shared/type/role";

import { expectView } from "./constants.spec";

const VIEW_CONTEXT = (isOn: boolean): OeFormlyViewTester.Context => ({
  "ctrlIoFixDigitalOutput0/_PropertyIsOn": isOn ? 1 : 0 // false
});

describe('ExampleSystemsTest', () => {
  let TEST_CONTEXT;
  beforeEach(() => TEST_CONTEXT = sharedSetup());

  it('#ModalComponent.generateView() FixDigitalOutput', () => {

    // Relay an
    {
      const EMS = DummyConfig.from(
        CONTROLLER_IO_FIX_DIGITAL_OUTPUT("ctrlIoFixDigitalOutput0")
      );
      expectView(EMS, Role.ADMIN, VIEW_CONTEXT(true), TEST_CONTEXT, {
        title: "ctrlIoFixDigitalOutput0",
        lines: [
          ONLY_NAME_LINE("Modus"),
          BUTTONS_FROM_CHANNEL_LINE([{
            name: "An",
            value: "true",
            icon: { color: "success", size: "small", name: "power-outline" }
          },
          {
            name: "Aus",
            value: "false",
            icon: { color: "danger", size: "small", name: "power-outline" }
          }], true)
        ]
      }, "ctrlIoFixDigitalOutput0");
    }

    // Relay aus
    {
      const EMS = DummyConfig.from(
        CONTROLLER_IO_FIX_DIGITAL_OUTPUT("ctrlIoFixDigitalOutput0", "Terassenbeleuchtung")
      );
      expectView(EMS, Role.ADMIN, VIEW_CONTEXT(false), TEST_CONTEXT, {
        title: "Terassenbeleuchtung",
        lines: [
          ONLY_NAME_LINE("Modus"),
          BUTTONS_FROM_CHANNEL_LINE([{
            name: "An",
            value: "true",
            icon: { color: "success", size: "small", name: "power-outline" }
          },
          {
            name: "Aus",
            value: "false",
            icon: { color: "danger", size: "small", name: "power-outline" }
          }], false)
        ]
      }, "ctrlIoFixDigitalOutput0");
    }

    // Relay null
    {
      const EMS = DummyConfig.from(
        CONTROLLER_IO_FIX_DIGITAL_OUTPUT("ctrlIoFixDigitalOutput0", "Terassenbeleuchtung")
      );

      const VIEW_CONTEXT: OeFormlyViewTester.Context = {
        "ctrlIoFixDigitalOutput0/_PropertyIsOn": null // false
      };

      expectView(EMS, Role.ADMIN, VIEW_CONTEXT, TEST_CONTEXT, {
        title: "Terassenbeleuchtung",
        lines: [
          ONLY_NAME_LINE("Modus"),
          BUTTONS_FROM_CHANNEL_LINE([{
            name: "An",
            value: "true",
            icon: { color: "success", size: "small", name: "power-outline" }
          },
          {
            name: "Aus",
            value: "false",
            icon: { color: "danger", size: "small", name: "power-outline" }
          }], false)
        ]
      }, "ctrlIoFixDigitalOutput0");
    }
  });
});
