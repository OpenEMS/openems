import { Component, ChangeDetectionStrategy } from "@angular/core";
import { WeatherBaseComponent } from "../shared/base";

@Component({
    selector: "oe-weather-modal-plain",
    templateUrl: "../new-navigation/new-navigation.html",
    styleUrls: ["../shared/weather.scss"],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class WeatherPlainComponent extends WeatherBaseComponent {}
