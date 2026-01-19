import { Pipe, PipeTransform } from "@angular/core";
import { getWeatherCodeInfo } from "../models/weather-code-info";
import { WeatherIcon } from "../models/weather-icon";

@Pipe({ name: "weatherCodeIcon" })
export class WeatherCodeIconPipe implements PipeTransform {
    transform(weatherCode: number, isDay: boolean): WeatherIcon {
        return getWeatherCodeInfo(weatherCode, isDay).icon;
    }
}
