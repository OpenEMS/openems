import { TranslateService } from "@ngx-translate/core";
import { NavigationTree } from "src/app/shared/components/navigation/shared";
import { EdgeConfig } from "src/app/shared/shared";

export namespace SharedControllerIoHeatingRoom {

    export function getNavigationTree(_translate: TranslateService, componentId: EdgeConfig.Component["id"], config: EdgeConfig): ConstructorParameters<typeof NavigationTree> | null {
        const component = config.getComponentSafely(componentId);
        if (component == null) {
            return null;
        }

        const label = component.alias?.trim() || component.id;
        return createComponentNavigationTree(componentId, label, "io-heating-room/" + componentId).toConstructorParams();
    }

    export function getNavigationTreeAsChild(_translate: TranslateService, componentId: EdgeConfig.Component["id"], config: EdgeConfig): NavigationTree | null {
        const component = config.getComponentSafely(componentId);
        if (component == null) {
            return null;
        }

        const label = component.alias?.trim() || component.id;
        return createComponentNavigationTree(componentId, label, componentId);
    }

    export function getGroupedNavigationTree(translate: TranslateService, componentIds: EdgeConfig.Component["id"][], config: EdgeConfig): ConstructorParameters<typeof NavigationTree> | null {
        const children = componentIds
            .slice()
            .sort((left, right) => compareByAliasThenComponentId(config, left, right))
            .map(componentId => getNavigationTreeAsChild(translate, componentId, config))
            .filter((child): child is NavigationTree => child != null);

        if (children.length <= 0) {
            return null;
        }

        return new NavigationTree(
            "heating-room-controllers",
            { baseString: "io-heating-room" },
            { name: "flame", color: "danger" },
            translate.instant("MENU.GROUPS.HEATING_ROOM"),
            "label",
            children,
            null,
        ).toConstructorParams();
    }

    function createComponentNavigationTree(id: string, label: string, baseString: string): NavigationTree {
        return new NavigationTree(id, { baseString }, { name: "flame", color: "danger" }, label, "label", [], null);
    }

    // TODO should be applied to all Widgets
    function compareByAliasThenComponentId(config: EdgeConfig, leftComponentId: string, rightComponentId: string): number {
        const leftAlias = config.getComponentSafely(leftComponentId)?.alias ?? "";
        const rightAlias = config.getComponentSafely(rightComponentId)?.alias ?? "";
        const aliasComparison = leftAlias.localeCompare(rightAlias);
        if (aliasComparison !== 0) {
            return aliasComparison;
        }

        return leftComponentId.localeCompare(rightComponentId);
    }
}
