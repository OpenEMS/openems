import { TranslateService } from "@ngx-translate/core";
import { NavigationTree } from "src/app/shared/components/navigation/shared";
import { OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { RouteService } from "src/app/shared/service/route.service";
import { ChannelAddress, Edge, EdgeConfig, Service } from "src/app/shared/shared";
import { Mode } from "src/app/shared/type/general";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";

export namespace SharedWeather {
    export const getFormlyView = (
        translate: TranslateService,
        component: EdgeConfig.Component,
        edge: Edge,
    ): OeFormlyView<{ mode: Mode }> => {
        return {
            title: translate.instant("TITLE"),
            icon: { name: "oe-partly-cloudy-day", color: "normal", size: "large" },
            lines: [],
            component: component,
            edge: edge,
        };
    };

    export function getChannelAddresses(
        service: Service,
        routeService: RouteService,
        component: EdgeConfig.Component | null = null,
    ): Promise<ChannelAddress[]> {
        const edge = service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);

        const EnerixControlComponent =
            component ?? config.getComponentSafely(routeService.getRouteParam("componentId"));

        AssertionUtils.assertIsDefined(EnerixControlComponent);
        return Promise.resolve([]);
    }

    export function getNavigationTree(
        translate: TranslateService,
        component: EdgeConfig.Component,
    ): ConstructorParameters<typeof NavigationTree> {
        return new NavigationTree(
            component.id,
            { baseString: "common/weather/" + component.id },
            { name: "oe-partly-cloudy-day", color: "normal" },
            translate.instant("TITLE"),
            "icon",
            [],
            null,
            { showOrder: "HIGH", isCommonWidget: true },
        ).toConstructorParams();
    }
}
