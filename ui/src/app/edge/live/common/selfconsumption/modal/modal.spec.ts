import { LINE_INFO } from "src/app/shared/components/edge/EDGECONFIG.SPEC";
import { OeFormlyViewTester } from "src/app/shared/components/shared/testing/tester";
import { TestContext, TestingUtils } from "src/app/shared/components/shared/testing/UTILS.SPEC";

import { ModalComponent } from "./modal";

export const VIEW_CONTEXT: OE_FORMLY_VIEW_TESTER.CONTEXT = ({});

export function expectView(testContext: TestContext, viewContext: OE_FORMLY_VIEW_TESTER.CONTEXT, view: OE_FORMLY_VIEW_TESTER.VIEW): void {

  const generatedView = OE_FORMLY_VIEW_TESTER.APPLY(MODAL_COMPONENT.GENERATE_VIEW(TEST_CONTEXT.TRANSLATE), viewContext);

  expect(generatedView).toEqual(view);
}

describe("SelfConsumption - Modal", () => {
  let TEST_CONTEXT: TestContext;
  beforeEach(async () => TEST_CONTEXT = await TESTING_UTILS.SHARED_SETUP());

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
