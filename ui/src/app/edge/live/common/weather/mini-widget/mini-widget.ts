import { Component, ChangeDetectionStrategy } from "@angular/core";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { NumberFormatPipe } from "../pipes/number-format.pipe";
import { WeatherCodeDescriptionPipe } from "../pipes/weather-code-description.pipe";
import { WeatherCodeIconPipe } from "../pipes/weather-code-icon.pipe";
import { AbstractWeatherWidget } from "../shared/abstract-weather-widget";

@Component({
    selector: MiniWeatherComponent.SELECTOR,
    templateUrl: "./mini-widget.html",
    styles: `
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
    standalone: true,
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [CommonUiModule, WeatherCodeIconPipe, WeatherCodeDescriptionPipe, NumberFormatPipe],
})
export class MiniWeatherComponent extends AbstractWeatherWidget {
    protected static readonly SELECTOR = "mini-weather";
}
