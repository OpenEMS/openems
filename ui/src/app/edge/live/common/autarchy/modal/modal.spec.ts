import { LINE_INFO } from "src/app/shared/edge/edgeconfig.spec";
import { OeFormlyViewTester } from "src/app/shared/genericComponents/shared/testing/tester";
import { sharedSetup, TestContext } from "src/app/shared/test/utils.spec";

import { ModalComponent } from "./modal";

export const VIEW_CONTEXT: OeFormlyViewTester.Context = ({});

export function expectView(testContext: TestContext, viewContext: OeFormlyViewTester.Context, view: OeFormlyViewTester.View): void {

  const generatedView = OeFormlyViewTester.apply(ModalComponent.generateView(testContext.translate), viewContext);
  expect(generatedView).toEqual(view);
}

describe('Autarchy - Modal', () => {
  let TEST_CONTEXT: TestContext;
  beforeEach(async () => TEST_CONTEXT = await sharedSetup());

  it('generateView()', () => {
    {
      expectView(TEST_CONTEXT, VIEW_CONTEXT, {
        title: "Autarkie",
        lines: [
          LINE_INFO("Die Autarkie gibt an zu wie viel Prozent die aktuell genutzte Leistung durch Erzeugung und Speicherentladung gedeckt wird."),
        ],
      });
    }
  });
});
