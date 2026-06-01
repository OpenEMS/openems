import { FormControl, FormGroup } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { NavigationConstants, NavigationTree } from "src/app/shared/components/navigation/shared";
import { Converter } from "src/app/shared/components/shared/converter";
import { Name } from "src/app/shared/components/shared/name";
import { OeFormlyField, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { RouteService } from "src/app/shared/service/route.service";
import { ChannelAddress, CurrentData, Edge, EdgeConfig, Service, Utils } from "src/app/shared/shared";
import { Mode } from "src/app/shared/type/general";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";

export namespace SharedEssFixDigitalPowerControl {
    // hide automatic elements when mode is manual
    const HIDE_ON_MODE_MANUAL_OFF = (el: { mode: Mode }) => el.mode === Mode.MANUAL_OFF;

    export const PROPERTY_MODE = "_PropertyMode";
    export const PROPERTY_POWER = "_PropertyPower";

    export const getFormlyView = (translate: TranslateService, component: EdgeConfig.Component, edge: Edge): OeFormlyView<{ mode: Mode }> => {
        return {
            title: component.alias,
            icon: { name: "swap-vertical-outline", color: "normal", size: "large" },
            lines: [
                ...getFormlySharedModeAndStateLines(translate, component),
                ...getFormlySharedLines(translate),
                ...getFormlyManualOnView(translate, HIDE_ON_MODE_MANUAL_OFF),
                ...getFormlySharedInfoLine(translate),
            ],
            component: component,
            edge: edge,
        };
    };

    const getFormlyManualOnView = (translate: TranslateService, hideCondition: (field: { mode: Mode }) => boolean): OeFormlyView<{ mode: Mode }>["lines"] => ([
        {
            type: "input-line",
            name: translate.instant("GENERAL.POWER"),
            controlName: "power",
            properties: {
                unit: "W",
            },
            hide: hideCondition,
        },
    ]);

    const getFormlySharedInfoLine = (translate: TranslateService): OeFormlyView["lines"] => ([
        {
            type: "info-line",
            name: translate.instant("EDGE.INDEX.WIDGETS.INFO_STORAGE_FOR_CHARGE") + ". " + translate.instant("EDGE.INDEX.WIDGETS.INFO_STORAGE_FOR_DISCHARGE"),
        },
    ]);

    export const getFormlySharedModeAndStateLines = (translate: TranslateService, component: EdgeConfig.Component): OeFormlyView["lines"] => {
        const lines: OeFormlyField[] = [];

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
                    new ChannelAddress(component.id, PROPERTY_POWER),
                ],
                value: (currentData: CurrentData) => {
                    const power = currentData.allComponents[component.id + "/_PropertyPower"];
                    const powerValue = Utils.convertChargeDischargePower(translate, power);
                    return Utils.CONVERT_WATT_TO_KILOWATT(powerValue.value);
                },
                filter: (currentData: CurrentData) => {
                    const mode = currentData.allComponents[new ChannelAddress(component.id, PROPERTY_MODE).toString()];
                    return mode === Mode.MANUAL_ON;
                },
            }
        );
        return lines;
    };

    const getFormlySharedLines = (translate: TranslateService): OeFormlyView["lines"] => ([
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
                    icon: { color: "success", name: "play-outline", size: "medium" },
                },
                {
                    name: translate.instant("GENERAL.OFF"),
                    value: Mode.MANUAL_OFF,
                    icon: { color: "danger", name: "stop-circle-outline", size: "medium" },
                },
            ],
        },
        {
            type: "horizontal-line",
        },
    ]);

    export function getChannelAddresses(service: Service, routeService: RouteService, component: EdgeConfig.Component | null = null): Promise<ChannelAddress[]> {
        const edge = service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);

        const EnerixControlComponent = component ?? config.getComponentSafely(routeService.getRouteParam("componentId"));

        AssertionUtils.assertIsDefined(EnerixControlComponent);
        return Promise.resolve([
            new ChannelAddress(EnerixControlComponent.id, PROPERTY_MODE),
            new ChannelAddress(EnerixControlComponent.id, PROPERTY_POWER),
        ]);
    }

    export function getFormGroup(): FormGroup {
        return new FormGroup({
            mode: new FormControl(null),
            power: new FormControl(null),
        });
    }

    export function getNavigationTree(translate: TranslateService, component: EdgeConfig.Component): ConstructorParameters<typeof NavigationTree> {
        return new NavigationTree(component.id, { baseString: "controller/ess-fix-active-power/" + component.id }, { name: "swap-vertical-outline", color: "normal" }, Name.METER_ALIAS_OR_ID(component), "label", [
            NavigationConstants.CommonNodes.SETTINGS(translate),
        ], null).toConstructorParams();
    }
}
