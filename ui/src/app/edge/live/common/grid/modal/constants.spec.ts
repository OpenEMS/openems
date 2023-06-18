import { GridMode } from "src/app/shared/shared";
import { TextIndentation } from "../../../../../shared/genericComponents/modal/modal-line/modal-line";
import { OeFormlyViewTester } from "../../../../../shared/genericComponents/shared/tester";

export namespace Constants {

  export const DEFAULT_CONTEXT: OeFormlyViewTester.Context = {
    "_sum/GridMode": GridMode.ON_GRID,
    "meter0/ActivePower": -1000,
    "meter0/VoltageL1": 230000,
    "meter0/CurrentL1": 2170,
    "meter0/ActivePowerL1": -500,
    "meter0/ActivePowerL2": 1500
  };

  export const LINE = (name: string, value: string): OeFormlyViewTester.Field => ({
    type: "line",
    name: name,
    value: value
  });

  export const PHASE_ADMIN = (name: string, voltage: string, current: string, power: string): OeFormlyViewTester.Field => ({
    type: "line-with-children",
    name: name,
    indentation: TextIndentation.SINGLE,
    children: [
      {
        type: "line-item",
        value: voltage
      },
      {
        type: "line-item",
        value: current
      },
      {
        type: "line-item",
        value: power
      }
    ]
  });

  export const PHASE_GUEST = (name: string, power: string): OeFormlyViewTester.Field => ({
    type: "line-with-children",
    name: name,
    indentation: TextIndentation.SINGLE,
    children: [
      {
        type: "line-item",
        value: power
      }
    ]
  });

  export const LINE_HORIZONTAL: OeFormlyViewTester.Field = {
    type: "line-horizontal"
  };

  export const LINE_INFO_PHASES_DE: OeFormlyViewTester.Field = {
    type: "line-info",
    name: "Die Summe der einzelnen Phasen kann aus technischen Gründen geringfügig von der Gesamtsumme abweichen."
  };

  export const EMS1_ADMIN_AND_INSTALLER_SINGLE_METER: OeFormlyViewTester.ViewContext = {
    context: DEFAULT_CONTEXT,
    view: {
      title: "Netz",
      lines: [
        LINE("Bezug", "0 W"),
        LINE("Einspeisung", "1.000 W"),
        PHASE_ADMIN("Phase L1 Einspeisung", "230 V", "2,2 A", "500 W"),
        PHASE_ADMIN("Phase L2 Bezug", "-", "-", "1.500 W"),
        PHASE_ADMIN("Phase L3", "-", "-", "0 W"),
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
        LINE("Bezug", "0 W"),
        LINE("Einspeisung", "0 W"),
        LINE_HORIZONTAL,
        LINE("meter10", "0 W"),
        PHASE_ADMIN("Phase L1", "-", "-", "0 W"),
        PHASE_ADMIN("Phase L2", "-", "-", "0 W"),
        PHASE_ADMIN("Phase L3", "-", "-", "0 W"),
        LINE_HORIZONTAL,
        LINE("meter11", "0 W"),
        PHASE_ADMIN("Phase L1", "-", "-", "0 W"),
        PHASE_ADMIN("Phase L2", "-", "-", "0 W"),
        PHASE_ADMIN("Phase L3", "-", "-", "0 W"),
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
        LINE("Bezug", "0 W"),
        LINE("Einspeisung", "1.000 W"),
        PHASE_GUEST("Phase L1 Einspeisung", "500 W"),
        PHASE_GUEST("Phase L2 Bezug", "1.500 W"),
        PHASE_GUEST("Phase L3", "0 W"),
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
        LINE("Bezug", "0 W"),
        LINE("Einspeisung", "0 W"),
        LINE_HORIZONTAL,
        LINE("meter10", "0 W"),
        PHASE_GUEST("Phase L1", "0 W"),
        PHASE_GUEST("Phase L2", "0 W"),
        PHASE_GUEST("Phase L3", "0 W"),
        LINE_HORIZONTAL,
        LINE("meter11", "0 W"),
        PHASE_GUEST("Phase L1", "0 W"),
        PHASE_GUEST("Phase L2", "0 W"),
        PHASE_GUEST("Phase L3", "0 W"),
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
          type: "line",
          name: "Keine Netzverbindung!",
          value: ""
        },
        LINE("Bezug", "0 W"),
        LINE("Einspeisung", "1.000 W"),
        PHASE_ADMIN("Phase L1 Einspeisung", "230 V", "2,2 A", "500 W"),
        PHASE_ADMIN("Phase L2 Bezug", "-", "-", "1.500 W"),
        PHASE_ADMIN("Phase L3", "-", "-", "0 W"),
        LINE_HORIZONTAL,
        LINE_INFO_PHASES_DE
      ]
    }
  };
}