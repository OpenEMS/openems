// @ts-strict-ignore
import { CHANNEL_LINE, DummyConfig, LINE_HORIZONTAL, LINE_INFO_PHASES_DE, VALUE_FROM_CHANNELS_LINE } from "src/app/shared/components/edge/EDGECONFIG.SPEC";
import { TextIndentation } from "src/app/shared/components/modal/modal-line/modal-line";
import { OeFormlyViewTester } from "src/app/shared/components/shared/testing/tester";

import { TestingUtils } from "src/app/shared/components/shared/testing/UTILS.SPEC";
import { expectView } from "./MODAL.CONSTANTS.SPEC";

describe("Consumption - Modal", () => {
  let TEST_CONTEXT;
  beforeEach(async () => TEST_CONTEXT = await TESTING_UTILS.SHARED_SETUP());

  it("generateView()", () => {

    // No evcs and consumptionMeters and negative ConsumptionActivePower
    {
      const VIEW_CONTEXT: OE_FORMLY_VIEW_TESTER.CONTEXT = {
        "_sum/ConsumptionActivePower": -1000,
        "_sum/ConsumptionActivePowerL1": -1000,
        "_sum/ConsumptionActivePowerL2": 1000,
        "_sum/ConsumptionActivePowerL3": -1000,
      };
      const EMS = DUMMY_CONFIG.FROM();

      expectView(EMS, VIEW_CONTEXT, TEST_CONTEXT, {
        title: "Verbrauch",
        lines: [
          CHANNEL_LINE("Gesamt", "0 W"),
          CHANNEL_LINE("Phase L1", "0 W", TEXT_INDENTATION.SINGLE),
          CHANNEL_LINE("Phase L2", "1.000 W", TEXT_INDENTATION.SINGLE),
          CHANNEL_LINE("Phase L3", "0 W", TEXT_INDENTATION.SINGLE),
          LINE_HORIZONTAL,
          VALUE_FROM_CHANNELS_LINE("Sonstiger", "0 W"),
          LINE_INFO_PHASES_DE,
        ],
      });
    }

    // two evcs and two consumptionMeter, negative consumptionMeter phase
    {
      const EMS = DUMMY_CONFIG.FROM(
        DUMMY_CONFIG.COMPONENT.SOCOMEC_CONSUMPTION_METER("meter0", "Waermepumpe"),
        DUMMY_CONFIG.COMPONENT.SOCOMEC_CONSUMPTION_METER("meter1", "Heizung"),
        DUMMY_CONFIG.COMPONENT.EVCS_KEBA_KECONTACT("evcs0", "Evcs"),
        DUMMY_CONFIG.COMPONENT.EVCS_KEBA_KECONTACT("evcs1", "Evcs 2"),
        DUMMY_CONFIG.COMPONENT.EVCS_KEBA_KECONTACT("evcs2", "Evcs 3"),
      );
      const VIEW_CONTEXT: OE_FORMLY_VIEW_TESTER.CONTEXT = {
        "_sum/ConsumptionActivePower": 1000,
        "_sum/ConsumptionActivePowerL1": 300,
        "_sum/ConsumptionActivePowerL2": 350,
        "_sum/ConsumptionActivePowerL3": 350,
        "meter0/ActivePower": 1000,
        "meter0/ActivePowerL1": 1000,
        "meter0/ActivePowerL2": -1000,
        "meter0/ActivePowerL3": 1000,
        "meter1/ActivePower": null,
        "meter1/ActivePowerL1": null,
        "meter1/ActivePowerL2": null,
        "meter1/ActivePowerL3": null,
        "evcs0/ChargePower": 1000,
        "evcs1/ChargePower": -1000,
        "evcs2/ChargePower": null,
      };

      expectView(EMS, VIEW_CONTEXT, TEST_CONTEXT, {
        title: "Verbrauch",
        lines: [
          CHANNEL_LINE("Gesamt", "1.000 W"),
          CHANNEL_LINE("Phase L1", "300 W", TEXT_INDENTATION.SINGLE),
          CHANNEL_LINE("Phase L2", "350 W", TEXT_INDENTATION.SINGLE),
          CHANNEL_LINE("Phase L3", "350 W", TEXT_INDENTATION.SINGLE),
          LINE_HORIZONTAL,
          CHANNEL_LINE("Evcs", "1.000 W"),
          LINE_HORIZONTAL,
          CHANNEL_LINE("Evcs 2", "0 W"),
          LINE_HORIZONTAL,
          CHANNEL_LINE("Evcs 3", "-"),
          LINE_HORIZONTAL,
          CHANNEL_LINE("Waermepumpe", "1.000 W"),
          CHANNEL_LINE("Phase L1", "1.000 W", TEXT_INDENTATION.SINGLE),
          CHANNEL_LINE("Phase L2", "0 W", TEXT_INDENTATION.SINGLE),
          CHANNEL_LINE("Phase L3", "1.000 W", TEXT_INDENTATION.SINGLE),
          LINE_HORIZONTAL,
          CHANNEL_LINE("Heizung", "-"),
          CHANNEL_LINE("Phase L1", "-", TEXT_INDENTATION.SINGLE),
          CHANNEL_LINE("Phase L2", "-", TEXT_INDENTATION.SINGLE),
          CHANNEL_LINE("Phase L3", "-", TEXT_INDENTATION.SINGLE),
          LINE_HORIZONTAL,
          VALUE_FROM_CHANNELS_LINE("Sonstiger", "0 W"),
          LINE_INFO_PHASES_DE,
        ],
      });
    }

    // No consumptionMeter, one evcs
    {
      const EMS = DUMMY_CONFIG.FROM(
        DUMMY_CONFIG.COMPONENT.EVCS_KEBA_KECONTACT("evcs0", "Evcs"),
      );
      const VIEW_CONTEXT: OE_FORMLY_VIEW_TESTER.CONTEXT = {
        "_sum/ConsumptionActivePower": 1000,
        "_sum/ConsumptionActivePowerL1": 300,
        "_sum/ConsumptionActivePowerL2": 350,
        "_sum/ConsumptionActivePowerL3": 350,
        "evcs0/ChargePower": 1000,
      };

      expectView(EMS, VIEW_CONTEXT, TEST_CONTEXT, {
        title: "Verbrauch",
        lines: [
          CHANNEL_LINE("Gesamt", "1.000 W"),
          CHANNEL_LINE("Phase L1", "300 W", TEXT_INDENTATION.SINGLE),
          CHANNEL_LINE("Phase L2", "350 W", TEXT_INDENTATION.SINGLE),
          CHANNEL_LINE("Phase L3", "350 W", TEXT_INDENTATION.SINGLE),
          LINE_HORIZONTAL,
          CHANNEL_LINE("Evcs", "1.000 W"),
          LINE_HORIZONTAL,
          VALUE_FROM_CHANNELS_LINE("Sonstiger", "0 W"),
          LINE_INFO_PHASES_DE,
        ],
      });
    }

    // One consumptionMeter, no evcs
    {
      const EMS = DUMMY_CONFIG.FROM(
        DUMMY_CONFIG.COMPONENT.SOCOMEC_CONSUMPTION_METER("meter0", "Waermepumpe"),
      );
      const VIEW_CONTEXT: OE_FORMLY_VIEW_TESTER.CONTEXT = {
        "_sum/ConsumptionActivePower": 1000,
        "_sum/ConsumptionActivePowerL1": 300,
        "_sum/ConsumptionActivePowerL2": 350,
        "_sum/ConsumptionActivePowerL3": 350,
        "meter0/ActivePower": 1000,
        "meter0/ActivePowerL1": 1000,
        "meter0/ActivePowerL2": -1000,
        "meter0/ActivePowerL3": 1000,
      };

      expectView(EMS, VIEW_CONTEXT, TEST_CONTEXT, {
        title: "Verbrauch",
        lines: [
          CHANNEL_LINE("Gesamt", "1.000 W"),
          CHANNEL_LINE("Phase L1", "300 W", TEXT_INDENTATION.SINGLE),
          CHANNEL_LINE("Phase L2", "350 W", TEXT_INDENTATION.SINGLE),
          CHANNEL_LINE("Phase L3", "350 W", TEXT_INDENTATION.SINGLE),
          LINE_HORIZONTAL,
          CHANNEL_LINE("Waermepumpe", "1.000 W"),
          CHANNEL_LINE("Phase L1", "1.000 W", TEXT_INDENTATION.SINGLE),
          CHANNEL_LINE("Phase L2", "0 W", TEXT_INDENTATION.SINGLE),
          CHANNEL_LINE("Phase L3", "1.000 W", TEXT_INDENTATION.SINGLE),
          LINE_HORIZONTAL,
          VALUE_FROM_CHANNELS_LINE("Sonstiger", "0 W"),
          LINE_INFO_PHASES_DE,
        ],
      });
    }
  });
});
