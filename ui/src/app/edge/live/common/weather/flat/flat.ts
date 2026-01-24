import { Component, inject, OnDestroy, OnInit } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { Modal } from "src/app/shared/components/flat/flat";
import { ChannelAddress, CurrentData, Service } from "src/app/shared/shared";
import { DailyWeatherForecasts } from "../jsonrpc/daily-weather-forecasts";
import { HourlyWeatherForecasts } from "../jsonrpc/hourly-weather-forecasts";
import { WeatherModalComponent } from "../modal/modal";
import { FORECAST_HOURS, isDayTime, WEATHER_CHANNEL_KEYS } from "../shared/weather.constants";

@Component({
    selector: WeatherComponent.SELECTOR,
    templateUrl: "./flat.html",
    styleUrls: ["../shared/weather.scss"],
    standalone: false,
})
export class WeatherComponent extends AbstractFlatWidget implements OnInit, OnDestroy {

    protected static readonly SELECTOR = "weather";

    protected placeName: string | null = null;
    protected isSmartphone: boolean | null = null;
    protected gotData: boolean | null = null;

    protected currentHourlyForecast: HourlyWeatherForecasts.Forecast | null = null;
    protected upcomingHourlyForecasts: HourlyWeatherForecasts.Forecast[] = [];

    protected currentDailyForecast: DailyWeatherForecasts.Forecast | null = null;
    protected upcomingDailyForecasts: DailyWeatherForecasts.Forecast[] = [];

    protected resolutionService = inject(Service);

    protected get modalComponent(): Modal {
        return {
            component: WeatherModalComponent,
            componentProps: {
                component: this.component,
            },
        };
    };

    protected override afterIsInitialized(): void {
        const meta = this.edge.getConfig(this.websocket).value.getComponentsByFactory("Core.Meta")[0];
        this.placeName = meta.getPropertyFromComponent("placeName") ?? "";

        this.isSmartphone = this.resolutionService.isSmartphoneResolution;
    }

    protected override getChannelAddresses() {
        const id = this.componentId;
        const buildChannelAddress = (name: string) => new ChannelAddress(id, name);

        return WEATHER_CHANNEL_KEYS.map(key => buildChannelAddress(key));
    }

    protected override onCurrentData(currentData: CurrentData) {
        if (this.gotData) {
            return;
        }

        const base = this.component.id;
        const getChannelData = (k: string) => {
            const value = currentData.allComponents?.[`${base}/${k}`];
            return value !== null && value !== undefined ? value : null;
        };

        this.currentDailyForecast = {
            date: new Date(),
            minTemperature: getChannelData("TodaysMinTemperature") ?? null,
            maxTemperature: getChannelData("TodaysMaxTemperature") ?? null,
            weatherCode: getChannelData("CurrentWeatherCode") ?? null,
            sunshineDuration: getChannelData("TodaysSunshineDuration") != null
                ? this.Utils.divideSafely(getChannelData("TodaysSunshineDuration"), 3600)
                : null,
        };

        this.currentHourlyForecast = {
            datetime: new Date(),
            temperature: getChannelData("CurrentTemperature") ?? null,
            weatherCode: getChannelData("CurrentWeatherCode") ?? null,
            isDay: isDayTime(new Date()),
        };

        const MS_PER_HOUR = 3600_000;
        const nowMs = Date.now();

        this.upcomingHourlyForecasts = FORECAST_HOURS
            .map(h => {
                const datetime = new Date(nowMs + h * MS_PER_HOUR);
                return {
                    datetime,
                    temperature: getChannelData(`TemperatureIn${h}h`) ?? null,
                    weatherCode: getChannelData(`WeatherCodeIn${h}h`) ?? null,
                    isDay: isDayTime(datetime),
                };
            })
            .filter(f => f.temperature !== null && f.weatherCode !== null);


        const isPresentNumber = (v: unknown): v is number =>
            typeof v === "number" && Number.isFinite(v);

        const hasDaily =
            this.currentDailyForecast != null &&
            isPresentNumber(this.currentDailyForecast.minTemperature) &&
            isPresentNumber(this.currentDailyForecast.maxTemperature) &&
            isPresentNumber(this.currentDailyForecast.sunshineDuration) &&
            isPresentNumber(this.currentDailyForecast.weatherCode);

        const hasHourly =
            this.currentHourlyForecast != null &&
            isPresentNumber(this.currentHourlyForecast.temperature) &&
            isPresentNumber(this.currentHourlyForecast.weatherCode);

        const hasUpcomingFull =
            Array.isArray(this.upcomingHourlyForecasts) &&
            this.upcomingHourlyForecasts.length === FORECAST_HOURS.length &&
            this.upcomingHourlyForecasts.every(f =>
                isPresentNumber(f.temperature) && isPresentNumber(f.weatherCode)
            );

        this.upcomingHourlyForecasts = this.upcomingHourlyForecasts.slice(0, this.isSmartphone ? 5 : 6);

        this.gotData = hasDaily && hasHourly && hasUpcomingFull;
    }
}
