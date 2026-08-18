import { FormControl, FormGroup } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { NavigationConstants, NavigationTree } from "src/app/shared/components/navigation/shared";
import { State } from "src/app/shared/components/shared/converter";
import { Name } from "src/app/shared/components/shared/name";
import { OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { RouteService } from "src/app/shared/service/route.service";
import { ChannelAddress, CurrentData, Edge, EdgeConfig, Service } from "src/app/shared/shared";
import { EnerixControlMode } from "src/app/shared/type/general";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { ControlMode } from "../flat/flat";

export namespace SharedControllerEnerixControl {
    // hide automatic elements when mode is OFF
    const HIDE_ON_MODE_OFF = (el: { controlMode: EnerixControlMode }) => el.controlMode === EnerixControlMode.OFF;

    export type EnerixControlViewModel = {
        controlMode: EnerixControlMode;
    };

    export const getFormlyView = (
        translate: TranslateService,
        component: EdgeConfig.Component,
        edge: Edge,
    ): OeFormlyView<EnerixControlViewModel> => {
        return {
            title: component.alias,
            helpKey: "REDIRECT.CONTROLLER_CLEVER_PV",
            lines: [
                ...getFormlySharedModeAndStateLines(translate, component),
                ...getFormlySharedLines(translate),
                ...getFormlyManualOnView(translate, HIDE_ON_MODE_OFF),
            ],
            component: component,
            edge: edge,
        };
    };

    const getFormlyManualOnView = (
        translate: TranslateService,
        hideCondition: (field: { controlMode: EnerixControlMode }) => boolean,
    ): OeFormlyView<EnerixControlViewModel>["lines"] => [
        {
            type: "info-line",
            name: translate.instant("EDGE.INDEX.WIDGETS.ENERIX_CONTROL.NOTE"),
            style: { name: { fontWeight: "bold" } },
            hide: hideCondition,
        },
        {
            type: "info-line",
            name: translate.instant("EDGE.INDEX.WIDGETS.ENERIX_CONTROL.OVERWRITE_INFO"),
            hide: hideCondition,
        },
    ];

    type EnerixStatus = {
        remoteControlMode: ControlMode | null;
        state: State;
    };

    const getModeAndStateChannels = (component: EdgeConfig.Component): ChannelAddress[] => [
        new ChannelAddress(component.id, "RemoteControlMode"),
        new ChannelAddress(component.id, "UnableToSend"),
        new ChannelAddress(component.id, "_PropertyReadOnly"),
    ];

    const getEnerixStatus = (currentData: CurrentData, component: EdgeConfig.Component): EnerixStatus => {
        const remoteControlMode = currentData.allComponents[component.id + "/RemoteControlMode"] ?? null;
        const unableToSend = currentData.allComponents[component.id + "/UnableToSend"];
        const readOnly = currentData.allComponents[component.id + "/_PropertyReadOnly"];

        if (readOnly) {
            return {
                remoteControlMode,
                state: unableToSend ? State.DISCONNECTED : State.CONNECTED,
            };
        }

        if (remoteControlMode == null) {
            return {
                remoteControlMode: null,
                state: State.OFF,
            };
        }

        return {
            remoteControlMode,
            state: mapControlMode(remoteControlMode, component),
        };
    };

    export const getFormlySharedModeAndStateLines = (
        translate: TranslateService,
        component: EdgeConfig.Component,
    ): OeFormlyView<EnerixControlViewModel>["lines"] => [
        {
            type: "value-from-channels-line",
            name: translate.instant("GENERAL.STATE"),
            channelsToSubscribe: getModeAndStateChannels(component),
            value: (currentData: CurrentData) => {
                const status = getEnerixStatus(currentData, component);
                return CONVERT_ENERIX_CONTROL_STATE(translate)(status.state);
            },
        },
        {
            type: "value-from-channels-line",
            name: translate.instant("EDGE.INDEX.WIDGETS.ENERIX_CONTROL.OVERWRITE_MODE"),
            channelsToSubscribe: getModeAndStateChannels(component),
            value: (currentData: CurrentData) => {
                const status = getEnerixStatus(currentData, component);
                if (status.remoteControlMode == null) {
                    return CONVERT_ENERIX_CONTROL_STATE(translate)(status.state);
                }
                return getOverwriteLabel(status.remoteControlMode, status.state, translate);
            },
        },
    ];

    const getFormlySharedLines = (translate: TranslateService): OeFormlyView<EnerixControlViewModel>["lines"] => [
        {
            type: "horizontal-line",
        },
        {
            type: "buttons-from-form-control-line",
            name: translate.instant("GENERAL.MODE"),
            controlName: "controlMode",
            buttons: [
                {
                    name: translate.instant("GENERAL.ON"),
                    value: EnerixControlMode.REMOTE_CONTROL,
                    icon: {
                        color: "success",
                        name: "play-outline",
                        size: "medium",
                    },
                },
                {
                    name: translate.instant("GENERAL.OFF"),
                    value: EnerixControlMode.OFF,
                    icon: {
                        color: "danger",
                        name: "stop-circle-outline",
                        size: "medium",
                    },
                },
            ],
        },
        {
            type: "horizontal-line",
        },
    ];

    export function getChannelAddresses(
        service: Service,
        routeService: RouteService,
        component: EdgeConfig.Component,
    ): Promise<ChannelAddress[]> {
        const edge = service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);

        const enerixControlComponent =
            component ?? config.getComponentSafely(routeService.getRouteParam("componentId"));

        AssertionUtils.assertIsDefined(enerixControlComponent);
        return Promise.resolve([new ChannelAddress(enerixControlComponent.id, "_PropertyControlMode")]);
    }

    export function getFormGroup(): FormGroup {
        return new FormGroup({
            controlMode: new FormControl(null),
        });
    }

    export function getNavigationTree(
        translate: TranslateService,
        component: EdgeConfig.Component,
    ): ConstructorParameters<typeof NavigationTree> {
        return new NavigationTree(
            component.id,
            { baseString: "controller/enerix-control/" + component.id },
            { name: "swap-vertical-outline", color: "normal" },
            Name.METER_ALIAS_OR_ID(component),
            "label",
            [NavigationConstants.CommonNodes.HISTORY(translate), NavigationConstants.CommonNodes.SETTINGS(translate)],
            null,
        ).toConstructorParams();
    }

    /**
     * Converts Power2Heat-State
     *
     * @param translate The current language to be translated to
     * @returns Converted value
     */
    export const CONVERT_ENERIX_CONTROL_STATE = (translate: TranslateService) => {
        return (value: any): string => {
            switch (value) {
                case State.ON:
                    return translate.instant("GENERAL.ON");
                case State.NO_DISCHARGE:
                    return translate.instant("EDGE.INDEX.WIDGETS.ENERIX_CONTROL.NO_DISCHARGE");
                case State.CHARGE_FROM_GRID:
                    return translate.instant("EDGE.INDEX.WIDGETS.ENERIX_CONTROL.CHARGE_FROM_GRID");
                case State.DISCONNECTED:
                    return translate.instant("EDGE.INDEX.WIDGETS.ENERIX_CONTROL.DISCONNECTED");
                case State.CONNECTED:
                    return translate.instant("EDGE.INDEX.WIDGETS.ENERIX_CONTROL.CONNECTED");
                default:
                    return translate.instant("GENERAL.OFF");
            }
        };
    };
}

export function mapControlMode(mode: ControlMode, component: EdgeConfig.Component): State {
    switch (mode) {
        case ControlMode.IDLE:
            return component.properties.controlMode === "REMOTE_CONTROL" ? State.ON : State.OFF;
        case ControlMode.NO_DISCHARGE:
            return State.NO_DISCHARGE;
        case ControlMode.CHARGE_FROM_GRID:
            return State.CHARGE_FROM_GRID;
        default:
            return State.OFF;
    }
}

export function getOverwriteLabel(mode: ControlMode, state: State, translate: TranslateService): string {
    if (state === State.OFF || state === State.DISCONNECTED) {
        return translate.instant("EDGE.INDEX.WIDGETS.ENERIX_CONTROL.NO_OVERWRITE");
    }
    return mode === ControlMode.IDLE
        ? translate.instant("EDGE.INDEX.WIDGETS.ENERIX_CONTROL.NO_OVERWRITE")
        : translate.instant("EDGE.INDEX.WIDGETS.ENERIX_CONTROL.OVERWRITE");
}
