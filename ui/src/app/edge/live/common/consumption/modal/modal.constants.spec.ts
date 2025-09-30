import { DummyConfig } from "src/app/shared/components/edge/EDGECONFIG.SPEC";
import { TestContext } from "src/app/shared/components/shared/testing/UTILS.SPEC";
import { EdgeConfig } from "src/app/shared/shared";

import { OeFormlyViewTester } from "../../../../../shared/components/shared/testing/tester";
import { ModalComponent } from "./modal";

export function expectView(config: EdgeConfig, viewContext: OE_FORMLY_VIEW_TESTER.CONTEXT, testContext: TestContext, view: OE_FORMLY_VIEW_TESTER.VIEW): void {

  const generatedView = OE_FORMLY_VIEW_TESTER.APPLY(MODAL_COMPONENT.GENERATE_VIEW(DUMMY_CONFIG.CONVERT_DUMMY_EDGE_CONFIG_TO_REAL_EDGE_CONFIG(config), TEST_CONTEXT.TRANSLATE), viewContext);

  expect(generatedView).toEqual(view);
}
