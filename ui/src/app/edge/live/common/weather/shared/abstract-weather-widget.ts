import { Directive, OnDestroy, OnInit } from "@angular/core";
import { MetaComponent } from "src/app/shared/components/edge/config-components/meta/meta";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { Modal } from "src/app/shared/components/flat/flat";
import { ChannelAddress, CurrentData } from "src/app/shared/shared";
import { NumberUtils } from "src/app/shared/utils/number/number-utils";
import { DailyWeatherForecasts } from "../jsonrpc/daily-weather-forecasts";
import { HourlyWeatherForecasts } from "../jsonrpc/hourly-weather-forecasts";
import { WeatherModalComponent } from "../modal/modal";
import { FORECAST_HOURS, isDayTime, WEATHER_CHANNEL_KEYS, } from "./weather.constants";

@Directive()
export abstract class AbstractWeatherWidget
    extends AbstractFlatWidget
    implements OnInit, OnDestroy
{
    protected placeName: string | null = null;
    protected isSmartphone: boolean = false;
    protected gotData: boolean = false;

    protected currentHourlyForecast: HourlyWeatherForecasts.Forecast | null =
        null;
    protected upcomingHourlyForecasts: HourlyWeatherForecasts.Forecast[] = [];
    protected currentDailyForecast: DailyWeatherForecasts.Forecast | null =
        null;
    protected upcomingDailyForecasts: DailyWeatherForecasts.Forecast[] = [];

    private lastProcessedHourKey: string | null = null;
    private lastWeatherSnapshot: string | null = null;

    protected get modalComponent(): Modal {
        return {
            component: WeatherModalComponent,
            componentProps: { component: this.component },
        };
    }

    protected override afterIsInitialized(): void {
        if (this.edge == null) {
            return;
        }
        const config = this.edge.getCurrentConfig();
        const meta = new MetaComponent(config);
        this.placeName = meta.getPropertyFromComponent("placeName") ?? "";
        this.isSmartphone = this.service.getIsSmartphoneResolution();
    }

    protected override getChannelAddresses(): ChannelAddress[] {
        const componentId = this.componentId;
        if (componentId == null) {
            return [];
        }
        return WEATHER_CHANNEL_KEYS.map(
            (key) => new ChannelAddress(componentId, key),
        );
    }

    protected override onCurrentData(currentData: CurrentData) {
        if (this.component == null) {
            return;
        }

        const base = this.component.id;
        const getChannelData = (k: string) =>
            currentData.allComponents?.[`${base}/${k}`] ?? null;
        const now = new Date();

        const hourKey = `${now.getFullYear()}-${now.getMonth()}-${now.getDate()}-${now.getHours()}`;

        const snapshot = WEATHER_CHANNEL_KEYS.map((key) =>
            String(getChannelData(key)),
        ).join("|");

        const shouldSkipUpdate =
            this.gotData &&
            this.lastProcessedHourKey === hourKey &&
            this.lastWeatherSnapshot === snapshot;

        if (shouldSkipUpdate) {
            return;
        }

        this.lastProcessedHourKey = hourKey;
        this.lastWeatherSnapshot = snapshot;

        this.currentDailyForecast = {
            date: now,
            minTemperature: getChannelData("TodaysMinTemperature"),
            maxTemperature: getChannelData("TodaysMaxTemperature"),
            weatherCode: getChannelData("CurrentWeatherCode"),
            sunshineDuration: NumberUtils.divideSafely(
                getChannelData("TodaysSunshineDuration"),
                3600,
            ),
        };

        this.currentHourlyForecast = {
            datetime: now,
            temperature: getChannelData("CurrentTemperature"),
            weatherCode: getChannelData("CurrentWeatherCode"),
            isDay: isDayTime(now),
        };

        const nowMs = now.getTime();
        this.upcomingHourlyForecasts = FORECAST_HOURS.map((h) => {
            const datetime = new Date(nowMs + h * 3_600_000);
            return {
                datetime,
                temperature: getChannelData(`TemperatureIn${h}h`),
                weatherCode: getChannelData(`WeatherCodeIn${h}h`),
                isDay: isDayTime(datetime),
            };
        }).filter((f) => f.temperature !== null && f.weatherCode !== null);

        const hasDaily =
            NumberUtils.isPresentNumber(
                this.currentDailyForecast.minTemperature,
            ) &&
            NumberUtils.isPresentNumber(
                this.currentDailyForecast.maxTemperature,
            ) &&
            NumberUtils.isPresentNumber(
                this.currentDailyForecast.sunshineDuration,
            ) &&
            NumberUtils.isPresentNumber(this.currentDailyForecast.weatherCode);

        const hasHourly =
            NumberUtils.isPresentNumber(
                this.currentHourlyForecast.temperature,
            ) &&
            NumberUtils.isPresentNumber(this.currentHourlyForecast.weatherCode);

        const hasUpcomingFull =
            this.upcomingHourlyForecasts.length === FORECAST_HOURS.length &&
            this.upcomingHourlyForecasts.every(
                (f) =>
                    NumberUtils.isPresentNumber(f.temperature) &&
                    NumberUtils.isPresentNumber(f.weatherCode),
            );

        this.upcomingHourlyForecasts = this.upcomingHourlyForecasts.slice(
            0,
            this.isSmartphone ? 5 : 6,
        );
        this.gotData = hasDaily && hasHourly && hasUpcomingFull;
    }
}
