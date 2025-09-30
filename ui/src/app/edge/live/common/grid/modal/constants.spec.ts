import { TestContext } from "src/app/shared/components/shared/testing/UTILS.SPEC";
import { EdgeConfig } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";

import { OeFormlyViewTester } from "../../../../../shared/components/shared/testing/tester";
import { ModalComponent } from "./modal";

export function expectView(config: EdgeConfig, role: Role, viewContext: OE_FORMLY_VIEW_TESTER.CONTEXT, testContext: TestContext, view: OE_FORMLY_VIEW_TESTER.VIEW): void {
  expect(OE_FORMLY_VIEW_TESTER.APPLY(MODAL_COMPONENT.GENERATE_VIEW(config, role, TEST_CONTEXT.TRANSLATE), viewContext))
    .toEqual(view);
}
