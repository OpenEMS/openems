import { FormControl, FormGroup } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { ButtonLabel } from "src/app/shared/components/modal/modal-button/modal-button";
import { GroupedNavigationTreeUtility, NavigationTree } from "src/app/shared/components/navigation/shared";
import { Converter } from "src/app/shared/components/shared/converter";
import { ChannelAddress, EdgeConfig } from "src/app/shared/shared";

export namespace SharedControllerIoHeatingRoom {
    export const PROPERTY_MODE = "_PropertyMode";

    export enum Mode {
        OFF = "OFF",
        MANUAL_LOW = "MANUAL_LOW",
        MANUAL_HIGH = "MANUAL_HIGH",
        AUTOMATIC = "AUTOMATIC",
    }

    export function getModeButtons(translate: TranslateService): ButtonLabel[] {
        return [
            {
                name: translate.instant("GENERAL.OFF"),
                value: Mode.OFF,
                icon: { color: "primary", name: "power-outline", size: "medium" },
            },
            {
                name: translate.instant("IO_HEATING_ROOM.MODE.MANUAL_LOW"),
                value: Mode.MANUAL_LOW,
                icon: { color: "primary", name: "caret-down-circle-outline", size: "medium" },
            },
            {
                name: translate.instant("IO_HEATING_ROOM.MODE.MANUAL_HIGH"),
                value: Mode.MANUAL_HIGH,
                icon: { color: "primary", name: "caret-up-circle-outline", size: "medium" },
            },
            {
                name: translate.instant("GENERAL.AUTOMATIC"),
                value: Mode.AUTOMATIC,
                icon: { color: "primary", name: "timer-outline", size: "medium" },
            },
        ];
    }

    export function convertToModeLabel(translate: TranslateService): Converter {
        return (value) => {
            switch (value) {
                case Mode.OFF:
                    return translate.instant("GENERAL.OFF");
                case Mode.MANUAL_LOW:
                    return translate.instant("IO_HEATING_ROOM.MODE.MANUAL_LOW");
                case Mode.MANUAL_HIGH:
                    return translate.instant("IO_HEATING_ROOM.MODE.MANUAL_HIGH");
                case Mode.AUTOMATIC:
                    return translate.instant("GENERAL.AUTOMATIC");
                default:
                    return "-";
            }
        };
    }

    export function getFormGroup(component: EdgeConfig.Component | null): FormGroup {
        return new FormGroup({
            mode: new FormControl(component?.properties?.mode ?? null),
        });
    }

    export function getChannelAddresses(component: EdgeConfig.Component): ChannelAddress[] {
        return [new ChannelAddress(component.id, PROPERTY_MODE)];
    }

    export function getNavigationTree(
        translate: TranslateService,
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
            translate,
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

    function createComponentNavigationTree(
        id: string,
        label: string,
        baseString: string,
        translate: TranslateService,
    ): NavigationTree {
        return new NavigationTree(
            id,
            { baseString },
            { name: "flame", color: "danger" },
            label,
            "label",
            [
                new NavigationTree(
                    id + "-mode",
                    { baseString: "mode" },
                    { name: "options-outline", color: "medium" },
                    translate.instant("IO_HEATING_ROOM.MODE.TITLE"),
                    "label",
                    [],
                    null,
                ),
            ],
            null,
        );
    }
}
