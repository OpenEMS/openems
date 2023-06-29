import { CHANNEL_LINE, DummyConfig, GOODWE_CHARGER_PV1, LINE_HORIZONTAL, LINE_INFO_PHASES_DE, PHASE_ADMIN, PHASE_GUEST, PV_INVERTER_KACO } from "src/app/shared/edge/edgeconfig.spec";
import { TextIndentation } from "src/app/shared/genericComponents/modal/modal-line/modal-line";
import { OeFormlyViewTester } from "src/app/shared/genericComponents/shared/tester";
import { sharedSetup } from "src/app/shared/test/utils.spec";
import { Role } from "src/app/shared/type/role";

import { expectView } from "./modal.constants.spec";

describe('Consumption - Modal', () => {
  let TEST_CONTEXT;
  beforeEach(() => TEST_CONTEXT = sharedSetup());

  it('generateView()', () => {

    // No productionMeters and charger
    {
      const VIEW_CONTEXT: OeFormlyViewTester.Context = {
        '_sum/ProductionAcActivePower': -1000,
        '_sum/ProductionAcActivePowerL1': -1000,
        '_sum/ProductionAcActivePowerL2': 1000,
        '_sum/ProductionAcActivePowerL3': null
      };
      const EMS = DummyConfig.from();

      expectView(EMS, Role.ADMIN, VIEW_CONTEXT, TEST_CONTEXT, {
        title: "Erzeugung",
        lines: []
      });
    }

    // one charger and one productionMeter
    {
      const EMS = DummyConfig.from(
        GOODWE_CHARGER_PV1("charger0", "Charger"),
        PV_INVERTER_KACO("pvInverter0", "Pv-Inverter")
      );
      const VIEW_CONTEXT: OeFormlyViewTester.Context = {
        '_sum/ProductionActivePower': 1000,
        '_sum/ProductionAcActivePower': 1000,
        '_sum/ProductionAcActivePowerL1': 300,
        '_sum/ProductionAcActivePowerL2': 350,
        '_sum/ProductionAcActivePowerL3': 350,
        'charger0/ActualPower': 1000,
        'charger0/Current': 1000,
        'charger0/Voltage': -1000,
        'pvInverter0/ActivePower': 1000,
        'pvInverter0/ActivePowerL1': 1000,
        'pvInverter0/ActivePowerL2': 0,
        'pvInverter0/ActivePowerL3': null,
        'pvInverter0/CurrentL1': 8000,
        'pvInverter0/CurrentL2': 0,
        'pvInverter0/CurrentL3': null,
        'pvInverter0/VoltageL1': 230000,
        'pvInverter0/VoltageL2': 0,
        'pvInverter0/VoltageL3': null
      };

      expectView(EMS, Role.ADMIN, VIEW_CONTEXT, TEST_CONTEXT, {
        title: "Erzeugung",
        lines: [
          CHANNEL_LINE("Gesamt", "1.000 W"),
          LINE_HORIZONTAL,
          CHANNEL_LINE("Pv-Inverter", "1.000 W"),
          PHASE_ADMIN("Phase L1", "230 V", "8,0 A", "1.000 W"),
          PHASE_ADMIN("Phase L2", "0 V", "0,0 A", "0 W"),
          PHASE_ADMIN("Phase L3", "-", "-", "-"),
          LINE_HORIZONTAL,
          PHASE_ADMIN("Charger", "-1 V", "1,0 A", "1.000 W", TextIndentation.NONE),
          LINE_HORIZONTAL,
          LINE_INFO_PHASES_DE
        ]
      });
    }

    //one charger
    {
      const EMS = DummyConfig.from(
        GOODWE_CHARGER_PV1("charger0", "Charger")
      );
      const VIEW_CONTEXT: OeFormlyViewTester.Context = {
        '_sum/ProductionActivePower': 1000,
        '_sum/ProductionAcActivePower': 1000,
        '_sum/ProductionAcActivePowerL1': 300,
        '_sum/ProductionAcActivePowerL2': 350,
        '_sum/ProductionAcActivePowerL3': 350,
        'charger0/ActualPower': 1000,
        'charger0/Current': 1000,
        'charger0/Voltage': 1000
      };

      expectView(EMS, Role.ADMIN, VIEW_CONTEXT, TEST_CONTEXT, {
        title: "Erzeugung",
        lines: [
          PHASE_ADMIN("Charger", "1 V", "1,0 A", "1.000 W", TextIndentation.NONE),
          LINE_HORIZONTAL,
          LINE_INFO_PHASES_DE
        ]
      });
    }

    // two charger
    {
      const EMS = DummyConfig.from(
        GOODWE_CHARGER_PV1("charger0", "Charger 1"),
        GOODWE_CHARGER_PV1("charger1", "Charger 2")
      );
      const VIEW_CONTEXT: OeFormlyViewTester.Context = {
        '_sum/ProductionDcActualPower': 1000,
        'charger0/ActualPower': 1000,
        'charger0/Current': 1000,
        'charger0/Voltage': 1000
      };

      expectView(EMS, Role.ADMIN, VIEW_CONTEXT, TEST_CONTEXT, {
        title: "Erzeugung",
        lines: [
          CHANNEL_LINE("Gesamt", "1.000 W"),
          LINE_HORIZONTAL,
          PHASE_ADMIN("Charger 1", "1 V", "1,0 A", "1.000 W"),
          LINE_HORIZONTAL,
          PHASE_ADMIN("Charger 2", "-", "-", "-"),
          LINE_HORIZONTAL,
          LINE_INFO_PHASES_DE
        ]
      });
    }

    // Two productionMeter, no charger
    {
      const EMS = DummyConfig.from(
        PV_INVERTER_KACO("pvInverter0"),
        PV_INVERTER_KACO("pvInverter1")
      );
      const VIEW_CONTEXT: OeFormlyViewTester.Context = {
        '_sum/ProductionActivePower': 1000,
        '_sum/ProductionAcActivePower': 1000,
        '_sum/ProductionAcActivePowerL1': 300,
        '_sum/ProductionAcActivePowerL2': 350,
        '_sum/ProductionAcActivePowerL3': 350,
        'pvInverter0/ActivePower': 1000,
        'pvInverter0/ActivePowerL1': 1000,
        'pvInverter0/ActivePowerL2': 0,
        'pvInverter0/ActivePowerL3': null,
        'pvInverter1/ActivePower': -1000,
        'pvInverter1/ActivePowerL1': 1000,
        'pvInverter1/ActivePowerL2': 0,
        'pvInverter1/ActivePowerL3': -2000
      };

      expectView(EMS, Role.GUEST, VIEW_CONTEXT, TEST_CONTEXT, {
        title: "Erzeugung",
        lines: [
          CHANNEL_LINE("Gesamt", "1.000 W"),
          CHANNEL_LINE("Phase L1", "300 W", TextIndentation.SINGLE),
          CHANNEL_LINE("Phase L2", "350 W", TextIndentation.SINGLE),
          CHANNEL_LINE("Phase L3", "350 W", TextIndentation.SINGLE),
          LINE_HORIZONTAL,
          CHANNEL_LINE("pvInverter0", "1.000 W"),
          PHASE_GUEST("Phase L1", "1.000 W"),
          PHASE_GUEST("Phase L2", "0 W"),
          PHASE_GUEST("Phase L3", "-"),
          LINE_HORIZONTAL,
          CHANNEL_LINE("pvInverter1", "0 W"),
          PHASE_GUEST("Phase L1", "1.000 W"),
          PHASE_GUEST("Phase L2", "0 W"),
          PHASE_GUEST("Phase L3", "0 W"),
          LINE_HORIZONTAL,
          LINE_INFO_PHASES_DE
        ]
      });
    }

    {
      const EMS = DummyConfig.from(
        PV_INVERTER_KACO("pvInverter0"),
        GOODWE_CHARGER_PV1("charger0"),
        GOODWE_CHARGER_PV1("charger1")
      );
      const VIEW_CONTEXT: OeFormlyViewTester.Context = {
        '_sum/ProductionActivePower': 1000,
        '_sum/ProductionAcActivePower': 1000,
        '_sum/ProductionAcActivePowerL1': 300,
        '_sum/ProductionAcActivePowerL2': 350,
        '_sum/ProductionAcActivePowerL3': 350,
        '_sum/ProductionDcActualPower': 1000,
        'pvInverter0/ActivePower': 1000,
        'pvInverter0/ActivePowerL1': 1000,
        'pvInverter0/ActivePowerL2': 0,
        'pvInverter0/ActivePowerL3': null,
        'charger0/ActualPower': 1000,
        'charger0/Current': 8000,
        'charger0/Voltage': 230000,
        'charger1/ActualPower': null,
        'charger1/Current': null,
        'charger1/Voltage': null
      };

      expectView(EMS, Role.GUEST, VIEW_CONTEXT, TEST_CONTEXT, {
        title: "Erzeugung",
        lines: [
          CHANNEL_LINE("Gesamt", "1.000 W"),
          LINE_HORIZONTAL,
          CHANNEL_LINE("pvInverter0", "1.000 W"),
          PHASE_GUEST("Phase L1", "1.000 W"),
          PHASE_GUEST("Phase L2", "0 W"),
          PHASE_GUEST("Phase L3", "-"),
          LINE_HORIZONTAL,
          CHANNEL_LINE('Gesamt DC', "1.000 W"),
          LINE_HORIZONTAL,
          PHASE_GUEST("charger0", "1.000 W", TextIndentation.NONE),
          LINE_HORIZONTAL,
          PHASE_GUEST("charger1", "-", TextIndentation.NONE),
          LINE_HORIZONTAL,
          LINE_INFO_PHASES_DE
        ]
      });
    }
  });
});
