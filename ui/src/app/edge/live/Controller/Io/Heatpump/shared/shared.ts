import { FormControl, FormGroup } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { NavigationConstants, NavigationTree } from "src/app/shared/components/navigation/shared";
import { Converter } from "src/app/shared/components/shared/converter";
import { Name } from "src/app/shared/components/shared/name";
import { OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, Edge, EdgeConfig, Service } from "src/app/shared/shared";
import { Mode } from "src/app/shared/type/general";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";

export namespace SharedControllerIoHeatpump {

    const PROPERTY_MODE: string = "_PropertyMode";
    // hide manual elements when mode is AUTOMATIC
    const HIDE_ON_MODE_AUTOMATIC = (el: { mode: Mode }) => el.mode === Mode.AUTOMATIC;
    // hide automatic elements when mode is manual
    const HIDE_ON_MODE_MANUAL = (el: { mode: Mode }) => el.mode === Mode.MANUAL;

    export const getFormlyView = (translate: TranslateService, component: EdgeConfig.Component, edge: Edge): OeFormlyView<{ mode: Mode }> => {

        return {
            title: component.alias,
            helpKey: "REDIRECT.CONTROLLER_IO_HEAT_PUMP_SG_READY",
            lines: [
                ...getFormlySharedLines(translate, component),
                ...getFormlyAutomaticView(translate, component, HIDE_ON_MODE_MANUAL),
                ...getFormlyManualView(translate, HIDE_ON_MODE_AUTOMATIC),
            ],
            component: component,
            edge: edge,
        };
    };

    const getFormlyAutomaticView = (
        translate: TranslateService,
        component: EdgeConfig.Component,
        hideCondition: (field: { mode: Mode }) => boolean
    ): OeFormlyView<{ mode: Mode }>["lines"] => {

        const lines: OeFormlyView<{ mode: Mode }>["lines"] = [
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
                type: "channel-line",
                channel: new ChannelAddress(component.id, "_PropertyAutomaticForceOnSoc").toString(),
                name: translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.ABOVE_SOC"),
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
                type: "channel-line",
                channel: new ChannelAddress(component.id, "_PropertyAutomaticLockSoc").toString(),
                name: translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.BELOW_SOC"),
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

        return lines.map(line => ({
            ...line,
            hide: hideCondition,
        }));
    };

    const getFormlyManualView = (translate: TranslateService, hideCondition: (field: { mode: Mode }) => boolean): OeFormlyView<{ mode: Mode }>["lines"] => ([
        {
            type: "select-line",
            controlName: "manualState",
            name: translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.OPERATING_STATUS"),
            options: getManualOptions(translate),
            hide: hideCondition,
        },
    ]);

    const getFormlySharedLines = (translate: TranslateService, component: EdgeConfig.Component): OeFormlyView["lines"] => ([{
        type: "channel-line",
        name: translate.instant("GENERAL.STATE"),
        channel: component.id + "/Status",
        converter: Converter.HEAT_PUMP_STATES(translate),
    }, {
        type: "channel-line",
        name: translate.instant("GENERAL.MODE"),
        channel: component.id + "/" + PROPERTY_MODE,
        converter: Converter.CONTROLLER_PROPERTY_MODES(translate),
    }, {
        type: "horizontal-line",
    },
    {
        type: "buttons-from-form-control-line",
        name: translate.instant("GENERAL.MODE"),
        controlName: "mode",
        buttons: [
            {
                name: translate.instant("GENERAL.MANUALLY"),
                value: "MANUAL",
                icon: { color: "success", name: "options-outline", size: "medium" },
            },
            {
                name: translate.instant("GENERAL.AUTOMATIC"),
                value: "AUTOMATIC",
                icon: { color: "danger", name: "power-outline", size: "medium" },
            },
        ],
    }, {
        type: "horizontal-line",
    }]);

    export function getChannelAddresses(service: Service, route: ActivatedRoute, component: EdgeConfig.Component | null = null): Promise<ChannelAddress[]> {
        const edge = service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);

        const heatpumpComponent = component ?? config.getComponentSafely(route.snapshot.params.componentId);

        AssertionUtils.assertIsDefined(heatpumpComponent);
        return Promise.resolve([
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
        ]);
    }

    export function getFormGroup(): FormGroup {

        return new FormGroup({
            mode: new FormControl(null),
            manualState: new FormControl(null),
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

    export function getNavigationTree(translate: TranslateService, component: EdgeConfig.Component): ConstructorParameters<typeof NavigationTree> {
        return new NavigationTree(component.id, { baseString: "controller/heatpump/" + component.id }, { name: "oe-heatpump", color: "normal" }, Name.METER_ALIAS_OR_ID(component), "label", [
            new NavigationTree("history", { baseString: "history" }, { name: "stats-chart-outline", color: "warning" }, translate.instant("GENERAL.HISTORY"), "label", [], null),
            NavigationConstants.CommonNodes.SETTINGS(translate),
        ], null).toConstructorParams();
    }

    export function getHeatPumpStates(translate: TranslateService): string {
        return `
            1.${translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.LOCK")}
            2.${translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.NORMAL_OPERATION")}
            3.${translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.SWITCH_ON_REC")}
            4.${translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.SWITCH_ON_COM")}
            `;
    }

    function getManualOptions(translate: TranslateService): { value: string; name: string; }[] {
        return [
            { name: translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.SWITCH_ON_COM"), value: "FORCE_ON" },
            { name: translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.SWITCH_ON_REC"), value: "RECOMMENDATION" },
            { name: translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.NORMAL_OPERATION"), value: "REGULAR" },
            { name: translate.instant("EDGE.INDEX.WIDGETS.HEAT_PUMP.LOCK"), value: "LOCK" },
        ];
    }
}

