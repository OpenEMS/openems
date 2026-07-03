import { TranslateService } from "@ngx-translate/core";
import { EdgeConfig } from "src/app/shared/shared";
import { NavigationConstants, NavigationTree } from "../../../../../shared/components/navigation/shared";

export namespace SharedSystemIndustrialXl {

    export function getNavigationTree(translate: TranslateService, componentId: EdgeConfig.Component["id"]): ConstructorParameters<typeof NavigationTree> | null {
        return new NavigationTree(componentId + "/industrial-xl", { baseString: componentId + "/industrial-xl" }, { name: "battery-full-outline", color: "medium" }, "FENECON Industrial XL", "label", [
            NavigationConstants.CommonNodes.INFO(translate, { source: componentId }),
        ], null).toConstructorParams();
    }
}
