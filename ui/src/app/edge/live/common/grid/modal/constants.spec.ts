import { GridMode } from "src/app/shared/shared";
import { TextIndentation } from "../../../../../shared/genericComponents/modal/modal-line/modal-line";
import { OeFormlyViewTester } from "../../../../../shared/genericComponents/shared/tester";

export namespace Constants {

  export const DEFAULT_CONTEXT: OeFormlyViewTester.Context = {
    "_sum/GridMode": GridMode.ON_GRID,
    "_sum/GridActivePower": -1000,
    "meter0/ActivePower": -1000,
    "meter0/VoltageL1": 230000,
    "meter0/CurrentL1": 2170,
    "meter0/ActivePowerL1": -500,
    "meter0/ActivePowerL2": 1500
  };

  export const CHANNEL_LINE = (name: string, value: string): OeFormlyViewTester.Field => ({
    type: "channel-line",
    name: name,
    value: value
  });

  export const PHASE_ADMIN = (name: string, voltage: string, current: string, power: string): OeFormlyViewTester.Field => ({
    type: "children-line",
    name: name,
    indentation: TextIndentation.SINGLE,
    children: [
      {
        type: "item",
        value: voltage
      },
      {
        type: "item",
        value: current
      },
      {
        type: "item",
        value: power
      }
    ]
  });

  export const PHASE_GUEST = (name: string, power: string): OeFormlyViewTester.Field => ({
    type: "children-line",
    name: name,
    indentation: TextIndentation.SINGLE,
    children: [
      {
        type: "item",
        value: power
      }
    ]
  });

  export const LINE_HORIZONTAL: OeFormlyViewTester.Field = {
    type: "horizontal-line"
  };

  export const LINE_INFO_PHASES_DE: OeFormlyViewTester.Field = {
    type: "info-line",
    name: "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen."
  };

  export const EMPTY_EMS: OeFormlyViewTester.ViewContext = {
    context: DEFAULT_CONTEXT,
    view: {
      title: "Netz",
      lines: [
      ]
    }
  };

  export const EMS1_ADMIN_AND_INSTALLER_SINGLE_METER: OeFormlyViewTester.ViewContext = {
    context: DEFAULT_CONTEXT,
    view: {
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
    }
  };

  export const EMS30093_ADMIN_AND_INSTALLER_TWO_METERS: OeFormlyViewTester.ViewContext = {
    context: DEFAULT_CONTEXT,
    view: {
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
    }
  };

  export const EMS1_OWNER_AND_GUEST_SINGLE_METER: OeFormlyViewTester.ViewContext = {
    context: DEFAULT_CONTEXT,
    view: {
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
    }
  };

  export const EMS30093_OWNER_AND_GUEST_TWO_METERS: OeFormlyViewTester.ViewContext = {
    context: DEFAULT_CONTEXT,
    view: {
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
    }
  };

  export const EMS1_OFF_GRID: OeFormlyViewTester.ViewContext = {
    context: Object.assign({}, DEFAULT_CONTEXT, { '_sum/GridMode': GridMode.OFF_GRID }),
    view: {
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
    }
  };
}