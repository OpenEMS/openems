import { TestContext } from "src/app/shared/components/shared/testing/utils.spec";
import { Edge, EdgeConfig } from "src/app/shared/shared";

import { OeFormlyViewTester } from "../../../../../../shared/components/shared/testing/tester";
import { ModalComponent } from "./modal";

export function expectView(config: EdgeConfig, edge: Edge, viewContext: OeFormlyViewTester.Context, testContext: TestContext, view: OeFormlyViewTester.View): void {
    expect(OeFormlyViewTester.apply(ModalComponent.generateView(testContext.translate, Object.values(config.components)[0], edge), viewContext))
        .toEqual(view);
}
