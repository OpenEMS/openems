import { Pipe, PipeTransform } from "@angular/core";
import { getWeatherCodeInfo } from "../models/weather-code-info";

@Pipe({ name: "weatherCodeDescription" })
export class WeatherCodeDescriptionPipe implements PipeTransform {
    transform(weatherCode: number, isDay: boolean): string {
        return getWeatherCodeInfo(weatherCode, isDay).description;
    }
}
