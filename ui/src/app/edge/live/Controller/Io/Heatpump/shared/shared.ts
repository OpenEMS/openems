import { FormControl, FormGroup } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { ButtonLabel } from "src/app/shared/components/modal/modal-button/modal-button";
import { NavigationConstants, NavigationTree } from "src/app/shared/components/navigation/shared";
import { Converter } from "src/app/shared/components/shared/converter";
import { Name } from "src/app/shared/components/shared/name";
import { OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, Edge, EdgeConfig, EdgePermission, Service } from "src/app/shared/shared";
import { Mode } from "src/app/shared/type/general";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";

export namespace SharedControllerIoHeatpump {
    const PROPERTY_MODE: string = "_PropertyMode";
    // hide manual elements when mode is AUTOMATIC
    const HIDE_ON_MODE_AUTOMATIC = (el: { mode: HeatpumpMode }) => el.mode === HeatpumpMode.AUTOMATIC;
    // hide automatic elements when mode is manual
    const HIDE_ON_MODE_MANUAL = (el: { mode: HeatpumpMode }) => el.mode === HeatpumpMode.MANUAL;

    export const getFormlyView = (
        translate: TranslateService,
        component: EdgeConfig.Component,
        edge: Edge,
    ): OeFormlyView<{ mode: HeatpumpMode }> => {
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);
        return {
            title: component.alias,
            helpKey: "REDIRECT.CONTROLLER_IO_HEAT_PUMP_SG_READY",
            lines: [
                ...getFormlySharedLines(translate, component, config),
                ...getFormlyAutomaticView(translate, HIDE_ON_MODE_MANUAL),
                ...getFormlyManualView(translate, HIDE_ON_MODE_AUTOMATIC),
            ],
            component: component,
            edge: edge,
        };
    };

    export const getFormlySettingsView = (
        translate: TranslateService,
        component: EdgeConfig.Component,
        edge: Edge,
    ): OeFormlyView<{ mode: HeatpumpMode }> => {
        return {
            title: translate.instant("MENU.SETTINGS"),
            helpKey: "REDIRECT.CONTROLLER_IO_HEAT_PUMP_SG_READY",
            icon: { name: "settings-outline", color: "medium", size: "large" },
            lines: [
                {
                    type: "info-line",
                    name: [
                        {
                            text: translate.instant("GENERAL.AUTOMATIC"),
                        },
                        {
                            text: translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP"),
                        },
                    ],
                },
                ...getFormlyAutomaticView(translate, () => false),
            ],
            component,
            edge,
        };
    };

    export const getFormlyBaseModeView = (
        translate: TranslateService,
        component: EdgeConfig.Component,
        edge: Edge,
    ): OeFormlyView<{ mode: HeatpumpMode }> => {
        return {
            title: translate.instant("GENERAL.BASE_MODE"),
            helpKey: "REDIRECT.CONTROLLER_IO_HEAT_PUMP_SG_READY",
            icon: { name: "repeat-outline", color: "production", size: "large" },
            lines: [
                {
                    type: "image-line",
                    img: {
                        url: "icons/component/heatpump.svg",
                        width: 20,
                        height: 20,
                        color: "var(--ion-color-heatpump-base)",
                        style: {
                            maxWidth: "20rem",
                            justifySelf: "center",
                            paddingBottom: "var(--ion-padding)",
                        },
                    },
                },
                {
                    type: "radio-buttons-from-form-control-line",
                    name: "base-state",
                    controlName: "baseMode",
                    buttons: getStateRadioButtons(translate),
                },
            ],
            component,
            edge,
        };
    };

    const getFormlyAutomaticView = (
        translate: TranslateService,
        hideCondition: (field: { mode: HeatpumpMode }) => boolean,
    ): OeFormlyView<{ mode: HeatpumpMode }>["lines"] => {
        const lines: OeFormlyView<{ mode: HeatpumpMode }>["lines"] = [
            {
                type: "toggle-line",
                controlName: "automaticRecommendationCtrlEnabled",
                name: translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.SWITCH_ON_REC"),
            },
            {
                type: "input-line",
                controlName: "automaticRecommendationSurplusPower",
                name: translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.GRID_SELL"),
                properties: { unit: "W" },
            },
            {
                type: "horizontal-line",
            },
            {
                type: "toggle-line",
                controlName: "automaticForceOnCtrlEnabled",
                name: translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.SWITCH_ON_COM"),
            },
            {
                type: "input-line",
                controlName: "automaticForceOnSurplusPower",
                name: translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.GRID_SELL"),
                properties: { unit: "W" },
            },
            {
                type: "value-from-form-control-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.ABOVE_SOC"),
                controlName: "automaticForceOnSoc",
                converter: Converter.STATE_IN_PERCENT,
            },
            {
                type: "range-button-from-form-control-line",
                controlName: "automaticForceOnSoc",
                properties: {
                    tickMax: 100,
                    tickMin: 0,
                    tickFormatter: (val) => Converter.STATE_IN_PERCENT(val),
                    pinFormatter: (val) => Converter.STATE_IN_PERCENT(val),
                },
            },
            {
                type: "horizontal-line",
            },
            {
                type: "toggle-line",
                controlName: "automaticLockCtrlEnabled",
                name: translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.LOCK"),
            },
            {
                type: "input-line",
                controlName: "automaticLockGridBuyPower",
                name: translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.GRID_BUY"),
                properties: { unit: "W" },
            },
            {
                type: "value-from-form-control-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.ABOVE_SOC"),
                controlName: "automaticLockSoc",
                converter: Converter.STATE_IN_PERCENT,
            },
            {
                type: "range-button-from-form-control-line",
                controlName: "automaticLockSoc",
                properties: {
                    tickMax: 100,
                    tickMin: 0,
                    tickFormatter: (val) => Converter.STATE_IN_PERCENT(val),
                    pinFormatter: (val) => Converter.STATE_IN_PERCENT(val),
                },
            },
            {
                type: "horizontal-line",
            },
            {
                type: "input-line",
                controlName: "minimumSwitchingTime",
                name: translate.instant("EDGE.INDEX.WIDGETS.SINGLETHRESHOLD.MIN_SWITCHING_TIME"),
                properties: { unit: "s" },
            },
        ];

        return lines.map((line) => ({
            ...line,
            hide: hideCondition,
        }));
    };

    const getFormlyManualView = (
        translate: TranslateService,
        hideCondition: (field: { mode: HeatpumpMode }) => boolean,
    ): OeFormlyView<{ mode: HeatpumpMode }>["lines"] => [
        {
            type: "select-line",
            controlName: "manualState",
            name: translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.OPERATING_STATUS"),
            options: getManualOptions(translate),
            hide: hideCondition,
        },
    ];

    const getFormlySharedLines = (
        translate: TranslateService,
        component: EdgeConfig.Component,
        config: EdgeConfig,
    ): OeFormlyView["lines"] => {
        const lines: OeFormlyView["lines"] = [];
        const consumptionMeter = getConsumptionMeter(config, component);
        if (consumptionMeter) {
            lines.push({
                type: "channel-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.HEAT.HEATING_OUTPUT"),
                channel: consumptionMeter.id + "/ActivePower",
                converter: Converter.POWER_IN_KILO_WATT,
            });
        }

        lines.push(
            {
                type: "channel-line",
                name: translate.instant("GENERAL.STATE"),
                channel: component.id + "/Status",
                converter: Converter.HEAT_PUMP_STATES(translate),
            },
            {
                type: "channel-line",
                name: translate.instant("GENERAL.MODE"),
                channel: component.id + "/" + PROPERTY_MODE,
                converter: Converter.CONTROLLER_PROPERTY_MODES(translate),
            },
            {
                type: "horizontal-line",
            },
            {
                type: "buttons-from-form-control-line",
                name: translate.instant("GENERAL.MODE"),
                controlName: "mode",
                buttons: [
                    {
                        name: translate.instant("GENERAL.MANUALLY"),
                        value: Mode.MANUAL,
                        icon: { color: "success", name: "options-outline", size: "medium" },
                    },
                    {
                        name: translate.instant("GENERAL.AUTOMATIC"),
                        value: Mode.AUTOMATIC,
                        icon: { color: "danger", name: "power-outline", size: "medium" },
                    },
                ],
            },
            {
                type: "horizontal-line",
            },
        );

        return lines;
    };

    export function getChannelAddresses(
        service: Service,
        route: ActivatedRoute,
        component: EdgeConfig.Component | null = null,
    ): Promise<ChannelAddress[]> {
        const edge = service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);

        const heatpumpComponent = component ?? config.getComponentSafely(route.snapshot.params.componentId);

        AssertionUtils.assertIsDefined(heatpumpComponent);

        const channelAddresses: ChannelAddress[] = [
            new ChannelAddress(heatpumpComponent.id, PROPERTY_MODE),
            new ChannelAddress(heatpumpComponent.id, "_PropertyAutomaticRecommendationCtrlEnabled"),
            new ChannelAddress(heatpumpComponent.id, "_PropertyAutomaticForceOnCtrlEnabled"),
            new ChannelAddress(heatpumpComponent.id, "_PropertyManualState"),
            new ChannelAddress(heatpumpComponent.id, "_PropertyAutomaticRecommendationSurplusPower"),
            new ChannelAddress(heatpumpComponent.id, "_PropertyAutomaticForceOnSurplusPower"),
            new ChannelAddress(heatpumpComponent.id, "_PropertyAutomaticForceOnSoc"),
            new ChannelAddress(heatpumpComponent.id, "_PropertyAutomaticLockCtrlEnabled"),
            new ChannelAddress(heatpumpComponent.id, "_PropertyAutomaticLockGridBuyPower"),
            new ChannelAddress(heatpumpComponent.id, "_PropertyAutomaticLockSoc"),
            new ChannelAddress(heatpumpComponent.id, "_PropertyMinimumSwitchingTime"),
        ];

        const consumptionMeter = config.getComponentFromOtherComponentsProperty(heatpumpComponent.id, "meter.id");
        if (consumptionMeter) {
            channelAddresses.push(new ChannelAddress(consumptionMeter.id, "ActivePower"));
        }

        return Promise.resolve(channelAddresses);
    }

    export function getFormGroup(): FormGroup {
        return new FormGroup({
            mode: new FormControl(null),
            manualState: new FormControl(null),
            baseMode: new FormControl(null),
            automaticRecommendationCtrlEnabled: new FormControl(null),
            automaticForceOnCtrlEnabled: new FormControl(null),
            automaticForceOnSurplusPower: new FormControl(null),
            automaticRecommendationSurplusPower: new FormControl(null),
            automaticForceOnSoc: new FormControl(null),
            automaticLockCtrlEnabled: new FormControl(null),
            automaticLockGridBuyPower: new FormControl(null),
            automaticLockSoc: new FormControl(null),
            minimumSwitchingTime: new FormControl(null),
        });
    }

    export function getNavigationTree(
        translate: TranslateService,
        component: EdgeConfig.Component,
        edge: Edge,
    ): ConstructorParameters<typeof NavigationTree> {
        const children: NavigationTree[] = [
            new NavigationTree(
                "history",
                { baseString: "history" },
                { name: "stats-chart-outline", color: "warning" },
                translate.instant("GENERAL.HISTORY"),
                "label",
                [],
                null,
            ),
        ];

        if (EdgePermission.isHeatpumpTimeScheduleAndBaseModeAvailable(edge)) {
            children.push(
                new NavigationTree(
                    "schedule",
                    { baseString: "schedule" },
                    { name: "calendar-outline", color: "warning" },
                    translate.instant("EDGE.INDEX.WIDGETS.EVSE.SCHEDULE.SCHEDULE"),
                    "label",
                    [
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
                    ],
                    null,
                ),
                new NavigationTree(
                    "baseMode",
                    { baseString: "baseMode" },
                    { name: "repeat-outline", color: "production" },
                    translate.instant("GENERAL.BASE_MODE"),
                    "label",
                    [],
                    null,
                ),
            );
        }

        children.push(
            NavigationConstants.CommonNodes.SETTINGS(translate),
            NavigationConstants.CommonNodes.INFO(translate, { source: component.id }),
        );

        return new NavigationTree(
            component.id,
            { baseString: "controller/heatpump/" + component.id },
            { name: "oe-heatpump", color: "normal" },
            Name.METER_ALIAS_OR_ID(component),
            "label",
            children,
            null,
        ).toConstructorParams();
    }

    export function getHeatPumpStates(translate: TranslateService): string {
        return `
            1.${translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.LOCK")}
            2.${translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.NORMAL_OPERATION")}
            3.${translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.SWITCH_ON_REC")}
            4.${translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.SWITCH_ON_COM")}
            `;
    }

    export function getConsumptionMeter(
        config: EdgeConfig | null,
        heatpump: EdgeConfig.Component,
    ): EdgeConfig.Component | null {
        AssertionUtils.assertIsDefined(config);
        return config.getComponentFromOtherComponentsProperty(heatpump.id, "meter.id");
    }

    function getManualOptions(
        translate: TranslateService,
    ): { value: ManualState; name: string; description: string }[] {
        return [
            {
                name: translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.SWITCH_ON_COM"),
                value: ManualState.FORCE_ON,
                description: translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.BASE_MODE_DESCRIPTIONS.SWITCH_ON_COM"),
            },
            {
                name: translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.SWITCH_ON_REC"),
                value: ManualState.RECOMMENDATION,
                description: translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.BASE_MODE_DESCRIPTIONS.SWITCH_ON_REC"),
            },
            {
                name: translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.NORMAL_OPERATION"),
                value: ManualState.REGULAR,
                description: translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.BASE_MODE_DESCRIPTIONS.NORMAL_OPERATION"),
            },
            {
                name: translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.LOCK"),
                value: ManualState.LOCK,
                description: translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.BASE_MODE_DESCRIPTIONS.LOCK"),
            },
        ];
    }

    function getStateRadioButtons(translate: TranslateService): ButtonLabel[] {
        return [
            {
                name: translate.instant("GENERAL.AUTOMATIC"),
                value: BaseMode.AUTOMATIC,
                description: translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.BASE_MODE_DESCRIPTIONS.AUTOMATIC"),
            },
            ...getManualOptions(translate).map((option) => ({
                name: option.name,
                value: option.value,
                description: option.description,
            })),
        ];
    }
}

export const CONVERT_TO_BASE_MODE_LABEL = (translate: TranslateService) => {
    return (value: BaseMode | null): string => {
        switch (value) {
            case BaseMode.AUTOMATIC:
                return translate.instant("GENERAL.AUTOMATIC");
            case BaseMode.FORCE_ON:
                return translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.SWITCH_ON_COM");
            case BaseMode.RECOMMENDATION:
                return translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.SWITCH_ON_REC");
            case BaseMode.REGULAR:
                return translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.NORMAL_OPERATION");
            case BaseMode.LOCK:
                return translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.LOCK");
            default:
                return Converter.HIDE_VALUE(value);
        }
    };
};

export enum HeatpumpMode {
    MANUAL = "MANUAL",
    AUTOMATIC = "AUTOMATIC",
    TIME_SCHEDULE = "TIME_SCHEDULE",
}

export enum ManualState {
    FORCE_ON = "FORCE_ON",
    RECOMMENDATION = "RECOMMENDATION",
    REGULAR = "REGULAR",
    LOCK = "LOCK",
}

export enum BaseMode {
    AUTOMATIC = "AUTOMATIC",
    FORCE_ON = "FORCE_ON",
    RECOMMENDATION = "RECOMMENDATION",
    REGULAR = "REGULAR",
    LOCK = "LOCK",
}
