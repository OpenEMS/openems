import { FormControl, FormGroup } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { MetaComponent } from "src/app/shared/components/edge/config-components/meta/meta";
import { NavigationConstants, NavigationTree } from "src/app/shared/components/navigation/shared";
import { Converter } from "src/app/shared/components/shared/converter";
import { Name } from "src/app/shared/components/shared/name";
import { OeFormlyField, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { RouteService } from "src/app/shared/service/route.service";
import { ChannelAddress, CurrentData, Edge, EdgeConfig, Service, Utils } from "src/app/shared/shared";
import { Mode } from "src/app/shared/type/general";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";

export namespace SharedEssFixDigitalPowerControl {
    export type PowerDirection = "CHARGE" | "DISCHARGE";

    export type FormModel = {
        mode: Mode;
        powerDirection: PowerDirection;
        chargeOnceTargetSocEnable: boolean;
        isEssDischargeToGridAllowed: boolean;
        isEssChargeToGridAllowed: boolean;
        dischargeOnceTargetSocEnable: boolean;
    };

    // Creates a hide predicate that hides the field when the form mode does not match the given mode
    const DONOT_HIDE_ON_MODE = (mode: Mode) => (el: FormModel) => el.mode !== mode;

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
                ...getFormlyManualOnView(translate, DONOT_HIDE_ON_MODE(Mode.MANUAL_ON), unit),
                ...(isNewVersion ? getFormlyChargeOnceView(translate, DONOT_HIDE_ON_MODE(Mode.CHARGE_ONCE), unit) : []),
                ...(isNewVersion
                    ? getFormlyDischargeOnceView(translate, DONOT_HIDE_ON_MODE(Mode.DISCHARGE_ONCE), unit)
                    : []),
                ...(isNewVersion
                    ? getSharedChargeConsumptionFromGridView(
                          translate,
                          (el) => el.mode !== Mode.MANUAL_ON && el.mode !== Mode.CHARGE_ONCE,
                      )
                    : []),
                ...(isNewVersion
                    ? getSharedDischargeConsumptionFromGridView(
                          translate,
                          (el) => el.mode !== Mode.MANUAL_ON && el.mode !== Mode.DISCHARGE_ONCE,
                      )
                    : []),
            ],
            component: component,
            edge: edge,
        };
    };

    const getSharedChargeConsumptionFromGridView = (
        translate: TranslateService,
        hideCondition: (field: FormModel) => boolean,
    ): OeFormlyView<FormModel>["lines"] => {
        // Block is visible when mode is CHARGE_ONCE, or MANUAL_ON with CHARGE direction
        const blockHide = (el: FormModel) =>
            hideCondition(el) || (el.mode === Mode.MANUAL_ON && el.powerDirection !== "CHARGE");

        // Info-line is additionally hidden when charge-from-grid is not allowed
        const infoLineHide = (el: FormModel) => blockHide(el) || !el.isEssChargeToGridAllowed;

        return [
            {
                type: "horizontal-line",
                hide: blockHide,
            },
            {
                type: "toggle-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.ENERIX_CONTROL.CHARGE_FROM_GRID_ALLOWED"),
                controlName: "isEssChargeToGridAllowed",
                hide: blockHide,
            },
            {
                type: "horizontal-line",
                hide: infoLineHide,
            },
            {
                type: "info-line",
                html: translate.instant("EDGE.INDEX.WIDGETS.ENERIX_CONTROL.CHARGE_CONSUMPTION"),
                hide: infoLineHide,
            },
        ];
    };

    const getSharedDischargeConsumptionFromGridView = (
        translate: TranslateService,
        hideCondition: (field: FormModel) => boolean,
    ): OeFormlyView<FormModel>["lines"] => {
        // Block is visible when mode is DISCHARGE_ONCE, or MANUAL_ON with DISCHARGE direction
        const blockHide = (el: FormModel) =>
            hideCondition(el) || (el.mode === Mode.MANUAL_ON && el.powerDirection !== "DISCHARGE");

        // Info-line is additionally hidden when discharge-to-grid is not allowed
        const infoLineHide = (el: FormModel) => blockHide(el) || !el.isEssDischargeToGridAllowed;

        return [
            {
                type: "horizontal-line",
                hide: blockHide,
            },
            {
                type: "toggle-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.ENERIX_CONTROL.DISCHARGE_TO_GRID_ACTIVATE"),
                controlName: "isEssDischargeToGridAllowed",
                hide: blockHide,
            },
            {
                type: "horizontal-line",
                hide: infoLineHide,
            },
            {
                type: "info-line",
                html: translate.instant("EDGE.INDEX.WIDGETS.ENERIX_CONTROL.DISCHARGE_TO_GRID_DESCRIPTION"),
                hide: infoLineHide,
            },
        ];
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
    ];

    const getFormlyDischargeOnceView = (
        translate: TranslateService,
        hideCondition: (field: FormModel) => boolean,
        unit: string,
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

            const meta = new MetaComponent(config);
            if (meta != null) {
                channelAddresses.push(
                    new ChannelAddress(meta.id, CHANNEL_ID_META_IS_ESS_CHARGE_FROM_GRID_ALLOWED),
                    new ChannelAddress(meta.id, CHANNEL_ID_META_IS_ESS_DISCHARGE_TO_GRID_ALLOWED),
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
            chargeOncePower: new FormControl(null),
            isEssChargeToGridAllowed: new FormControl(null),
            isEssDischargeToGridAllowed: new FormControl(null),
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
            [
                NavigationConstants.CommonNodes.INFO(translate, {
                    source: component.id,
                }),
            ],
            null,
        ).toConstructorParams();
    }
}
