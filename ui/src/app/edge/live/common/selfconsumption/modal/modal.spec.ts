import { LINE_INFO } from "src/app/shared/components/edge/edgeconfig.spec";
import { OeFormlyViewTester } from "src/app/shared/components/shared/testing/tester";
import { TestContext, TestingUtils } from "src/app/shared/components/shared/testing/utils.spec";

import { ModalComponent } from "./modal";

export const VIEW_CONTEXT: OeFormlyViewTester.Context = ({});

export function expectView(testContext: TestContext, viewContext: OeFormlyViewTester.Context, view: OeFormlyViewTester.View): void {

  const generatedView = OeFormlyViewTester.apply(ModalComponent.generateView(testContext.translate), viewContext);

  expect(generatedView).toEqual(view);
}

describe("SelfConsumption - Modal", () => {
  let TEST_CONTEXT: TestContext;
  beforeEach(async () => TEST_CONTEXT = await TestingUtils.sharedSetup());

  it("generateView()", () => {
    {
      expectView(TEST_CONTEXT, VIEW_CONTEXT, {
        title: "Eigenverbrauch",
        lines: [
          LINE_INFO("Der Eigenverbrauch gibt an zu wie viel Prozent die aktuell erzeugte Leistung durch direkten Verbrauch und durch Speicherbeladung selbst genutzt wird."),
        ],
      });
    }
  });
});
