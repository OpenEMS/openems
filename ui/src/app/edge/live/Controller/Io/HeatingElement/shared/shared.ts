import { FormControl, FormGroup } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { NavigationConstants, NavigationTree } from "src/app/shared/components/navigation/shared";
import { Converter } from "src/app/shared/components/shared/converter";
import { Formatter } from "src/app/shared/components/shared/formatter";
import { Name } from "src/app/shared/components/shared/name";
import { OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, CurrentData, Edge, EdgeConfig } from "src/app/shared/shared";
import { Mode, WorkMode } from "src/app/shared/type/general";
import { NumberUtils } from "src/app/shared/utils/number/number-utils";

export namespace SharedControllerIoHeatingElement {
    export const PREDICTED_PV_PRODUCTION_HOUR = 5;
    export const POWER_OVERSHOOT_FACTOR = 1.1;
    // hide elements when mode is not off
    const HIDE_ON_MODE_NOT_MANUAL_ON = (el: { mode: Mode }) => el.mode !== Mode.MANUAL_ON;
    // hide elements when mode is not off
    const HIDE_ON_WORKMODE_NOT_TIME_OR_MODE_NOT_TIME = (el: { mode: Mode; workMode: WorkMode }) =>
        el.mode !== Mode.AUTOMATIC || el.workMode === WorkMode.NONE || el.workMode === WorkMode.ENERGY;
    const HIDE_ON_WORKMODE_NOT_ENERGY_OR_MODE_NOT_TIME = (el: { mode: Mode; workMode: WorkMode }) =>
        el.mode !== Mode.AUTOMATIC || el.workMode === WorkMode.NONE || el.workMode === WorkMode.TIME;

    export const getFormlyView = (
        translate: TranslateService,
        component: EdgeConfig.Component,
        edge: Edge,
    ): OeFormlyView<HeatingElementViewModel> => {
        return {
            title: component.alias,
            helpKey: "REDIRECT.CONTROLLER_IO_HEATING_ELEMENT",
            icon: { name: "oe-heating-element", color: "normal", size: "large" },
            lines: [
                ...getFormlySharedLines(translate, component),
                ...getFormlyAutomaticView(
                    translate,
                    HIDE_ON_WORKMODE_NOT_TIME_OR_MODE_NOT_TIME,
                    HIDE_ON_WORKMODE_NOT_ENERGY_OR_MODE_NOT_TIME,
                ),
                ...getFormlyManualOnView(HIDE_ON_MODE_NOT_MANUAL_ON),
                ...getFormlyTemperatureView(translate, component, edge),
            ],
            component: component,
            edge: edge,
        };
    };

    const getFormlyAutomaticView = (
        translate: TranslateService,
        HIDE_ON_WORKMODE_NOT_TIME_OR_MODE_NOT_TIME: (field: { mode: Mode; workMode: WorkMode }) => boolean,
        HIDE_ON_WORKMODE_NOT_ENERGY_OR_MODE_NOT_TIME: (field: { mode: Mode; workMode: WorkMode }) => boolean,
    ): OeFormlyView<HeatingElementViewModel>["lines"] => {
        const lines: OeFormlyView<HeatingElementViewModel>["lines"] = [];

        lines.push({
            type: "select-line",
            controlName: "workMode",
            name: translate.instant("EDGE.INDEX.WIDGETS.HEATINGELEMENT.GUARANTEE_MODE"),
            options: [
                {
                    name: translate.instant("EDGE.INDEX.WIDGETS.HEATINGELEMENT.MINIMUM_DURATION"),
                    value: WorkMode.TIME,
                },
                {
                    name: translate.instant("EDGE.INDEX.WIDGETS.HEATINGELEMENT.MINIMUM_ENERGY"),
                    value: WorkMode.ENERGY,
                },
                { name: translate.instant("EDGE.INDEX.WIDGETS.HEATINGELEMENT.GUARANTEE_NONE"), value: WorkMode.NONE },
            ],
            hide: (el: { mode: Mode }) => el.mode !== Mode.AUTOMATIC,
        });

        lines.push(
            {
                type: "horizontal-line",
                hide: HIDE_ON_WORKMODE_NOT_ENERGY_OR_MODE_NOT_TIME,
            },
            {
                type: "value-from-form-control-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.HEATINGELEMENT.MINIMAL_ENERGY_LIMIT"),
                controlName: "minEnergyLimitInKwh",
                converter: Converter.TO_KILO_WATT_HOURS,
                hide: HIDE_ON_WORKMODE_NOT_ENERGY_OR_MODE_NOT_TIME,
            },
            {
                type: "range-button-from-form-control-line",
                controlName: "minEnergyLimitInKwh",
                properties: {
                    tickMaxControlName: "maxPower",
                    tickMax: 10,
                    tickMin: 0,
                    unit: "kWh",
                    keepValue: true,
                    pinFormatter: (value: number) => Formatter.FORMAT_KILO_WATT_HOURS(value),
                    snaps: true,
                },
                hide: HIDE_ON_WORKMODE_NOT_ENERGY_OR_MODE_NOT_TIME,
            },
            {
                type: "time-line",
                controlName: "endTimeWithMeter",
                name: translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.END_TIME"),
                hide: HIDE_ON_WORKMODE_NOT_ENERGY_OR_MODE_NOT_TIME,
            },
            {
                type: "info-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.HEATINGELEMENT.MESSAGE_UNREACHABLE"),
                hide: (el: { mode: Mode; workMode: WorkMode; isUnreachable: boolean }) =>
                    el.mode !== Mode.AUTOMATIC ||
                    el.workMode === WorkMode.NONE ||
                    el.workMode === WorkMode.TIME ||
                    !el.isUnreachable,
            },
        );

        lines.push(
            {
                type: "horizontal-line",
            },
            {
                type: "select-line",
                controlName: "defaultLevel",
                name: "Level",
                options: [
                    { name: translate.instant("EDGE.INDEX.WIDGETS.HEATINGELEMENT.LEVEL_ONE"), value: "LEVEL_1" },
                    { name: translate.instant("EDGE.INDEX.WIDGETS.HEATINGELEMENT.LEVEL_TWO"), value: "LEVEL_2" },
                    { name: translate.instant("EDGE.INDEX.WIDGETS.HEATINGELEMENT.LEVEL_THREE"), value: "LEVEL_3" },
                ],
            },
            {
                type: "horizontal-line",
            },
            {
                type: "info-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.END_TIME"),
            },
            {
                type: "time-line",
                controlName: "endTime",
                name: translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.END_TIME"),
            },
            {
                type: "horizontal-line",
            },
            {
                type: "value-from-form-control-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.HEATINGELEMENT.MINIMUM_RUN_TIME"),
                controlName: "minTime",
            },
            {
                type: "range-button-from-form-control-line",
                controlName: "minTime",
                properties: {
                    tickMax: 10,
                    tickMin: 0,
                    pinFormatter: (value: number) => Formatter.FORMAT_HOUR(value),
                    snaps: true,
                },
            },
        );

        return lines.map((line) => ({
            ...line,
            hide: line.hide ?? HIDE_ON_WORKMODE_NOT_TIME_OR_MODE_NOT_TIME,
        }));
    };

    const getFormlyManualOnView = (
        hideCondition: (field: { mode: Mode }) => boolean,
    ): OeFormlyView<HeatingElementViewModel>["lines"] => [
        {
            type: "select-line",
            controlName: "defaultLevel",
            name: "Level",
            options: [
                { name: "Level 1", value: "LEVEL_1" },
                { name: "Level 2", value: "LEVEL_2" },
                { name: "Level 3", value: "LEVEL_3" },
            ],
            hide: hideCondition,
        },
    ];

    const HIDE_ON_NO_THERMOMETER = (el: HeatingElementViewModel) => !el.hasWaterThermometer;
    const HIDE_ON_NO_LEGIONELLA = (el: HeatingElementViewModel) =>
        !el.hasWaterThermometer || !el.enableLegionellenProtection;

    const getLevelOptions = (translate: TranslateService, withLevelZero: boolean) => [
        ...(withLevelZero
            ? [{ name: translate.instant("EDGE.INDEX.WIDGETS.HEATINGELEMENT.LEVEL_ZERO"), value: "LEVEL_0" }]
            : []),
        { name: translate.instant("EDGE.INDEX.WIDGETS.HEATINGELEMENT.LEVEL_ONE"), value: "LEVEL_1" },
        { name: translate.instant("EDGE.INDEX.WIDGETS.HEATINGELEMENT.LEVEL_TWO"), value: "LEVEL_2" },
        { name: translate.instant("EDGE.INDEX.WIDGETS.HEATINGELEMENT.LEVEL_THREE"), value: "LEVEL_3" },
    ];

    /**
     * Creates the lines for the optional water temperature control.
     *
     * @param translate The {@link TranslateService}
     * @param component The heating element component
     * @param edge The current {@link Edge}
     * @returns The formly lines
     */
    export const getFormlyTemperatureView = (
        translate: TranslateService,
        component: EdgeConfig.Component,
        edge: Edge,
    ): OeFormlyView<HeatingElementViewModel>["lines"] => {
        const T = "EDGE.INDEX.WIDGETS.HEATINGELEMENT.";
        const thermometers = (
            edge.getCurrentConfig()?.getComponentsImplementingNature("io.openems.edge.thermometer.api.Thermometer") ??
            []
        ).map((c) => ({ name: c.alias ? c.alias + " (" + c.id + ")" : c.id, value: c.id }));
        const temperatureInput = (
            controlName: string,
            name: string,
            hide: (el: HeatingElementViewModel) => boolean = HIDE_ON_NO_THERMOMETER,
        ) => ({
            type: "input-line" as const,
            name: translate.instant(T + name),
            controlName: controlName,
            properties: { unit: "°C", type: "number" as const },
            hide: hide,
        });

        return [
            { type: "horizontal-line" },
            {
                type: "toggle-line",
                name: translate.instant(T + "HAS_WATER_THERMOMETER"),
                controlName: "hasWaterThermometer",
            },
            {
                type: "value-from-channels-line",
                name: translate.instant(T + "WATER_ACTUAL"),
                channelsToSubscribe: [new ChannelAddress(component.id, "WaterActual")],
                value: (currentData: CurrentData) =>
                    Converter.TEMPERATURE_IN_DEGREES(currentData.allComponents[component.id + "/WaterActual"]),
                hide: HIDE_ON_NO_THERMOMETER,
            },
            {
                type: "value-from-channels-line",
                name: translate.instant(T + "WATER_TARGET"),
                channelsToSubscribe: [new ChannelAddress(component.id, "WaterTarget")],
                value: (currentData: CurrentData) =>
                    Converter.TEMPERATURE_IN_DEGREES(currentData.allComponents[component.id + "/WaterTarget"]),
                hide: HIDE_ON_NO_THERMOMETER,
            },
            {
                type: "value-from-channels-line",
                name: translate.instant(T + "SENSOR_STATE"),
                channelsToSubscribe: [new ChannelAddress(component.id, "WaterThermometerNotAvailable")],
                value: (currentData: CurrentData) =>
                    currentData.allComponents[component.id + "/WaterThermometerNotAvailable"] === 1
                        ? translate.instant(T + "SENSOR_NOT_AVAILABLE")
                        : translate.instant("GENERAL.OK"),
                hide: HIDE_ON_NO_THERMOMETER,
            },
            {
                type: "value-from-channels-line",
                name: translate.instant(T + "LEGIONELLA_STATE"),
                channelsToSubscribe: [new ChannelAddress(component.id, "LegionellenProtectionFailed")],
                value: (currentData: CurrentData) =>
                    currentData.allComponents[component.id + "/LegionellenProtectionFailed"] === 1
                        ? translate.instant(T + "LEGIONELLA_FAILED")
                        : translate.instant("GENERAL.OK"),
                hide: HIDE_ON_NO_LEGIONELLA,
            },
            { type: "horizontal-line", hide: HIDE_ON_NO_THERMOMETER },
            {
                type: "select-line",
                name: translate.instant(T + "WATER_THERMOMETER"),
                controlName: "waterThermometerId",
                options: thermometers,
                hide: HIDE_ON_NO_THERMOMETER,
            },
            {
                type: "select-line",
                name: translate.instant(T + "SENSOR_ERROR_LEVEL"),
                controlName: "sensorErrorLevel",
                options: getLevelOptions(translate, true),
                hide: HIDE_ON_NO_THERMOMETER,
            },
            temperatureInput("lowWaterTemperature", "LOW_WATER_TEMPERATURE"),
            temperatureInput("highWaterTemperature", "HIGH_WATER_TEMPERATURE"),
            temperatureInput("maxWaterTemperature", "MAX_WATER_TEMPERATURE"),
            { type: "horizontal-line", hide: HIDE_ON_NO_THERMOMETER },
            {
                type: "toggle-line",
                name: translate.instant(T + "LEGIONELLA_PROTECTION"),
                controlName: "enableLegionellenProtection",
                hide: HIDE_ON_NO_THERMOMETER,
            },
            temperatureInput("legionellenWaterTemperature", "LEGIONELLA_TEMPERATURE", HIDE_ON_NO_LEGIONELLA),
            {
                type: "input-line",
                name: translate.instant(T + "LEGIONELLA_INTERVAL"),
                controlName: "legionellenInterval",
                properties: { unit: translate.instant(T + "UNIT_DAYS"), type: "number" },
                hide: HIDE_ON_NO_LEGIONELLA,
            },
            {
                type: "input-line",
                name: translate.instant(T + "LEGIONELLA_MIN_TIME"),
                controlName: "minLegionellenTime",
                properties: { unit: "h", type: "number" },
                hide: HIDE_ON_NO_LEGIONELLA,
            },
            {
                type: "input-line",
                name: translate.instant(T + "LEGIONELLA_MAX_HEATING_TIME"),
                controlName: "legionellenMaxHeatingTime",
                properties: { unit: "h", type: "number" },
                hide: HIDE_ON_NO_LEGIONELLA,
            },
        ];
    };

    const getFormlySharedLines = (
        translate: TranslateService,
        component: EdgeConfig.Component,
    ): OeFormlyView<HeatingElementViewModel>["lines"] => [
        {
            type: "value-from-channels-line",
            name: translate.instant("GENERAL.STATE"),
            channelsToSubscribe: [new ChannelAddress(component.id, "Status")],
            value: (currentData: CurrentData) => {
                const runState = currentData.allComponents[component.id + "/Status"];
                return Converter.CONVERT_HEATING_ELEMENT_RUNSTATE(translate)(runState);
            },
        },
        {
            type: "buttons-from-form-control-line",
            name: translate.instant("GENERAL.MODE"),
            controlName: "mode",
            buttons: [
                {
                    name: translate.instant("GENERAL.ON"),
                    value: Mode.MANUAL_ON,
                    icon: { color: "success", name: "play-outline", size: "medium" },
                },
                {
                    name: translate.instant("GENERAL.AUTOMATIC"),
                    value: Mode.AUTOMATIC,
                    icon: { color: "primary", name: "sunny", size: "medium" },
                },
                {
                    name: translate.instant("GENERAL.OFF"),
                    value: Mode.MANUAL_OFF,
                    icon: { color: "danger", name: "power-outline", size: "medium" },
                },
            ],
        },
        {
            type: "horizontal-line",
        },
    ];

    /** Config properties of the temperature control: form control name, channel and OSGi property name. */
    export const TEMPERATURE_PROPERTIES: { control: string; channel: string; property: string; boolean?: true }[] = [
        {
            control: "hasWaterThermometer",
            channel: "_PropertyHasWaterThermometer",
            property: "hasWaterThermometer",
            boolean: true,
        },
        { control: "waterThermometerId", channel: "_PropertyWaterThermometerId", property: "waterThermometer.id" },
        { control: "sensorErrorLevel", channel: "_PropertySensorErrorLevel", property: "sensorErrorLevel" },
        { control: "lowWaterTemperature", channel: "_PropertyLowWaterTemperature", property: "lowWaterTemperature" },
        { control: "highWaterTemperature", channel: "_PropertyHighWaterTemperature", property: "highWaterTemperature" },
        { control: "maxWaterTemperature", channel: "_PropertyMaxWaterTemperature", property: "maxWaterTemperature" },
        {
            control: "enableLegionellenProtection",
            channel: "_PropertyEnableLegionellenProtection",
            property: "enableLegionellenProtection",
            boolean: true,
        },
        {
            control: "legionellenWaterTemperature",
            channel: "_PropertyLegionellenWaterTemperature",
            property: "legionellenWaterTemperature",
        },
        { control: "legionellenInterval", channel: "_PropertyLegionellenInterval", property: "legionellenInterval" },
        { control: "minLegionellenTime", channel: "_PropertyMinLegionellenTime", property: "minLegionellenTime" },
        {
            control: "legionellenMaxHeatingTime",
            channel: "_PropertyLegionellenMaxHeatingTime",
            property: "legionellenMaxHeatingTime",
        },
    ];

    export function getChannelAddresses(component: EdgeConfig.Component): Promise<ChannelAddress[]> {
        return Promise.resolve([
            new ChannelAddress(component.id, "_PropertyMode"),
            new ChannelAddress(component.id, "_PropertyDefaultLevel"),
            new ChannelAddress(component.id, "_PropertyWorkMode"),
            new ChannelAddress(component.id, "_PropertyMinTime"),
            new ChannelAddress(component.id, "_PropertyEndTime"),
            new ChannelAddress(component.id, "_PropertyMinEnergyLimitInKwh"),
            new ChannelAddress(component.id, "Phase1AvgPower"),
            new ChannelAddress(component.id, "Phase2AvgPower"),
            new ChannelAddress(component.id, "Phase3AvgPower"),
            new ChannelAddress(component.id, "SessionEnergy"),
            new ChannelAddress(component.id, "WaterActual"),
            new ChannelAddress(component.id, "WaterTarget"),
            new ChannelAddress(component.id, "WaterThermometerNotAvailable"),
            new ChannelAddress(component.id, "LegionellenProtectionFailed"),
            ...TEMPERATURE_PROPERTIES.map((p) => new ChannelAddress(component.id, p.channel)),
        ]);
    }

    export function getFormGroup(component: EdgeConfig.Component): FormGroup {
        return new FormGroup({
            mode: new FormControl(null),
            defaultLevel: new FormControl(null),
            minTime: new FormControl(null),
            workMode: new FormControl(null),
            endTime: new FormControl(null),
            minEnergyLimitInKwh: new FormControl(NumberUtils.divideSafely(component.properties.minEnergylimit, 1000)),
            maxPower: new FormControl(null),
            endTimeWithMeter: new FormControl(component.properties.endTimeWithMeter),
            isUnreachable: new FormControl(null),
            hasWaterThermometer: new FormControl(component.properties.hasWaterThermometer ?? false),
            waterThermometerId: new FormControl(component.properties["waterThermometer.id"] ?? null),
            sensorErrorLevel: new FormControl(component.properties.sensorErrorLevel ?? null),
            lowWaterTemperature: new FormControl(component.properties.lowWaterTemperature ?? null),
            highWaterTemperature: new FormControl(component.properties.highWaterTemperature ?? null),
            maxWaterTemperature: new FormControl(component.properties.maxWaterTemperature ?? null),
            enableLegionellenProtection: new FormControl(component.properties.enableLegionellenProtection ?? false),
            legionellenWaterTemperature: new FormControl(component.properties.legionellenWaterTemperature ?? null),
            legionellenInterval: new FormControl(component.properties.legionellenInterval ?? null),
            minLegionellenTime: new FormControl(component.properties.minLegionellenTime ?? null),
            legionellenMaxHeatingTime: new FormControl(component.properties.legionellenMaxHeatingTime ?? null),
        });
    }

    export function getNavigationTree(
        translate: TranslateService,
        component: EdgeConfig.Component,
    ): ConstructorParameters<typeof NavigationTree> {
        return new NavigationTree(
            component.id,
            { baseString: "controller/heatingelement/" + component.id },
            { name: "oe-heating-element", color: "normal" },
            Name.METER_ALIAS_OR_ID(component),
            "label",
            [
                NavigationConstants.CommonNodes.HISTORY(translate, component.id),
                NavigationConstants.CommonNodes.SETTINGS(translate, component.id),
            ],
            null,
        ).toConstructorParams();
    }
}

export type HeatingElementViewModel = {
    mode: Mode;
    workMode: WorkMode;
    isUnreachable: boolean;
    hasWaterThermometer: boolean;
    enableLegionellenProtection: boolean;
};
