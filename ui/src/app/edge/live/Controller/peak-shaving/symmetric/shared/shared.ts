import { TranslateService } from "@ngx-translate/core";
import { NavigationConstants, NavigationTree, } from "src/app/shared/components/navigation/shared";
import { Name } from "src/app/shared/components/shared/name";
import { EdgeConfig } from "src/app/shared/shared";

export namespace SharedControllerPeakShavingSymmetric {
    export function getNavigationTree(
        translate: TranslateService,
        component: EdgeConfig.Component,
    ): ConstructorParameters<typeof NavigationTree> {
        return new NavigationTree(
            component.id,
            { baseString: "controller/peak-shaving-symmetric/" + component.id },
            { name: "trending-down-outline", color: "normal" },
            Name.METER_ALIAS_OR_ID(component),
            "label",
            [
                NavigationConstants.CommonNodes.HISTORY(translate),
                NavigationConstants.CommonNodes.SETTINGS(translate),
            ],
            null,
        ).toConstructorParams();
    }
}
