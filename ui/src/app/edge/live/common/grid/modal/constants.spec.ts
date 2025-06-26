import { TestContext } from "src/app/shared/components/shared/testing/utils.spec";
import { EdgeConfig } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";

import { OeFormlyViewTester } from "../../../../../shared/components/shared/testing/tester";
import { ModalComponent } from "./modal";

export function expectView(config: EdgeConfig, role: Role, viewContext: OeFormlyViewTester.Context, testContext: TestContext, view: OeFormlyViewTester.View): void {
  expect(OeFormlyViewTester.apply(ModalComponent.generateView(config, role, testContext.translate), viewContext))
    .toEqual(view);
}
