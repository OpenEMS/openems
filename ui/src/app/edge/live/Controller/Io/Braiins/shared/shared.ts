import { TranslateService } from "@ngx-translate/core";
import { GroupedNavigationTreeUtility, NavigationTree } from "src/app/shared/components/navigation/shared";
import { Converter } from "src/app/shared/components/shared/converter";
import { EdgeConfig } from "src/app/shared/shared";

export namespace ControllerBraiinsShared {
    export const PROPERTY_MODE = "_PropertyMode";
    export const ACTIVE_POWER = "ActivePower";
    export const EFFECTIVE_MODE = "EffectiveMode";
    const NAVIGATION_BASE = "controller/braiins";

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

        return createComponentNavigationTree(
            componentId,
            translate.instant("MENU.GROUPS.BRAIINS"),
            `${NAVIGATION_BASE}/${componentId}`,
            translate,
        ).toConstructorParams();
    }

    export function getNavigationTreeAsChild(
        translate: TranslateService,
        componentId: EdgeConfig.Component["id"],
        config: EdgeConfig,
    ): NavigationTree | null {
        const component = getComponentSafely(config, componentId);
        if (component == null) {
            return null;
        }

        const label = component.alias?.trim() || component.id;
        return createComponentNavigationTree(componentId, label, componentId, translate);
    }

    export function getGroupedNavigationTree(
        translate: TranslateService,
        componentIds: EdgeConfig.Component["id"][],
        config: EdgeConfig,
        factoryId: EdgeConfig.Factory["id"],
    ): NavigationTree | null {
        return GroupedNavigationTreeUtility.createGroupedNavigationTree(
            NAVIGATION_BASE,
            { name: "logo-bitcoin", color: "normal" },
            "MENU.GROUPS.BRAIINS",
            NAVIGATION_BASE,
            translate,
            componentIds,
            config,
            factoryId,
            (componentId) => getNavigationTreeAsChild(translate, componentId, config),
        );
    }

    function createComponentNavigationTree(
        id: string,
        label: string,
        baseString: string,
        translate: TranslateService,
    ): NavigationTree {
        const scheduleChildren: NavigationTree[] = [
            new NavigationTree(
                "edit-task",
                { baseString: "edit-task" },
                { name: "create-outline" },
                translate.instant("JS_SCHEDULE.EDIT_TASK"),
                "label",
                [],
                null,
                { showOrder: "HIDE" },
            ),
            new NavigationTree(
                "add-task",
                { baseString: "add-task" },
                { name: "add-outline" },
                translate.instant("JS_SCHEDULE.ADD_TASK"),
                "label",
                [],
                null,
                { showOrder: "HIDE" },
            ),
        ];

        const children: NavigationTree[] = [
            new NavigationTree(
                "mode",
                { baseString: "mode" },
                { name: "checkmark-done-outline", color: "medium" },
                translate.instant("BRAIINS_SINGLE.MODE.LABEL"),
                "label",
                [],
                null,
            ),
            new NavigationTree(
                "schedule",
                { baseString: "schedule" },
                { name: "calendar-outline", color: "warning" },
                translate.instant("HEAT.SCHEDULE.SCHEDULE"),
                "label",
                scheduleChildren,
                null,
            ),
        ];

        return new NavigationTree(
            id,
            { baseString },
            { name: "logo-bitcoin", color: "normal" },
            label,
            "label",
            children,
            null,
        );
    }

    /**
     * Converts a string or numeric mode to a presentable label
     *
     * @param raw The raw value
     * @returns The value for chosen mode
     */
    export const CONVERT_TO_MODE_LABEL = (translate: TranslateService): Converter => {
        return (raw): string => {
            return Converter.IF_NUMBER_OR_STRING(raw, (value) => {
                switch (value) {
                    case 1:
                    case Mode.ON:
                        return translate.instant("BRAIINS_SINGLE.MODE.ON");
                    case 0:
                    case Mode.OFF:
                        return translate.instant("BRAIINS_SINGLE.MODE.OFF");
                    default:
                        return Converter.HIDE_VALUE(value);
                }
            });
        };
    };

    export enum Mode {
        ON = "ON",
        OFF = "OFF",
    }
}
