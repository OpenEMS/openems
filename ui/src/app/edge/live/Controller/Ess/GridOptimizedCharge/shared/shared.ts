import { Type } from "@angular/core";
import { FormControl, FormGroup } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { NavigationConstants, NavigationTree } from "src/app/shared/components/navigation/shared";
import { Converter } from "src/app/shared/components/shared/converter";
import { Filter } from "src/app/shared/components/shared/filter";
import { Name } from "src/app/shared/components/shared/name";
import { OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { hasMaximumGridFeedInLimitInMeta } from "src/app/shared/permissions/edgePermissions";
import { RouteService } from "src/app/shared/service/route.service";
import { ChannelAddress, CurrentData, Edge, EdgeConfig, Service, Utils } from "src/app/shared/shared";
import { Mode } from "src/app/shared/type/general";
import { Language } from "src/app/shared/type/language";
import { Role } from "src/app/shared/type/role";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { DelayChargeState } from "../modal/modal";
import { NewNavigationPredictionChartComponent } from "../shared/prediction-chart";

export namespace SharedGridOptimizedCharge {
    // hide elements when mode is off
    const HIDE_ON_MODE_OFF = (el: { mode: Mode }) => el.mode === Mode.OFF;
    // hide elements when mode is not off
    const HIDE_ON_MODE_NOT_OFF = (el: { mode: Mode }) => el.mode !== Mode.OFF;
    // hide elements when mode is not off
    const HIDE_ON_MODE_NOT_AUTOMATIC = (el: { mode: Mode }) => el.mode !== Mode.AUTOMATIC;
    // hide elements when mode is not off
    const HIDE_ON_MODE_NOT_MANUAL = (el: { mode: Mode }) => el.mode !== Mode.MANUAL;

    export const getFormlyView = (
        translate: TranslateService,
        component: EdgeConfig.Component,
        edge: Edge,
        targetEpochSeconds: number | null,
        chargeStartEpochSeconds: number | null,
        predictionChartComponent: Type<NewNavigationPredictionChartComponent>
    ): OeFormlyView<GridOptimizedChargeViewModel> => {
        return {
            title: component.alias,
            helpKey: "REDIRECT.CONTROLLER_ESS_GRID_OPTIMIZED_CHARGE",
            icon: { name: "oe-grid-storage", color: "dark", size: "large" },
            lines: [
                ...getFormlySharedLines(translate, component, edge, HIDE_ON_MODE_OFF),
                ...getFormlyOffView(translate, HIDE_ON_MODE_NOT_OFF),
                ...getFormlyAutomaticView(translate, component, edge, targetEpochSeconds, chargeStartEpochSeconds, predictionChartComponent, HIDE_ON_MODE_NOT_AUTOMATIC),
                ...getFormlyManualView(translate, HIDE_ON_MODE_NOT_MANUAL),
            ],
            component: component,
            edge: edge,
        };
    };

    const getFormlyOffView = (
        translate: TranslateService,
        hideCondition: (field: { mode: Mode }) => boolean
    ): OeFormlyView<GridOptimizedChargeViewModel>["lines"] => ([
        {
            type: "info-line",
            name: translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.GRID_OPTIMIZED_CHARGE_DISABLED"),
            hide: hideCondition,
        },
    ]);

    const getFormlyAutomaticView = (
        translate: TranslateService,
        component: EdgeConfig.Component,
        edge: Edge,
        targetEpochSeconds: number | null,
        chargeStartEpochSeconds: number | null,
        predictionChartComponent: Type<NewNavigationPredictionChartComponent>,
        hideCondition: (field: { mode: Mode }) => boolean,
    ): OeFormlyView<GridOptimizedChargeViewModel>["lines"] => {
        const lines: OeFormlyView<GridOptimizedChargeViewModel>["lines"] = [];

        lines.push(
            {
                type: "value-from-channels-line",
                name: "",
                singleLine: true,
                channelsToSubscribe: [
                    new ChannelAddress(component.id, "DelayChargeMaximumChargeLimit",),
                    new ChannelAddress(component.id, "TargetMinute",),
                ],
                value: (data: CurrentData) => {
                    const delayChargeMaximumChargeLimit = data.allComponents[component.id + "/DelayChargeMaximumChargeLimit"];
                    const targetMinute = data.allComponents[component.id + "/TargetMinute"];
                    const chargingEndTime = targetMinute !== null ? Converter.CONVERT_MINUTE_TO_TIME_OF_DAY(translate, Language.geti18nLocale())(targetMinute) : null;

                    if (targetMinute !== null && delayChargeMaximumChargeLimit !== null) {
                        return translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.END_TIME_DETAILED_DESCRIPTION", {
                            maxChargingPowerW: delayChargeMaximumChargeLimit,
                            endTime: chargingEndTime,
                        });
                    }
                    return translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.END_TIME_DESCRIPTION");
                },
            },
            {
                type: "horizontal-line",
            },
            {
                type: "component-line",
                component: predictionChartComponent,
                inputs: {
                    component: component,
                    edge: edge,
                    refresh: true,
                    targetEpochSeconds: targetEpochSeconds,
                    chargeStartEpochSeconds: chargeStartEpochSeconds,
                },
            },
            {
                type: "info-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.RISK_PROPENSITY"),
            },
            {
                type: "buttons-from-form-control-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.RISK_PROPENSITY"),
                controlName: "delayChargeRiskLevel",
                buttons: [
                    {
                        name: translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.LOW"),
                        value: RiskLevel.LOW,
                    },
                    {
                        name: translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.MEDIUM"),
                        value: RiskLevel.MEDIUM,
                    },
                    {
                        name: translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.HIGH"),
                        value: RiskLevel.HIGH,
                    },
                ],
            }
        );

        Object.values(RiskLevel).forEach(delayChargeRiskLevel => {
            lines.push(
                {
                    type: "info-line",
                    name: translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.RISK_DESCRIPTION." + delayChargeRiskLevel + ".FUNCTION_DESCRIPTION"),
                    hide: (el: { delayChargeRiskLevel: RiskLevel, mode: Mode }) => el.delayChargeRiskLevel !== delayChargeRiskLevel || el.mode !== Mode.AUTOMATIC,
                },
                {
                    type: "info-line",
                    name: translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.RISK_DESCRIPTION." + delayChargeRiskLevel + ".STORAGE_DESCRIPTION"),
                    icon: { name: "arrow-up-outline", color: "primary", size: "medium" },
                    hide: (el: { delayChargeRiskLevel: RiskLevel, mode: Mode }) => el.delayChargeRiskLevel !== delayChargeRiskLevel || el.mode !== Mode.AUTOMATIC,
                },
                {
                    type: "info-line",
                    name: translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.RISK_DESCRIPTION." + delayChargeRiskLevel + ".PV_CURTAIL"),
                    icon: { name: "arrow-down-outline", color: "primary", size: "medium" },
                    hide: (el: { delayChargeRiskLevel: RiskLevel, mode: Mode }) => el.delayChargeRiskLevel !== delayChargeRiskLevel || el.mode !== Mode.AUTOMATIC,
                }
            );
        });

        return lines.map(line => ({
            ...line,
            hide: line.hide ?? hideCondition,
        }));
    };

    const getFormlyManualView = (translate: TranslateService, hideCondition: (field: { mode: Mode }) => boolean,): OeFormlyView<GridOptimizedChargeViewModel>["lines"] => ([
        {
            type: "info-line",
            name: translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.END_TIME_DESCRIPTION"),
            hide: hideCondition,
        },
        {
            type: "time-line",
            name: translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.END_TIME"),
            controlName: "manualTargetTime",
            hide: hideCondition,
        },
    ]);

    const getFormlySharedLines = (
        translate: TranslateService,
        component: EdgeConfig.Component,
        edge: Edge,
        hideCondition: (field: { mode: Mode }) => boolean
    ): OeFormlyView<GridOptimizedChargeViewModel>["lines"] => {
        const lines: OeFormlyView<GridOptimizedChargeViewModel>["lines"] = [];

        lines.push(
            {
                type: "channel-line",
                name: translate.instant("GENERAL.STATE"),
                channel: new ChannelAddress(component.id, "DelayChargeState").toString(),
                converter: CONVERT_GRID_OPTIMIZED_CHARGE_STATE(translate),
                hide: hideCondition,
            },
            {
                type: "info-line",
                name: [
                    { text: translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.LIMITED_FUNCTIONALITY"), lineStyle: "color: #FFA500" },
                    { text: translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.STATE.ALLOWED_FEED_IN_LIMIT_TOO_LOW_DESCRIPTION") },
                ],
                icon: { name: "information-outline", size: "large", color: "primary" },
                hide: (el: { mode: Mode, delayChargeState: DelayChargeState }) => el.mode === Mode.OFF || el.delayChargeState !== DelayChargeState.ALLOWED_FEED_IN_LIMIT_TOO_LOW,
            },
            {
                type: "channel-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.MAXIMUM_CHARGE"),
                channel: component.id + "/SellToGridLimitMinimumChargeLimit",
                converter: Utils.CONVERT_TO_WATT,
                filter: Filter.NOT_NULL_OR_UNDEFINED,
                hide: hideCondition,
            },
            {
                type: "value-from-channels-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.END_TIME_LONG"),
                channelsToSubscribe: [
                    new ChannelAddress(component.id, "TargetMinute",),
                    new ChannelAddress(component.id, "DelayChargeState",),
                ],
                value: (data: CurrentData) => {
                    const targetMinute = data.allComponents[component.id + "/TargetMinute"];
                    return Converter.CONVERT_MINUTE_TO_TIME_OF_DAY(translate, Language.geti18nLocale())(targetMinute);
                },
                filter: (currentData: CurrentData) => {
                    return currentData.allComponents[component.id + "/DelayChargeState"] != DelayChargeState.ACTIVE_LIMIT &&
                        currentData.allComponents[component.id + "/TargetMinute"];
                },
                hide: hideCondition,
            });

        const essId = component.properties["ess.id"];
        if (essId != null) {
            lines.push({
                type: "channel-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.STORAGE_CAPACITY"),
                channel: essId + "/Capacity",
                filter: Filter.IS_AT_LEAST_ROLE(Role.ADMIN, edge),
                converter: Converter.CONVERT_TO_WATTHOURS(),
                hide: hideCondition,
            });
        }

        if (component.properties.sellToGridLimitEnabled) {
            lines.push({
                type: "value-from-channels-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.MAXIMUM_GRID_FEED_IN"),
                channelsToSubscribe: [
                    new ChannelAddress("_meta", "_PropertyMaximumGridFeedInLimit",),
                    new ChannelAddress(component.id, "_PropertyMaximumSellToGridPower",),
                ],
                value: (data: CurrentData) => {
                    if (hasMaximumGridFeedInLimitInMeta(edge)) {
                        return Utils.CONVERT_TO_WATT(data.allComponents["_meta/_PropertyMaximumGridFeedInLimit"]);
                    }
                    return Utils.CONVERT_TO_WATT(data.allComponents[component.id + "/_PropertyMaximumGridFeedInLimit"]);
                },
                hide: hideCondition,
            });
        }

        lines.push(
            {
                type: "info-line",
                name: translate.instant("GENERAL.MODE"),
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
                        icon: { color: "primary", name: "sunny", size: "medium" },
                    },
                    {
                        name: translate.instant("GENERAL.OFF"),
                        value: Mode.OFF,
                        icon: { color: "danger", name: "power-outline", size: "medium" },
                    },
                ],
            });

        lines.push(
            {
                type: "horizontal-line",
            });

        return lines;
    };

    export function getChannelAddresses(service: Service, routeService: RouteService, component: EdgeConfig.Component | null = null): Promise<ChannelAddress[]> {
        const edge = service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);

        const gridOptimizedChartComponent = component ?? config.getComponentSafely(routeService.getRouteParam("componentId"));

        AssertionUtils.assertIsDefined(gridOptimizedChartComponent);
        return Promise.resolve([
            new ChannelAddress(gridOptimizedChartComponent.id, "DelayChargeMaximumChargeLimit"),
            new ChannelAddress(gridOptimizedChartComponent.id, "_PropertyMode"),
            new ChannelAddress(gridOptimizedChartComponent.id, "_PropertyManualTargetTime"),
            new ChannelAddress(gridOptimizedChartComponent.id, "_PropertyDelayChargeRiskLevel"),
            new ChannelAddress(gridOptimizedChartComponent.id, "PredictedChargeStartEpochSeconds"),
            new ChannelAddress(gridOptimizedChartComponent.id, "DelayChargeState"),
            new ChannelAddress(gridOptimizedChartComponent.id, "TargetEpochSeconds"),
        ]);
    }

    export function getFormGroup(): FormGroup {
        return new FormGroup({
            mode: new FormControl(null),
            manualTargetTime: new FormControl(null),
            delayChargeRiskLevel: new FormControl(null),
            delayChargeState: new FormControl(null),
        });
    }

    export function getNavigationTree(translate: TranslateService, component: EdgeConfig.Component): ConstructorParameters<typeof NavigationTree> {
        return new NavigationTree(component.id, { baseString: "controller/grid-optimized-charge/" + component.id }, { name: "oe-grid-storage", color: "normal" }, Name.METER_ALIAS_OR_ID(component), "label", [
            new NavigationTree("history", { baseString: "history" }, { name: "stats-chart-outline", color: "warning" }, translate.instant("GENERAL.HISTORY"), "label", [], null),
            NavigationConstants.CommonNodes.SETTINGS(translate),
        ], null).toConstructorParams();
    }

    export enum RiskLevel {
        LOW = "LOW",
        MEDIUM = "MEDIUM",
        HIGH = "HIGH",
    }
}

export type GridOptimizedChargeViewModel = {
    mode: Mode;
    delayChargeRiskLevel: SharedGridOptimizedCharge.RiskLevel;
    delayChargeState: DelayChargeState;
};


/**
 * Converts Grid-optimized-charge-State
 *
 * @param translate the current language to be translated to
 * @returns converted value
 */
export const CONVERT_GRID_OPTIMIZED_CHARGE_STATE = (translate: TranslateService) => {
    return (value: any): string => {
        switch (value) {
            case -1:
                return translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.STATE.NOT_DEFINED");
            case 0:
                return translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.STATE.CHARGE_LIMIT_ACTIVE");
            case 1:
                return translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.STATE.PASSED_END_TIME");
            case 2:
                return translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.STATE.STORAGE_ALREADY_FULL");
            case 3:
                return translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.STATE.END_TIME_NOT_CALCULATED");
            case 4:
                return translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.STATE.NO_LIMIT_POSSIBLE");
            case 5: // Case 6: 'DISABLED' hides 'state-line', so no Message needed
            case 7:
                return translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.STATE.NO_LIMIT_ACTIVE");
            case 8:
                return translate.instant("EDGE.INDEX.WIDGETS.GRID_OPTIMIZED_CHARGE.CHARGING_DELAYED");
            default:
                return "";
        };
    };
};
