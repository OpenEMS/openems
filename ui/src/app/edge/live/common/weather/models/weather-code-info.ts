import { WeatherIcon } from "./weather-icon";

export interface WeatherCodeInfo {
    icon: WeatherIcon;
    description: string;
}

const weatherCodeDayMap = new Map<number, WeatherCodeInfo>([
    [0, { icon: WeatherIcon.ClearDay, description: "WEATHER_CODE_0" }],
    [1, { icon: WeatherIcon.PartlyCloudyDay, description: "WEATHER_CODE_1" }],
    [2, { icon: WeatherIcon.PartlyCloudyDay, description: "WEATHER_CODE_2" }],
    [3, { icon: WeatherIcon.WeatherCloudy, description: "WEATHER_CODE_3" }],
    [45, { icon: WeatherIcon.WeatherFoggy, description: "WEATHER_CODE_45" }],
    [48, { icon: WeatherIcon.WeatherFoggy, description: "WEATHER_CODE_48" }],
    [51, { icon: WeatherIcon.WeatherRainy, description: "WEATHER_CODE_51" }],
    [53, { icon: WeatherIcon.WeatherRainy, description: "WEATHER_CODE_53" }],
    [55, { icon: WeatherIcon.WeatherRainy, description: "WEATHER_CODE_55" }],
    [56, { icon: WeatherIcon.WeatherMix, description: "WEATHER_CODE_56" }],
    [57, { icon: WeatherIcon.WeatherMix, description: "WEATHER_CODE_57" }],
    [61, { icon: WeatherIcon.WeatherRainy, description: "WEATHER_CODE_61" }],
    [63, { icon: WeatherIcon.WeatherRainy, description: "WEATHER_CODE_63" }],
    [65, { icon: WeatherIcon.WeatherRainy, description: "WEATHER_CODE_65" }],
    [66, { icon: WeatherIcon.WeatherMix, description: "WEATHER_CODE_66" }],
    [67, { icon: WeatherIcon.WeatherMix, description: "WEATHER_CODE_67" }],
    [71, { icon: WeatherIcon.WeatherSnowy, description: "WEATHER_CODE_71" }],
    [73, { icon: WeatherIcon.WeatherSnowy, description: "WEATHER_CODE_73" }],
    [75, { icon: WeatherIcon.WeatherSnowy, description: "WEATHER_CODE_75" }],
    [77, { icon: WeatherIcon.WeatherSnowy, description: "WEATHER_CODE_77" }],
    [80, { icon: WeatherIcon.WeatherRainy, description: "WEATHER_CODE_80" }],
    [81, { icon: WeatherIcon.WeatherRainy, description: "WEATHER_CODE_81" }],
    [82, { icon: WeatherIcon.WeatherRainy, description: "WEATHER_CODE_82" }],
    [85, { icon: WeatherIcon.WeatherSnowy, description: "WEATHER_CODE_85" }],
    [86, { icon: WeatherIcon.WeatherSnowy, description: "WEATHER_CODE_86" }],
    [95, { icon: WeatherIcon.Thunderstorm, description: "WEATHER_CODE_95" }],
    [96, { icon: WeatherIcon.Thunderstorm, description: "WEATHER_CODE_96" }],
    [99, { icon: WeatherIcon.Thunderstorm, description: "WEATHER_CODE_99" }],
]);

const weatherCodeNightMap = new Map<number, WeatherCodeInfo>([
    [0, { icon: WeatherIcon.ClearNight, description: "WEATHER_CODE_0_NIGHT" }],
    [1, { icon: WeatherIcon.PartlyCloudyNight, description: "WEATHER_CODE_1_NIGHT" }],
    [2, { icon: WeatherIcon.PartlyCloudyNight, description: "WEATHER_CODE_2_NIGHT" }],
]);

const defaultWeatherCodeInfo: WeatherCodeInfo = { icon: WeatherIcon.Unknown, description: "WEATHER_CODE_UNKNOWN" };

/**
 * Returns weather info (icon + description) for a given weather code.
 *
 * @param weatherCode numeric weather code from forecast
 * @param isDay whether it is daytime (true) or nighttime (false)
 * @returns WeatherCodeInfo object containing icon and description
 */
export function getWeatherCodeInfo(weatherCode: number, isDay: boolean): WeatherCodeInfo {
    if (isDay === true) {
        return weatherCodeDayMap.get(weatherCode) ?? defaultWeatherCodeInfo;
    }
    return weatherCodeNightMap.get(weatherCode) ?? weatherCodeDayMap.get(weatherCode) ?? defaultWeatherCodeInfo;
}
