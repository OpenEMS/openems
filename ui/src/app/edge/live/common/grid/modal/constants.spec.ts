import { EdgeConfig } from "src/app/shared/shared";
import { TestContext } from "src/app/shared/test/utils.spec";
import { Role } from "src/app/shared/type/role";
import { TextIndentation } from "../../../../../shared/genericComponents/modal/modal-line/modal-line";
import { OeFormlyViewTester } from "../../../../../shared/genericComponents/shared/tester";
import { ModalComponent } from "./modal";

export function expectView(config: EdgeConfig, role: Role, viewContext: OeFormlyViewTester.Context, testContext: TestContext, view: OeFormlyViewTester.View): void {
  expect(OeFormlyViewTester.apply(ModalComponent.generateView(config, role, testContext.translate), viewContext))
    .toEqual(view);
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