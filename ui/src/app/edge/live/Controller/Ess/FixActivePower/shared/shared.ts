import { FormControl, FormGroup } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { NavigationTree } from "src/app/shared/components/navigation/shared";
import { Converter } from "src/app/shared/components/shared/converter";
import { Name } from "src/app/shared/components/shared/name";
import { OeFormlyField, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { RouteService } from "src/app/shared/service/route/route.service";
import { ChannelAddress, CurrentData, Edge, EdgeConfig, Service, Utils } from "src/app/shared/shared";
import { Mode } from "src/app/shared/type/general";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";

export namespace SharedEssFixDigitalPowerControl {
    export type PowerDirection = "CHARGE" | "DISCHARGE";

    export type FormModel = {
        mode: Mode;
        powerDirection: PowerDirection;
        chargeOnceTargetSocEnable: boolean;
        dischargeOnceTargetSocEnable: boolean;
        isEssChargeFromGridAllowed: boolean | null;
        isEssDischargeToGridAllowed: boolean | null;
    };

    // Creates a hide predicate that hides the field when the form mode does not match the given mode
    const DO_NOT_HIDE_ON_MODE = (mode: Mode) => (el: FormModel) => el.mode !== mode;

    export const PROPERTY_MODE = "_PropertyMode";
    export const PROPERTY_POWER = "_PropertyPower";
    export const PROPERTY_CHARGE_ONCE_POWER = "_PropertyChargeOncePower";
    export const PROPERTY_CHARGE_ONCE_TARGET_SOC_ENABLE = "_PropertyChargeOnceTargetSocEnable";
    export const PROPERTY_CHARGE_ONCE_TARGET_SOC = "_PropertyChargeOnceTargetSoc";
    export const PROPERTY_DISCHARGE_ONCE_POWER = "_PropertyDischargeOncePower";
    export const PROPERTY_DISCHARGE_ONCE_TARGET_SOC_ENABLE = "_PropertyDischargeOnceTargetSocEnable";
    export const PROPERTY_DISCHARGE_ONCE_TARGET_SOC = "_PropertyDischargeOnceTargetSoc";
    export const CHANNEL_ID_META_IS_ESS_CHARGE_FROM_GRID_ALLOWED = "IsEssChargeFromGridAllowed";
    export const CHANNEL_ID_META_IS_ESS_DISCHARGE_TO_GRID_ALLOWED = "IsEssDischargeToGridAllowed";
    export const TARGET_AFTER_LIMITATIONS = "TargetAfterLimitations";

    export const NEW_FEATURES_MIN_VERSION = "2026.6.2";

    export const getFormlyView = (
        translate: TranslateService,
        component: EdgeConfig.Component,
        edge: Edge,
        unit: string,
        powerConverter: (value: number | null) => string,
        navigateToGridSettings: () => void,
    ): OeFormlyView<FormModel> => {
        const isNewVersion =
            (edge.isVersionAtLeast(NEW_FEATURES_MIN_VERSION) || edge.isVersionAtLeast("2026.6.2-SNAPSHOT")) &&
            component.factoryId !== "Controller.Symmetric.FixReactivePower";
        return {
            title: component.alias,
            icon: {
                name: "swap-vertical-outline",
                color: "normal",
                size: "large",
            },
            lines: [
                ...getFormlySharedModeAndStateLines(translate, component, powerConverter),
                ...getFormlySharedLines(translate, isNewVersion),
                ...getFormlyManualOnView(translate, DO_NOT_HIDE_ON_MODE(Mode.MANUAL_ON), unit),
                ...(isNewVersion
                    ? getFormlyChargeOnceView(
                          translate,
                          DO_NOT_HIDE_ON_MODE(Mode.CHARGE_ONCE),
                          unit,
                          navigateToGridSettings,
                      )
                    : []),
                ...(isNewVersion
                    ? getFormlyDischargeOnceView(
                          translate,
                          DO_NOT_HIDE_ON_MODE(Mode.DISCHARGE_ONCE),
                          unit,
                          navigateToGridSettings,
                      )
                    : []),
            ],
            component: component,
            edge: edge,
        };
    };

    const getFormlyManualOnView = (
        translate: TranslateService,
        hideCondition: (field: FormModel) => boolean,
        unit: string,
    ): OeFormlyView<FormModel>["lines"] => [
        {
            type: "select-line",
            name: translate.instant("GENERAL.CHARGE_DISCHARGE"),
            controlName: "powerDirection",
            options: [
                { value: "CHARGE", name: translate.instant("GENERAL.CHARGE") },
                {
                    value: "DISCHARGE",
                    name: translate.instant("GENERAL.DISCHARGE"),
                },
            ],
            hide: hideCondition,
        },
        {
            type: "input-line",
            name: translate.instant("GENERAL.POWER"),
            controlName: "power",
            properties: {
                unit: unit,
            },
            hide: hideCondition,
        },
    ];

    const getFormlyChargeOnceView = (
        translate: TranslateService,
        hideCondition: (field: FormModel) => boolean,
        unit: string,
        navigateToGridSettings: () => void,
    ): OeFormlyView<FormModel>["lines"] => [
        {
            type: "input-line",
            name: translate.instant("GENERAL.POWER"),
            controlName: "chargeOncePower",
            properties: {
                unit: unit,
            },
            hide: hideCondition,
        },
        {
            type: "toggle-line",
            name: translate.instant("GENERAL.TARGET_SOC"),
            controlName: "chargeOnceTargetSocEnable",
            hide: hideCondition,
        },
        {
            type: "input-line",
            name: translate.instant("GENERAL.SOC"),
            controlName: "chargeOnceTargetSoc",
            properties: {
                unit: "%",
            },
            hide: (el) => hideCondition(el) || !el.chargeOnceTargetSocEnable,
        },
        {
            type: "info-line",
            name: translate.instant("EDGE.INDEX.GRID.CHARGE_ONCE_GRID_PERMISSION_REQUIRED"),
            link: {
                text: translate.instant("EDGE.INDEX.GRID.SETTINGS_LINK"),
                callback: navigateToGridSettings,
            },
            icon: { name: "oe-info", color: "primary", size: "large" },
            hide: (el) => hideCondition(el) || el.isEssChargeFromGridAllowed !== false,
        },
    ];

    const getFormlyDischargeOnceView = (
        translate: TranslateService,
        hideCondition: (field: FormModel) => boolean,
        unit: string,
        navigateToGridSettings: () => void,
    ): OeFormlyView<FormModel>["lines"] => [
        {
            type: "input-line",
            name: translate.instant("GENERAL.POWER"),
            controlName: "dischargeOncePower",
            properties: {
                unit: unit,
            },
            hide: hideCondition,
        },
        {
            type: "toggle-line",
            name: translate.instant("GENERAL.TARGET_SOC"),
            controlName: "dischargeOnceTargetSocEnable",
            hide: hideCondition,
        },
        {
            type: "input-line",
            name: translate.instant("GENERAL.SOC"),
            controlName: "dischargeOnceTargetSoc",
            properties: {
                unit: "%",
            },
            hide: (el) => hideCondition(el) || !el.dischargeOnceTargetSocEnable,
        },
        {
            type: "info-line",
            name: translate.instant("EDGE.INDEX.GRID.DISCHARGE_ONCE_GRID_PERMISSION_REQUIRED"),
            link: {
                text: translate.instant("EDGE.INDEX.GRID.SETTINGS_LINK"),
                callback: navigateToGridSettings,
            },
            icon: { name: "oe-info", color: "primary", size: "large" },
            hide: (el) => hideCondition(el) || el.isEssDischargeToGridAllowed !== false,
        },
    ];

    export const getFormlySharedModeAndStateLines = (
        translate: TranslateService,
        component: EdgeConfig.Component,
        powerConverter: (value: number | null) => string,
    ): OeFormlyView["lines"] => {
        const lines: OeFormlyField[] = [];
        const TARGET_AFTER_LIMITATIONS_CHANNEL = component.id + "/" + TARGET_AFTER_LIMITATIONS;

        lines.push(
            {
                type: "channel-line",
                name: translate.instant("GENERAL.STATE"),
                channel: new ChannelAddress(component.id, PROPERTY_MODE).toString(),
                converter: Converter.CONTROLLER_PROPERTY_MODES(translate),
            },
            {
                type: "value-from-channels-line",
                name: translate.instant("GENERAL.CHARGE"),
                channelsToSubscribe: [
                    new ChannelAddress(component.id, PROPERTY_MODE),
                    new ChannelAddress(component.id, TARGET_AFTER_LIMITATIONS),
                ],
                value: (currentData: CurrentData) => {
                    const power = currentData.allComponents[TARGET_AFTER_LIMITATIONS_CHANNEL];
                    const powerValue = Utils.convertChargeDischargePower(translate, power);
                    return powerConverter(powerValue.value);
                },
                filter: (currentData: CurrentData) => {
                    const mode = currentData.allComponents[new ChannelAddress(component.id, PROPERTY_MODE).toString()];
                    const power = currentData.allComponents[TARGET_AFTER_LIMITATIONS_CHANNEL] ?? null;

                    return (mode === Mode.MANUAL_ON || mode === Mode.CHARGE_ONCE) && power != null && power < 0;
                },
            },
            {
                type: "value-from-channels-line",
                name: translate.instant("GENERAL.DISCHARGE"),
                channelsToSubscribe: [
                    new ChannelAddress(component.id, PROPERTY_MODE),
                    new ChannelAddress(component.id, TARGET_AFTER_LIMITATIONS),
                ],
                value: (currentData: CurrentData) => {
                    const power = currentData.allComponents[TARGET_AFTER_LIMITATIONS_CHANNEL];
                    return powerConverter(Math.abs(power ?? 0));
                },
                filter: (currentData: CurrentData) => {
                    const mode = currentData.allComponents[new ChannelAddress(component.id, PROPERTY_MODE).toString()];
                    const power = currentData.allComponents[TARGET_AFTER_LIMITATIONS_CHANNEL] ?? null;
                    return (mode === Mode.MANUAL_ON || mode === Mode.DISCHARGE_ONCE) && (power == null || power >= 0);
                },
            },
        );
        return lines;
    };

    const getFormlySharedLines = (translate: TranslateService, isNewVersion: boolean): OeFormlyView["lines"] => [
        {
            type: "horizontal-line",
        },
        {
            type: "buttons-from-form-control-line",
            name: translate.instant("GENERAL.MODE"),
            controlName: "mode",
            buttons: [
                {
                    name: translate.instant("GENERAL.ON"),
                    value: Mode.MANUAL_ON,
                    icon: {
                        color: "success",
                        name: "play-outline",
                        size: "medium",
                    },
                },
                ...(isNewVersion
                    ? [
                          {
                              name: translate.instant("GENERAL.CHARGE_ONCE"),
                              value: Mode.CHARGE_ONCE,
                              icon: {
                                  color: "success",
                                  name: "arrow-down-outline",
                                  size: "medium",
                              },
                          },
                          {
                              name: translate.instant("GENERAL.DISCHARGE_ONCE"),
                              value: Mode.DISCHARGE_ONCE,
                              icon: {
                                  color: "success",
                                  name: "arrow-up-outline",
                                  size: "medium",
                              },
                          },
                      ]
                    : []),
                {
                    name: translate.instant("GENERAL.OFF"),
                    value: Mode.MANUAL_OFF,
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
        component: EdgeConfig.Component | null = null,
    ): Promise<ChannelAddress[]> {
        const edge = service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);

        const EnerixControlComponent =
            component ?? config.getComponentSafely(routeService.getRouteParam("componentId"));

        AssertionUtils.assertIsDefined(EnerixControlComponent);

        const isNewVersion =
            edge.isVersionAtLeast(NEW_FEATURES_MIN_VERSION) || edge.isVersionAtLeast("2026.6.2-SNAPSHOT");

        const channelAddresses = [
            new ChannelAddress(EnerixControlComponent.id, PROPERTY_MODE),
            new ChannelAddress(EnerixControlComponent.id, PROPERTY_POWER),
        ];

        if (isNewVersion) {
            channelAddresses.push(
                new ChannelAddress(EnerixControlComponent.id, PROPERTY_CHARGE_ONCE_POWER),
                new ChannelAddress(EnerixControlComponent.id, PROPERTY_CHARGE_ONCE_TARGET_SOC_ENABLE),
                new ChannelAddress(EnerixControlComponent.id, PROPERTY_CHARGE_ONCE_TARGET_SOC),
                new ChannelAddress(EnerixControlComponent.id, PROPERTY_DISCHARGE_ONCE_POWER),
                new ChannelAddress(EnerixControlComponent.id, PROPERTY_DISCHARGE_ONCE_TARGET_SOC_ENABLE),
                new ChannelAddress(EnerixControlComponent.id, PROPERTY_DISCHARGE_ONCE_TARGET_SOC),
            );

            if (EnerixControlComponent.factoryId !== "Controller.Symmetric.FixReactivePower") {
                channelAddresses.push(
                    new ChannelAddress("_meta", CHANNEL_ID_META_IS_ESS_CHARGE_FROM_GRID_ALLOWED),
                    new ChannelAddress("_meta", CHANNEL_ID_META_IS_ESS_DISCHARGE_TO_GRID_ALLOWED),
                );
            }
        }

        return Promise.resolve(channelAddresses);
    }

    export function getFormGroup(): FormGroup {
        return new FormGroup({
            mode: new FormControl(null),
            powerDirection: new FormControl<PowerDirection>("DISCHARGE"),
            power: new FormControl(null),
            isEssChargeFromGridAllowed: new FormControl(null),
            isEssDischargeToGridAllowed: new FormControl(null),
            chargeOncePower: new FormControl(null),
            chargeOnceTargetSocEnable: new FormControl(null),
            chargeOnceTargetSoc: new FormControl(null),
            dischargeOncePower: new FormControl(null),
            dischargeOnceTargetSocEnable: new FormControl(null),
            dischargeOnceTargetSoc: new FormControl(null),
        });
    }

    export function getNavigationTree(
        translate: TranslateService,
        component: EdgeConfig.Component,
        baseString: string,
    ): ConstructorParameters<typeof NavigationTree> {
        return new NavigationTree(
            component.id,
            { baseString: baseString },
            { name: "swap-vertical-outline", color: "normal" },
            Name.METER_ALIAS_OR_ID(component),
            "label",
            [],
            null,
        ).toConstructorParams();
    }
}
