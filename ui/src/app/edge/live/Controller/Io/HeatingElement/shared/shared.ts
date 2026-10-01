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
};
