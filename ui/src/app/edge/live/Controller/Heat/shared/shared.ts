import { TranslateService } from "@ngx-translate/core";
import { NavigationTree } from "src/app/shared/components/navigation/shared";
import { Converter } from "src/app/shared/components/shared/converter";
import { Name } from "src/app/shared/components/shared/name";
import { OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { RouteService } from "src/app/shared/service/route.service";
import { ChannelAddress, CurrentData, Edge, EdgeConfig, Service, Utils } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";

export namespace SharedControllerHeat {

    export const getFormlyView = (translate: TranslateService, component: EdgeConfig.Component, edge: Edge, isMyPv: boolean): OeFormlyView => {

        return {
            title: component.alias,
            icon: { name: "flame", color: "normal", size: "normal" },
            helpKey: "CONTROLLER_IO_HEATING_ELEMENT",
            lines: [
                ...getFormlySharedLines(translate, component),
                ...(isMyPv ? getMyPVInfoLine(translate, component) : []),
            ],
            component: component,
            edge: edge,
        };
    };

    const getFormlySharedLines = (translate: TranslateService, component: EdgeConfig.Component): OeFormlyView["lines"] => ([{
        type: "channel-line",
        name: translate.instant("GENERAL.STATUS"),
        channel: component.id + "/Status",
        converter: Converter.CONVERT_POWER_2_HEAT_STATE(translate),
    }, {
        type: "channel-line",
        name: translate.instant("EDGE.INDEX.WIDGETS.HEAT.HEATING_OUTPUT"),
        channel: component.id + "/ActivePower",
        converter: Utils.CONVERT_TO_WATT,
    }, {
        type: "channel-line",
        name: translate.instant("EDGE.INDEX.WIDGETS.HEAT.TEMPERATURE"),
        channel: component.id + "/Temperature",
        converter: Converter.DEZIDEGREE_CELSIUS_TO_DEGREE_CELSIUS,
    }]);

    const getMyPVInfoLine = (translate: TranslateService, component: EdgeConfig.Component): OeFormlyView["lines"] => ([{
        type: "info-line",
        name: translate.instant("EDGE.INDEX.WIDGETS.HEAT.CHANGES_MY_PV_INFO"),
        icon: { name: "information-outline", color: "primary", size: "small" },
    }]);


    export function getChannelAddresses(service: Service, routeService: RouteService, component: EdgeConfig.Component | null = null): Promise<ChannelAddress[]> {
        const edge = service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);

        const heatComponent = component ?? config.getComponentSafely(routeService.getRouteParam("componentId"));
        AssertionUtils.assertIsDefined(heatComponent);
        const channelAddresses: ChannelAddress[] = [
            new ChannelAddress(heatComponent.id, "ActivePower"),
            new ChannelAddress(heatComponent.id, "Temperature"),
            new ChannelAddress(heatComponent.id, "Status"),
        ];

        return Promise.resolve(channelAddresses);
    }

    export function getNavigationTree(translate: TranslateService, component: EdgeConfig.Component): ConstructorParameters<typeof NavigationTree> {
        return new NavigationTree(component.id, { baseString: "controller/heat/" + component.id }, { name: "flame", color: "normal" }, Name.METER_ALIAS_OR_ID(component), "label", [
            new NavigationTree("history", { baseString: "history" }, { name: "stats-chart-outline", color: "warning" }, translate.instant("GENERAL.HISTORY"), "label", [], null),
        ], null).toConstructorParams();
    }

    export function getStatusFromStatusNumber(currentData: CurrentData, statusNumber: number, component: EdgeConfig.Component): State {
        if (component == null) {
            return State.NO_HEATING;
        }

        // for read-only and write
        statusNumber = currentData.allComponents[component.id + "/" + "Status"] ?? Status.ERROR;

        switch (statusNumber) {
            case Status.STANDBY:
            case Status.EXCESS:
            case Status.CONTROL_NOT_ALLOWED:
                return State.HEATING;
            case Status.TEMPERATURE_REACHED:
                return State.TEMERATURE_REACHED;
            case Status.NO_CONTROL_SIGNAL:
                if (currentData.allComponents[component.id + "/" + "ActivePower"] > 0) {
                    return State.HEATING;
                } else {
                    return State.NO_HEATING;
                }
            case Status.ERROR:
                return State.NO_HEATING;
            default:
                return State.NO_HEATING;
        }
    }
}

export enum Status {
    STANDBY,                    // Device is in standby mode
    EXCESS,                     // Device is running using excess energy
    CONTROL_NOT_ALLOWED,          // Control is overridden by another system
    TEMPERATURE_REACHED,         // Target temperature has been reached
    NO_CONTROL_SIGNAL,            // No control signal is available
    ERROR,                      // An error occurred on the device
}

export enum State {
    HEATING,                    // Device is heating
    TEMERATURE_REACHED,         // Target temperature has been reached
    NO_HEATING,                  // Device is not heating
}
