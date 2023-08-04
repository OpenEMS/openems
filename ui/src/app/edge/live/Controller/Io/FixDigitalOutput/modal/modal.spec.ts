import { TranslateService } from "@ngx-translate/core";
import { CONTROLLER_IO_FIX_DIGITAL_OUTPUT, DummyConfig } from "src/app/shared/edge/edgeconfig.spec";
import { OeFormlyView } from "src/app/shared/genericComponents/shared/oe-formly-component";
import { BUTTONS_FROM_CHANNEL_LINE, NAME_LINE, OeFormlyViewTester, expectView } from "src/app/shared/genericComponents/shared/tester";
import { EdgeConfig } from "src/app/shared/shared";
import { sharedSetup } from "src/app/shared/test/utils.spec";
import { Role } from "src/app/shared/type/role";
import { ModalComponent } from "./modal.component";

const VIEW_CONTEXT = (isOn: boolean): OeFormlyViewTester.Context => ({
  "ctrlIoFixDigitalOutput0/_PropertyIsOn": isOn ? 1 : 0 // false
});

function generateView(config: EdgeConfig, role: Role, translate: TranslateService, componentId: string): OeFormlyView {
  return ModalComponent.generateView(config, role, translate, componentId);
}

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
          NAME_LINE("Modus"),
          BUTTONS_FROM_CHANNEL_LINE([{
            name: "An",
            value: "true",
            icon: { color: "success", name: "power-outline" }
          },
          {
            name: "Aus",
            value: "false",
            icon: { color: "danger", name: "power-outline" }
          }], "true")
        ]
      }, generateView, "ctrlIoFixDigitalOutput0");
    }

    // Relay aus
    {
      const EMS = DummyConfig.from(
        CONTROLLER_IO_FIX_DIGITAL_OUTPUT("ctrlIoFixDigitalOutput0", "Terassenbeleuchtung")
      );
      expectView(EMS, Role.ADMIN, VIEW_CONTEXT(false), TEST_CONTEXT, {
        title: "Terassenbeleuchtung",
        lines: [
          NAME_LINE("Modus"),
          BUTTONS_FROM_CHANNEL_LINE([{
            name: "An",
            value: "true",
            icon: { color: "success", name: "power-outline" }
          },
          {
            name: "Aus",
            value: "false",
            icon: { color: "danger", name: "power-outline" }
          }], "false")
        ]
      }, generateView, "ctrlIoFixDigitalOutput0");
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
          NAME_LINE("Modus"),
          BUTTONS_FROM_CHANNEL_LINE([{
            name: "An",
            value: "true",
            icon: { color: "success", name: "power-outline" }
          },
          {
            name: "Aus",
            value: "false",
            icon: { color: "danger", name: "power-outline" }
          }], "-")
        ]
      }, generateView, "ctrlIoFixDigitalOutput0");
    }
  });
});
