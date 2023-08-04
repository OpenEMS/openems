import { DummyConfig, SOCOMEC_GRID_METER } from "src/app/shared/edge/edgeconfig.spec";
import { CHANNEL_LINE, LINE_HORIZONTAL, LINE_INFO_PHASES_DE, OeFormlyViewTester, PHASE_ADMIN, PHASE_GUEST, expectView } from "src/app/shared/genericComponents/shared/tester";
import { EdgeConfig, GridMode } from "src/app/shared/shared";
import { sharedSetup } from "src/app/shared/test/utils.spec";
import { Role } from "src/app/shared/type/role";

import { TranslateService } from "@ngx-translate/core";
import { OeFormlyView } from "src/app/shared/genericComponents/shared/oe-formly-component";
import { ModalComponent } from "./modal";

const VIEW_CONTEXT = (properties?: {}): OeFormlyViewTester.Context => ({
  "_sum/GridMode": GridMode.ON_GRID,
  "_sum/GridActivePower": -1000,
  "meter0/ActivePower": -1000,
  "meter0/VoltageL1": 230000,
  "meter0/CurrentL1": 2170,
  "meter0/ActivePowerL1": -500,
  "meter0/ActivePowerL2": 1500,
  ...properties
});

function generateView(config: EdgeConfig, role: Role, translate: TranslateService, componentId: string): OeFormlyView {
  return ModalComponent.generateView(config, role, translate);
}

describe('ExampleSystemsTest', () => {
  let TEST_CONTEXT;
  beforeEach(() => TEST_CONTEXT = sharedSetup());

  it('ModalComponent.generateView() GridModal', () => {
    {
      // No Meters
      const EMS = DummyConfig.from();

      expectView(EMS, Role.ADMIN, VIEW_CONTEXT(), TEST_CONTEXT, {
        title: "Netz",
        lines: [
        ]
      }, generateView);
    }

    {
      // Single Meter
      const EMS = DummyConfig.from(
        SOCOMEC_GRID_METER("meter0", "Netzzähler")
      );

      // Admin and Installer
      expectView(EMS, Role.ADMIN, VIEW_CONTEXT(), TEST_CONTEXT, {
        title: "Netz",
        lines: [
          CHANNEL_LINE("Bezug", "0 W"),
          CHANNEL_LINE("Einspeisung", "1.000 W"),
          PHASE_ADMIN("Phase L1 Einspeisung", "230 V", "2,2 A", "500 W"),
          PHASE_ADMIN("Phase L2 Bezug", "-", "-", "1.500 W"),
          PHASE_ADMIN("Phase L3", "-", "-", "-"),
          LINE_HORIZONTAL,
          LINE_INFO_PHASES_DE
        ]
      }, generateView);

      // Owner and Guest
      expectView(EMS, Role.OWNER, VIEW_CONTEXT(), TEST_CONTEXT, {
        title: "Netz",
        lines: [
          CHANNEL_LINE("Bezug", "0 W"),
          CHANNEL_LINE("Einspeisung", "1.000 W"),
          PHASE_GUEST("Phase L1 Einspeisung", "500 W"),
          PHASE_GUEST("Phase L2 Bezug", "1.500 W"),
          PHASE_GUEST("Phase L3", "-"),
          LINE_HORIZONTAL,
          LINE_INFO_PHASES_DE
        ]
      }, generateView);

      // Offgrid
      expectView(EMS, Role.ADMIN, VIEW_CONTEXT({ '_sum/GridMode': GridMode.OFF_GRID }), TEST_CONTEXT, {
        title: "Netz",
        lines: [
          {
            type: "channel-line",
            name: "Keine Netzverbindung!",
            value: ""
          },
          CHANNEL_LINE("Bezug", "0 W"),
          CHANNEL_LINE("Einspeisung", "1.000 W"),
          PHASE_ADMIN("Phase L1 Einspeisung", "230 V", "2,2 A", "500 W"),
          PHASE_ADMIN("Phase L2 Bezug", "-", "-", "1.500 W"),
          PHASE_ADMIN("Phase L3", "-", "-", "-"),
          LINE_HORIZONTAL,
          LINE_INFO_PHASES_DE
        ]
      }, generateView);
    }

    {
      // Two Meters
      const EMS = DummyConfig.from(
        SOCOMEC_GRID_METER("meter10"),
        SOCOMEC_GRID_METER("meter11")
      );

      // Admin and Installer -> two meters
      expectView(EMS, Role.ADMIN, VIEW_CONTEXT(), TEST_CONTEXT, {
        title: "Netz",
        lines: [
          CHANNEL_LINE("Bezug", "0 W"),
          CHANNEL_LINE("Einspeisung", "1.000 W"),
          LINE_HORIZONTAL,
          CHANNEL_LINE("meter10", "-"),
          PHASE_ADMIN("Phase L1", "-", "-", "-"),
          PHASE_ADMIN("Phase L2", "-", "-", "-"),
          PHASE_ADMIN("Phase L3", "-", "-", "-"),
          LINE_HORIZONTAL,
          CHANNEL_LINE("meter11", "-"),
          PHASE_ADMIN("Phase L1", "-", "-", "-"),
          PHASE_ADMIN("Phase L2", "-", "-", "-"),
          PHASE_ADMIN("Phase L3", "-", "-", "-"),
          LINE_HORIZONTAL,
          LINE_INFO_PHASES_DE
        ]
      }, generateView);

      // Owner and Guest -> two meters
      expectView(EMS, Role.GUEST, VIEW_CONTEXT(), TEST_CONTEXT, {
        title: "Netz",
        lines: [
          CHANNEL_LINE("Bezug", "0 W"),
          CHANNEL_LINE("Einspeisung", "1.000 W"),
          LINE_HORIZONTAL,
          CHANNEL_LINE("meter10", "-"),
          PHASE_GUEST("Phase L1", "-"),
          PHASE_GUEST("Phase L2", "-"),
          PHASE_GUEST("Phase L3", "-"),
          LINE_HORIZONTAL,
          CHANNEL_LINE("meter11", "-"),
          PHASE_GUEST("Phase L1", "-"),
          PHASE_GUEST("Phase L2", "-"),
          PHASE_GUEST("Phase L3", "-"),
          LINE_HORIZONTAL,
          LINE_INFO_PHASES_DE
        ]
      }, generateView);
    }
  });
});
