import { FormControl, FormGroup } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { GroupedNavigationTreeUtility, NavigationConstants, NavigationTree, } from "src/app/shared/components/navigation/shared";
import { Name } from "src/app/shared/components/shared/name";
import { OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { RouteService } from "src/app/shared/service/route.service";
import { ChannelAddress, Edge, EdgeConfig, Service } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";

export namespace SharedControllerIoFixDigitalOutput {
    export function getFormlyView(
        translate: TranslateService,
        component: EdgeConfig.Component,
        edge: Edge,
    ): OeFormlyView {
        return {
            title: Name.METER_ALIAS_OR_ID(component),
            lines: [
                {
                    type: "channel-line",
                    name: translate.instant("GENERAL.CURRENT_STATUS"),
                    channel: component.properties["outputChannelAddress"],
                },
                {
                    type: "buttons-from-form-control-line",
                    name: translate.instant("GENERAL.MODE"),
                    controlName: "isOn",
                    buttons: [
                        {
                            name: translate.instant("GENERAL.ON"),
                            value: 1,
                            icon: { color: "success", name: "play-outline", size: "medium" },
                        },
                        {
                            name: translate.instant("GENERAL.OFF"),
                            value: 0,
                            icon: { color: "danger", name: "power-outline", size: "medium" },
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

        const fixDigitalOutputComponent =
            component ?? config.getComponentSafely(routeService.getRouteParam("componentId"));

        AssertionUtils.assertIsDefined(fixDigitalOutputComponent);
        const outputChannel = fixDigitalOutputComponent.getPropertyFromComponent<string>("outputChannelAddress");

        const channelAddress = ChannelAddress.fromStringSafely(outputChannel);

        return [
            new ChannelAddress(fixDigitalOutputComponent.id, "_PropertyIsOn"),
            ...(channelAddress ? [channelAddress] : []),
        ];
    }

    export function getFormGroup(): FormGroup {
        return new FormGroup({
            isOn: new FormControl(null),
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

        const label = component.alias?.trim() || component.id;
        return createComponentNavigationTree(
            componentId,
            label,
            "controller/io-fix-digital-output/" + componentId,
            translate,
        ).toConstructorParams();
    }

    export function getGroupedNavigationTree(
        translate: TranslateService,
        componentIds: EdgeConfig.Component["id"][],
        config: EdgeConfig,
        factoryId: EdgeConfig.Factory["id"],
    ): NavigationTree | null {
        return GroupedNavigationTreeUtility.createGroupedNavigationTree(
            "fix-digital-output-controllers",
            { name: "power-outline", color: "normal" },
            "MENU.GROUPS.FIX_DIGITAL_OUTPUT",
            "controller/io-fix-digital-output",
            translate,
            componentIds,
            config,
            factoryId,
            (componentId) =>
                GroupedNavigationTreeUtility.getNavigationTreeAsChild(
                    translate,
                    componentId,
                    config,
                    createComponentNavigationTree,
                ),
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
            { name: "power-outline", color: "normal" },
            label,
            "label",
            [NavigationConstants.CommonNodes.HISTORY(translate)],
            null,
        );
    }
}
