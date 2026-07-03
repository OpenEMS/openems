import { FormControl, FormGroup } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { NavigationConstants, NavigationTree } from "src/app/shared/components/navigation/shared";
import { Name } from "src/app/shared/components/shared/name";
import { OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { RouteService } from "src/app/shared/service/route.service";
import { ChannelAddress, Edge, EdgeConfig, Service } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";

export namespace SharedControllerIoFixDigitalOutput {

    export const getFormlyView = (translate: TranslateService, component: EdgeConfig.Component, edge: Edge): OeFormlyView => ({
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
    });

    export async function getChannelAddresses(service: Service, routeService: RouteService, component: EdgeConfig.Component | null = null): Promise<ChannelAddress[]> {
        const edge = service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);

        const fixDigitalOutputComponent = component ?? config.getComponentSafely(routeService.getRouteParam("componentId"));

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

    export function getNavigationTree(translate: TranslateService, component: EdgeConfig.Component): ConstructorParameters<typeof NavigationTree> {
        return new NavigationTree(component.id, { baseString: "controller/io-fix-digital-output/" + component.id }, { name: "power-outline", color: "normal" }, Name.METER_ALIAS_OR_ID(component), "label", [
            NavigationConstants.CommonNodes.HISTORY(translate),
        ], null).toConstructorParams();
    }
}
