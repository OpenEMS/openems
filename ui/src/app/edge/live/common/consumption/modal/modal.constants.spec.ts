import { DummyConfig } from "src/app/shared/edge/edgeconfig.spec";
import { EdgeConfig } from "src/app/shared/shared";
import { TestContext } from "src/app/shared/test/utils.spec";

import { OeFormlyViewTester } from "../../../../../shared/genericComponents/shared/testing/tester";
import { ModalComponent } from "./modal";

export function expectView(config: EdgeConfig, viewContext: OeFormlyViewTester.Context, testContext: TestContext, view: OeFormlyViewTester.View): void {

  const generatedView = OeFormlyViewTester.apply(ModalComponent.generateView(DummyConfig.convertDummyEdgeConfigToRealEdgeConfig(config), testContext.translate), viewContext);

  expect(generatedView).toEqual(view);
};
