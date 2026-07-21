import { Component, ChangeDetectionStrategy } from "@angular/core";
import { AbstractWeatherWidget } from "../shared/abstract-weather-widget";

@Component({
    selector: WeatherComponent.SELECTOR,
    templateUrl: "./flat.html",
    styleUrls: ["../shared/weather.scss"],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class WeatherComponent extends AbstractWeatherWidget {
    protected static readonly SELECTOR = "weather";
}
