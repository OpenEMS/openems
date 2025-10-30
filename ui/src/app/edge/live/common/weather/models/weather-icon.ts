import { addIcons } from "ionicons";

export enum WeatherIcon {
    ClearDay = "oe-clear-day",
    ClearNight = "oe-clear-night",
    PartlyCloudyDay = "oe-partly-cloudy-day",
    PartlyCloudyNight = "oe-partly-cloudy-night",
    Thunderstorm = "oe-thunderstorm",
    WeatherCloudy = "oe-weather-cloudy",
    WeatherFoggy = "oe-weather-foggy",
    WeatherMix = "oe-weather-mix",
    WeatherRainy = "oe-weather-rainy",
    WeatherSnowy = "oe-weather-snowy",
    Unknown = "oe-help",
}

export function registerWeatherIcons(): void {
    addIcons({
        "oe-clear-day": "assets/img/icon/clear_day.svg",
        "oe-clear-night": "assets/img/icon/clear_night.svg",
        "oe-partly-cloudy-day": "assets/img/icon/partly_cloudy_day.svg",
        "oe-partly-cloudy-night": "assets/img/icon/partly_cloudy_night.svg",
        "oe-thunderstorm": "assets/img/icon/thunderstorm.svg",
        "oe-weather-cloudy": "assets/img/icon/weather_cloudy.svg",
        "oe-weather-foggy": "assets/img/icon/weather_foggy.svg",
        "oe-weather-mix": "assets/img/icon/weather_mix.svg",
        "oe-weather-rainy": "assets/img/icon/weather_rainy.svg",
        "oe-weather-snowy": "assets/img/icon/weather_snowy.svg",
        "oe-sunshine-duration": "assets/img/icon/sunshine_duration.svg",
        "oe-help": "assets/img/icon/help.svg",
    });
}
