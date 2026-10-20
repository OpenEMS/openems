import { TranslateService } from "@ngx-translate/core";
import { NavigationConstants, NavigationTree } from "src/app/shared/components/navigation/shared";
import { Name } from "src/app/shared/components/shared/name";
import { EdgeConfig } from "src/app/shared/shared";

export namespace SharedEvcsApiCluster {
    function getComponentSafely(
        config: EdgeConfig,
        componentId: EdgeConfig.Component["id"],
    ): EdgeConfig.Component | null {
        return config.getComponentSafely(componentId);
    }

    export function getNavigationTree(
        translate: TranslateService,
        componentId: EdgeConfig.Component["id"],
        config: EdgeConfig,
    ): ConstructorParameters<typeof NavigationTree> | null {
        const component = getComponentSafely(config, componentId);
        if (component == null) {
            return null;
        }

        const tree = createComponentNavigationTree(
            componentId,
            Name.METER_ALIAS_OR_ID(component),
            "controller/evcs/" + componentId,
            translate,
            config,
        );
        return tree.toConstructorParams();
    }

    export function createComponentNavigationTree(
        id: string,
        label: string,
        baseString: string,
        translate: TranslateService,
        config: EdgeConfig,
    ): NavigationTree {
        const isReadOnly =
            config.getPropertyFromComponent<boolean>(config.getComponentSafelyOrDefault(id), "readOnly") ?? false;
        return new NavigationTree(
            id,
            { baseString: baseString },
            { name: "oe-evcs", color: "normal" },
            label,
            "label",
            [
                ...(isReadOnly ? [] : [NavigationConstants.CommonNodes.SETTINGS(translate, id, "HIGH")]),
                NavigationConstants.CommonNodes.HISTORY(translate, id, []),
            ],
            null,
            { hideFavorite: false },
        );
    }
}
