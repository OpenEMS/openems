import { EdgeConfig } from "src/app/shared/shared";
import { TestContext } from "src/app/shared/test/utils.spec";
import { Role } from "src/app/shared/type/role";
import { ModalComponent } from "./modal.component";
import { OeFormlyViewTester } from "src/app/shared/genericComponents/shared/tester";

export function expectView(config: EdgeConfig, role: Role, viewContext: OeFormlyViewTester.Context, testContext: TestContext, view: OeFormlyViewTester.View, componentId: string): void {
  expect(OeFormlyViewTester.apply(ModalComponent.generateView(config, role, testContext.translate, componentId), viewContext))
    .toEqual(view);
};