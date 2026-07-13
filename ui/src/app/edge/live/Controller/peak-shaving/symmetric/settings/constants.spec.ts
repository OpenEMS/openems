import { TestContext } from "src/app/shared/components/shared/testing/utils.spec";
import { Edge, EdgeConfig } from "src/app/shared/shared";

import { OeFormlyViewTester } from "../../../../../../shared/components/shared/testing/tester";
import { ControllerPeakShavingSymmetricSettingsComponent } from "./settings";

export function expectView(
    component: EdgeConfig.Component,
    edge: Edge,
    viewContext: OeFormlyViewTester.Context,
    testContext: TestContext,
    view: OeFormlyViewTester.View,
): void {
    expect(
        OeFormlyViewTester.apply(
            ControllerPeakShavingSymmetricSettingsComponent.getFormlyGeneralView(
                testContext.translate,
                component,
                edge,
            ),
            viewContext,
        ),
    ).toEqual(view);
}
