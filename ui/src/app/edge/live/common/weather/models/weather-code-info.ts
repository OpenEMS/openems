import { SharedWeather } from "../shared/shared";
import { WeatherIcon } from "./weather-icon";

export interface WeatherCodeInfo {
    icon: WeatherIcon;
    description: string;
}

const defaultWeatherCodeInfo: WeatherCodeInfo = { icon: WeatherIcon.Unknown, description: "WEATHER_CODE_UNKNOWN" };

/**
 * Returns weather info (icon + description) for a given weather code.
 *
 * @param weatherCode Numeric weather code from forecast
 * @param isDay Whether it is daytime (true) or nighttime (false)
 * @returns WeatherCodeInfo object containing icon and description
 */
export function getWeatherCodeInfo(weatherCode: number, isDay: boolean): WeatherCodeInfo {
    if (isDay === true) {
        return SharedWeather.weatherCodeDayMap.get(weatherCode) ?? defaultWeatherCodeInfo;
    }
    return (
        SharedWeather.weatherCodeNightMap.get(weatherCode) ??
        SharedWeather.weatherCodeDayMap.get(weatherCode) ??
        defaultWeatherCodeInfo
    );
}
