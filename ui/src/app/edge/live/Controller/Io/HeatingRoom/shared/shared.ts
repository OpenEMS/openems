import { TranslateService } from "@ngx-translate/core";
import { GroupedNavigationTreeUtility, NavigationTree } from "src/app/shared/components/navigation/shared";
import { EdgeConfig } from "src/app/shared/shared";

export namespace SharedControllerIoHeatingRoom {
    export function getNavigationTree(
        _translate: TranslateService,
        componentId: EdgeConfig.Component["id"],
        config: EdgeConfig,
    ): ConstructorParameters<typeof NavigationTree> | null {
        const component = config.getComponentSafely(componentId);
        if (component == null) {
            return null;
        }

        const label = component.alias?.trim() || component.id;
        return createComponentNavigationTree(
            componentId,
            label,
            "io-heating-room/" + componentId,
        ).toConstructorParams();
    }

    export function getGroupedNavigationTree(
        translate: TranslateService,
        componentIds: EdgeConfig.Component["id"][],
        config: EdgeConfig,
        factoryId: EdgeConfig.Factory["id"],
    ): NavigationTree | null {
        return GroupedNavigationTreeUtility.createGroupedNavigationTree(
            "heating-room-controllers",
            { name: "flame", color: "danger" },
            "MENU.GROUPS.HEATING_ROOM",
            "io-heating-room",
            translate,
            componentIds,
            config,
            factoryId,
            (componentId) =>
                GroupedNavigationTreeUtility.getNavigationTreeAsChild(
                    translate,
                    componentId,
                    config,
                    createComponentNavigationTree,
                ),
        );
    }

    function createComponentNavigationTree(id: string, label: string, baseString: string): NavigationTree {
        return new NavigationTree(id, { baseString }, { name: "flame", color: "danger" }, label, "label", [], null);
    }
}
