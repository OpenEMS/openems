import { LINE_INFO } from "src/app/shared/edge/edgeconfig.spec";
import { OeFormlyViewTester } from "src/app/shared/genericComponents/shared/tester";
import { sharedSetup, TestContext } from "src/app/shared/test/utils.spec";

import { ModalComponent } from "./modal";

export const VIEW_CONTEXT: OeFormlyViewTester.Context = ({});

export function expectView(testContext: TestContext, viewContext: OeFormlyViewTester.Context, view: OeFormlyViewTester.View): void {

  const generatedView = OeFormlyViewTester.apply(ModalComponent.generateView(testContext.translate), viewContext);

  expect(generatedView).toEqual(view);
};

describe('SelfConsumption - Modal', () => {
  let TEST_CONTEXT:TestContext;
  beforeEach(() => TEST_CONTEXT = sharedSetup());

  it('generateView()', () => {
    {
      expectView(TEST_CONTEXT, VIEW_CONTEXT, {
        title: "Eigenverbrauch",
        lines: [
          LINE_INFO("Der Eigenverbrauch gibt an zu wie viel Prozent die aktuell erzeugte Leistung durch direkten Verbrauch und durch Speicherbeladung selbst genutzt wird.")
        ]
      });
    }
  });
});