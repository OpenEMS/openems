import { TestContext } from "src/app/shared/components/shared/testing/UTILS.SPEC";
import { Edge, EdgeConfig } from "src/app/shared/shared";

import { OeFormlyViewTester } from "../../../../../../shared/components/shared/testing/tester";
import { ModalComponent } from "./modal";

export function expectView(config: EdgeConfig, edge: Edge, viewContext: OE_FORMLY_VIEW_TESTER.CONTEXT, testContext: TestContext, view: OE_FORMLY_VIEW_TESTER.VIEW): void {
    expect(OE_FORMLY_VIEW_TESTER.APPLY(MODAL_COMPONENT.GENERATE_VIEW(TEST_CONTEXT.TRANSLATE, OBJECT.VALUES(CONFIG.COMPONENTS)[0], edge), viewContext))
        .toEqual(view);
}
