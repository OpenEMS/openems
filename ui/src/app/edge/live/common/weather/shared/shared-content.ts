import { ChangeDetectionStrategy, Component, Input } from "@angular/core";
import { DailyWeatherForecasts } from "../jsonrpc/daily-weather-forecasts";
import { HourlyWeatherForecasts } from "../jsonrpc/hourly-weather-forecasts";

@Component({
    selector: "oe-weather-content",
    templateUrl: "./shared-content.html",
    styleUrls: ["../shared/weather.scss"],
    standalone: false,
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WeatherSharedContentComponent {
    @Input({ required: true }) public currentHourlyForecast!: HourlyWeatherForecasts.Forecast;
    @Input({ required: true }) public currentDailyForecast!: DailyWeatherForecasts.Forecast;
    @Input({ required: true }) public upcomingHourlyForecasts!: HourlyWeatherForecasts.Forecast[];
    @Input({ required: true }) public upcomingDailyForecasts!: DailyWeatherForecasts.Forecast[];
}
