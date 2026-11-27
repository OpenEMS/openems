import { TranslateService } from "@ngx-translate/core";
import { NavigationTree } from "src/app/shared/components/navigation/shared";
import { OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { RouteService } from "src/app/shared/service/route/route.service";
import { ChannelAddress, Edge, EdgeConfig, Service } from "src/app/shared/shared";
import { Mode } from "src/app/shared/type/general";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { WeatherCodeInfo } from "../models/weather-code-info";
import { WeatherIcon } from "../models/weather-icon";

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

    export const weatherCodeDayMap = new Map<number, WeatherCodeInfo>([
        [0, { icon: WeatherIcon.ClearDay, description: "WEATHER_CODE_0" }],
        [1, { icon: WeatherIcon.PartlyCloudyDay, description: "WEATHER_CODE_1" }],
        [2, { icon: WeatherIcon.PartlyCloudyDay, description: "WEATHER_CODE_2" }],
        [3, { icon: WeatherIcon.WeatherCloudy, description: "WEATHER_CODE_3" }],
        [45, { icon: WeatherIcon.WeatherFoggy, description: "WEATHER_CODE_45" }],
        [48, { icon: WeatherIcon.WeatherFoggy, description: "WEATHER_CODE_48" }],
        [51, { icon: WeatherIcon.WeatherRainy, description: "WEATHER_CODE_51" }],
        [53, { icon: WeatherIcon.WeatherRainy, description: "WEATHER_CODE_53" }],
        [55, { icon: WeatherIcon.WeatherRainy, description: "WEATHER_CODE_55" }],
        [56, { icon: WeatherIcon.WeatherMix, description: "WEATHER_CODE_56" }],
        [57, { icon: WeatherIcon.WeatherMix, description: "WEATHER_CODE_57" }],
        [61, { icon: WeatherIcon.WeatherRainy, description: "WEATHER_CODE_61" }],
        [63, { icon: WeatherIcon.WeatherRainy, description: "WEATHER_CODE_63" }],
        [65, { icon: WeatherIcon.WeatherRainy, description: "WEATHER_CODE_65" }],
        [66, { icon: WeatherIcon.WeatherMix, description: "WEATHER_CODE_66" }],
        [67, { icon: WeatherIcon.WeatherMix, description: "WEATHER_CODE_67" }],
        [71, { icon: WeatherIcon.WeatherSnowy, description: "WEATHER_CODE_71" }],
        [73, { icon: WeatherIcon.WeatherSnowy, description: "WEATHER_CODE_73" }],
        [75, { icon: WeatherIcon.WeatherSnowy, description: "WEATHER_CODE_75" }],
        [77, { icon: WeatherIcon.WeatherSnowy, description: "WEATHER_CODE_77" }],
        [80, { icon: WeatherIcon.WeatherRainy, description: "WEATHER_CODE_80" }],
        [81, { icon: WeatherIcon.WeatherRainy, description: "WEATHER_CODE_81" }],
        [82, { icon: WeatherIcon.WeatherRainy, description: "WEATHER_CODE_82" }],
        [85, { icon: WeatherIcon.WeatherSnowy, description: "WEATHER_CODE_85" }],
        [86, { icon: WeatherIcon.WeatherSnowy, description: "WEATHER_CODE_86" }],
        [95, { icon: WeatherIcon.Thunderstorm, description: "WEATHER_CODE_95" }],
        [96, { icon: WeatherIcon.Thunderstorm, description: "WEATHER_CODE_96" }],
        [99, { icon: WeatherIcon.Thunderstorm, description: "WEATHER_CODE_99" }],
    ]);

    export const weatherCodeNightMap = new Map<number, WeatherCodeInfo>([
        [0, { icon: WeatherIcon.ClearNight, description: "WEATHER_CODE_0_NIGHT" }],
        [1, { icon: WeatherIcon.PartlyCloudyNight, description: "WEATHER_CODE_1_NIGHT" }],
        [2, { icon: WeatherIcon.PartlyCloudyNight, description: "WEATHER_CODE_2_NIGHT" }],
    ]);
}
