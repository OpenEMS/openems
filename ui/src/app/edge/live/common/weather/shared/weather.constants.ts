import { Pipe, PipeTransform } from "@angular/core";

export const FORECAST_HOURS = [4, 8, 12, 16, 20, 24] as const;

export const WEATHER_CHANNEL_KEYS = [
    "CurrentTemperature",
    "CurrentWeatherCode",

    "TodaysSunshineDuration",
    "TodaysMinTemperature",
    "TodaysMaxTemperature",

    "TemperatureIn4h",
    "WeatherCodeIn4h",
    "TemperatureIn8h",
    "WeatherCodeIn8h",
    "TemperatureIn12h",
    "WeatherCodeIn12h",
    "TemperatureIn16h",
    "WeatherCodeIn16h",
    "TemperatureIn20h",
    "WeatherCodeIn20h",
    "TemperatureIn24h",
    "WeatherCodeIn24h",
] as const;

export const isDayTime = (date: Date): boolean => {
    const sunrise = 6;
    const sunset = 21;
    const hour = date.getHours();
    return hour >= sunrise && hour < sunset;
};

@Pipe({ name: "secondsToHours", standalone: true, pure: true })
export class SecondsToHoursPipe implements PipeTransform {
    transform(seconds?: number): number {
        return Math.round(((seconds ?? 0) / 3600));
    }
}

