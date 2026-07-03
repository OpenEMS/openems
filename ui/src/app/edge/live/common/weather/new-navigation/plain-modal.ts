import { Component } from "@angular/core";
import { WeatherBaseComponent } from "../shared/base";

@Component({
    selector: "oe-weather-modal-plain",
    templateUrl: "../new-navigation/new-navigation.html",
    styleUrls: ["../shared/weather.scss"],
    standalone: false,
})
export class WeatherPlainComponent extends WeatherBaseComponent { }
