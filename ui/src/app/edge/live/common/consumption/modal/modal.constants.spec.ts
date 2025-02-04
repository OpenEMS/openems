import { DummyConfig } from "src/app/shared/components/edge/edgeconfig.spec";
import { TestContext } from "src/app/shared/components/shared/testing/utils.spec";
import { EdgeConfig } from "src/app/shared/shared";

import { OeFormlyViewTester } from "../../../../../shared/components/shared/testing/tester";
import { ModalComponent } from "./modal";

export function expectView(config: EdgeConfig, viewContext: OeFormlyViewTester.Context, testContext: TestContext, view: OeFormlyViewTester.View): void {

  const generatedView = OeFormlyViewTester.apply(ModalComponent.generateView(DummyConfig.convertDummyEdgeConfigToRealEdgeConfig(config), testContext.translate), viewContext);

  expect(generatedView).toEqual(view);
}
