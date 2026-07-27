import { Component, ChangeDetectionStrategy } from "@angular/core";
import { WeatherBaseComponent } from "../shared/base";

@Component({
    templateUrl: "./modal.html",
    styleUrls: ["../shared/weather.scss"],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class WeatherModalComponent extends WeatherBaseComponent {}
