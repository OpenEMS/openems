import { CHANNEL_LINE, DummyConfig, KEBA_KECONTACT_EVCS, LINE_HORIZONTAL, LINE_INFO_PHASES_DE, SOCOMEC_CONSUMPTION_METER, VALUE_FROM_CHANNELS_LINE } from "src/app/shared/edge/edgeconfig.spec";
import { TextIndentation } from "src/app/shared/genericComponents/modal/modal-line/modal-line";
import { OeFormlyViewTester } from "src/app/shared/genericComponents/shared/tester";
import { sharedSetup } from "src/app/shared/test/utils.spec";
import { Role } from "src/app/shared/type/role";

import { expectView } from "./modal.constants.spec";

describe('Consumption - Modal', () => {
  let TEST_CONTEXT;
  beforeEach(() => TEST_CONTEXT = sharedSetup());

  it('generateView()', () => {

    // No evcs and consumptionMeters and negative ConsumptionActivePower
    {
      const VIEW_CONTEXT: OeFormlyViewTester.Context = {
        '_sum/ConsumptionActivePower': -1000,
        '_sum/ConsumptionActivePowerL1': -1000,
        '_sum/ConsumptionActivePowerL2': 1000,
        '_sum/ConsumptionActivePowerL3': -1000,
      }
      const EMS = DummyConfig.from();

      // TODO remove Role
      expectView(EMS, Role.ADMIN, VIEW_CONTEXT, TEST_CONTEXT, {
        title: "Verbrauch",
        lines: [
          CHANNEL_LINE("Gesamt", "0 W"),
          CHANNEL_LINE("Phase L1", "0 W", TextIndentation.SINGLE),
          CHANNEL_LINE("Phase L2", "1.000 W", TextIndentation.SINGLE),
          CHANNEL_LINE("Phase L3", "0 W", TextIndentation.SINGLE),
          VALUE_FROM_CHANNELS_LINE("Sonstiger", "0 W"),
          LINE_INFO_PHASES_DE
        ]
      });
    }

    // two evcs and one consumptionMeter, negative ConsumptionActivePower
    {
      const EMS = DummyConfig.from(
        SOCOMEC_CONSUMPTION_METER("meter0", "Waermepumpe"),
        KEBA_KECONTACT_EVCS("evcs0", "Evcs"),
        KEBA_KECONTACT_EVCS("evcs1", "Evcs 2"),
        KEBA_KECONTACT_EVCS("evcs2", "Evcs 3")
      );
      const VIEW_CONTEXT: OeFormlyViewTester.Context = {
        '_sum/ConsumptionActivePower': 1000,
        '_sum/ConsumptionActivePowerL1': 300,
        '_sum/ConsumptionActivePowerL2': 350,
        '_sum/ConsumptionActivePowerL3': 350,
        'meter0/ActivePower': 1000,
        'meter0/ActivePowerL1': 1000,
        'meter0/ActivePowerL2': -1000,
        'meter0/ActivePowerL3': 1000,
        'evcs0/ChargePower': 1000,
        'evcs1/ChargePower': 0,
        'evcs2/ChargePower': null,
      }

      // Admin and Installer
      expectView(EMS, Role.ADMIN, VIEW_CONTEXT, TEST_CONTEXT, {
        title: "Verbrauch",
        lines: [
          CHANNEL_LINE("Gesamt", "1.000 W"),
          CHANNEL_LINE("Phase L1", "300 W", TextIndentation.SINGLE),
          CHANNEL_LINE("Phase L2", "350 W", TextIndentation.SINGLE),
          CHANNEL_LINE("Phase L3", "350 W", TextIndentation.SINGLE),
          LINE_HORIZONTAL,
          CHANNEL_LINE("Evcs", "1.000 W"),
          LINE_HORIZONTAL,
          CHANNEL_LINE("Evcs 2", "0 W"),
          LINE_HORIZONTAL,
          CHANNEL_LINE("Evcs 3", "-"),
          LINE_HORIZONTAL,
          CHANNEL_LINE("Waermepumpe", "1.000 W"),
          CHANNEL_LINE("Phase L1", "1.000 W", TextIndentation.SINGLE),
          CHANNEL_LINE("Phase L2", "0 W", TextIndentation.SINGLE),
          CHANNEL_LINE("Phase L3", "1.000 W", TextIndentation.SINGLE),
          LINE_HORIZONTAL,
          VALUE_FROM_CHANNELS_LINE("Sonstiger", "0 W"),
          LINE_INFO_PHASES_DE
        ]
      });
    }
  });
});
