import { FormControl, FormGroup } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { GroupedNavigationTreeUtility, NavigationTree, } from "src/app/shared/components/navigation/shared";
import { Converter } from "src/app/shared/components/shared/converter";
import { Name } from "src/app/shared/components/shared/name";
import { OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { RouteService } from "src/app/shared/service/route.service";
import { ChannelAddress, Edge, EdgeConfig, Service, } from "src/app/shared/shared";
import { Mode } from "src/app/shared/type/general";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";

export namespace SharedControllerBraiins {
    export const PROPERTY_MODE = "_PropertyMode";
    export const ACTIVE_POWER = "ActivePower";

    export function getFormlyView(
        translate: TranslateService,
        component: EdgeConfig.Component,
        edge: Edge,
    ): OeFormlyView {
        return {
            title: Name.METER_ALIAS_OR_ID(component),
            icon: {
                name: "logo-bitcoin",
                color: "normal",
                size: "large",
            },
            lines: [
                {
                    type: "channel-line",
                    name: translate.instant("GENERAL.STATE"),
                    channel: new ChannelAddress(
                        component.id,
                        PROPERTY_MODE,
                    ).toString(),
                    converter: Converter.CONTROLLER_PROPERTY_MODES(translate),
                },
                {
                    type: "channel-line",
                    name: translate.instant("GENERAL.POWER"),
                    channel: new ChannelAddress(
                        component.id,
                        ACTIVE_POWER,
                    ).toString(),
                    converter: Converter.POWER_IN_KILO_WATT,
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
                            name: translate.instant("GENERAL.ON"),
                            value: Mode.MANUAL_ON,
                            icon: {
                                color: "success",
                                name: "play-outline",
                                size: "medium",
                            },
                        },
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
            ],
            component: component,
            edge: edge,
        };
    }

    export async function getChannelAddresses(
        service: Service,
        routeService: RouteService,
        component: EdgeConfig.Component | null = null,
    ): Promise<ChannelAddress[]> {
        const edge = service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);

        const braiinsComponent =
            component ??
            config.getComponentSafely(
                routeService.getRouteParam("componentId"),
            );

        AssertionUtils.assertIsDefined(braiinsComponent);

        return [new ChannelAddress(braiinsComponent.id, PROPERTY_MODE)];
    }

    export function getFormGroup(): FormGroup {
        return new FormGroup({
            mode: new FormControl(null),
        });
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

        return createComponentNavigationTree(
            componentId,
            translate.instant("MENU.GROUPS.BRAIINS"),
            "controller/braiins/" + componentId,
            translate,
        ).toConstructorParams();
    }

    export function getNavigationTreeAsChild(
        translate: TranslateService,
        componentId: EdgeConfig.Component["id"],
        config: EdgeConfig,
    ): NavigationTree | null {
        const component = config.getComponentSafely(componentId);
        if (component == null) {
            return null;
        }

        const label = component.alias?.trim() || component.id;
        return createComponentNavigationTree(
            componentId,
            label,
            componentId,
            translate,
        );
    }

    export function getGroupedNavigationTree(
        translate: TranslateService,
        componentIds: EdgeConfig.Component["id"][],
        config: EdgeConfig,
    ): ConstructorParameters<typeof NavigationTree> | null {
        return GroupedNavigationTreeUtility.createGroupedNavigationTree(
            "braiins",
            { name: "logo-bitcoin", color: "normal" },
            "MENU.GROUPS.BRAIINS",
            "controller/braiins",
            translate,
            componentIds,
            config,
            (componentId) =>
                getNavigationTreeAsChild(translate, componentId, config),
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
            { name: "logo-bitcoin", color: "normal" },
            label,
            "label",
            [],
            null,
        );
    }
}
