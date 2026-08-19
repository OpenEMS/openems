import { Component, ChangeDetectionStrategy, inject, computed } from "@angular/core";
import { PlatFormService } from "src/app/platform.service";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { NavigationService } from "src/app/shared/components/navigation/service/navigation.service";
import { NavigationTree } from "src/app/shared/components/navigation/shared";
import { RouteService } from "src/app/shared/service/route.service";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { NumberFormatPipe } from "../pipes/number-format.pipe";
import { WeatherCodeDescriptionPipe } from "../pipes/weather-code-description.pipe";
import { WeatherCodeIconPipe } from "../pipes/weather-code-icon.pipe";
import { AbstractWeatherWidget } from "../shared/abstract-weather-widget";
import { SharedWeather } from "../shared/shared";

@Component({
    selector: MiniWeatherComponent.SELECTOR,
    templateUrl: "./mini-widget.html",
    standalone: true,
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [CommonUiModule, WeatherCodeIconPipe, WeatherCodeDescriptionPipe, NumberFormatPipe],
    styles: `
        /* only applied on desktop*/

        .weather-widget {
            width: 100%;
            max-width: 16em;
            min-width: 13em;
            justify-self: start;
            align-self: start;
            padding-left: 1rem;
        }

        .weather-main {
            display: flex;
            align-items: center;
            justify-content: space-evenly;
            gap: 0.75rem;
        }

        .weather-icon {
            font-size: 6em;
            flex-shrink: 0;
        }

        .weather-text {
            display: flex;
            flex-direction: column;
            align-items: flex-start;
        }

        .temperature {
            font-size: 2em;
            line-height: 1;
            font-family: "FeneconHeadline";
        }

        .sunshine-row {
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 0.25rem;
        }

        .sunshine-duration-icon {
            flex-shrink: 0;
        }
    `,
})
export class MiniWeatherComponent extends AbstractWeatherWidget {
    protected static readonly SELECTOR = "mini-weather";

    protected readonly platFormService = inject(PlatFormService);
    protected readonly routeService = inject(RouteService);

    protected readonly isSmartPhone = computed(() => {
        const _currentUrl = this.routeService.currentUrl();
        return this.platFormService.getDevice().isSmartphone() ?? false;
    });
    private readonly navigationService = inject(NavigationService);

    protected async navigateToWeather() {
        const edgeConfig = await this.service.getConfig();
        AssertionUtils.assertIsDefined(edgeConfig);
        const weatherComponent = edgeConfig.getFirstComponentByFactoryId("Weather.OpenMeteo");
        AssertionUtils.assertIsDefined(weatherComponent);

        const weatherTree = new NavigationTree(...SharedWeather.getNavigationTree(this.translate, weatherComponent));

        const absoluteNavigationTree = NavigationService.convertRelativeToAbsoluteLink(
            this.navigationService.navigationTree(),
        );

        const weatherNode = NavigationTree.findById(absoluteNavigationTree, weatherTree.id);
        AssertionUtils.assertIsDefined(weatherNode);
        this.navigationService.navigateAbsolute(weatherNode);
    }
}
