import { Component, inject, OnDestroy, OnInit } from "@angular/core";
import { isAfter, isSameDay, startOfDay } from "date-fns";
import { interval, startWith, Subscription } from "rxjs";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { ChannelAddress, CurrentData, Service } from "src/app/shared/shared";
import { DailyWeatherForecasts } from "../jsonrpc/daily-weather-forecasts";
import { HourlyWeatherForecasts } from "../jsonrpc/hourly-weather-forecasts";
import { FORECAST_HOURS, isDayTime, WEATHER_CHANNEL_KEYS } from "../shared/weather.constants";

@Component({
  templateUrl: "./modal.html",
  styleUrls: ["../shared/weather.scss"],
  standalone: false,
})
export class WeatherModalComponent extends AbstractModal implements OnInit, OnDestroy {

  private static readonly FETCH_INTERVAL_MS = 15 /* minutes */ * 60 * 1000;

  protected placeName: string | null = null;
  protected isSmartphone: boolean = false;
  protected gotData: boolean | null = null;

  protected currentHourlyForecast: HourlyWeatherForecasts.Forecast | null = null;
  protected upcomingHourlyForecasts: HourlyWeatherForecasts.Forecast[] = [];

  protected currentDailyForecast: DailyWeatherForecasts.Forecast | null = null;
  protected upcomingDailyForecasts: DailyWeatherForecasts.Forecast[] = [];

  protected resolutionService = inject(Service);
  protected weatherForecastSubscription?: Subscription;

  public override ngOnDestroy(): void {
    this.weatherForecastSubscription?.unsubscribe();
    super.ngOnDestroy();
  }

  protected override onIsInitialized(): void {
    if (this.component == null || this.edge == null) {
      return;
    }
    const meta = this.edge.getConfig(this.websocket).value.getComponentsByFactory("Core.Meta")[0];
    this.placeName = meta.getPropertyFromComponent("placeName") ?? "";

    this.isSmartphone = this.resolutionService.isSmartphoneResolution;

    this.weatherForecastSubscription = interval(WeatherModalComponent.FETCH_INTERVAL_MS)
      .pipe(startWith(0))
      .subscribe(() => {
        this.fetchDailyWeatherForecast();
      });
  }

  protected fetchDailyWeatherForecast() {
    if (this.component == null || this.edge == null) {
      return;
    }

    this.edge.sendRequest<DailyWeatherForecasts.Response>(this.websocket,
      new ComponentJsonApiRequest({
        componentId: this.component.id,
        payload:
          new DailyWeatherForecasts.Request({}),
      })
    ).then(rawResponse => {
      const response = new DailyWeatherForecasts.Response(rawResponse.id, rawResponse.result);
      const now = new Date();
      this.currentDailyForecast = response.forecast.find(forecast => isSameDay(forecast.date, now)) ?? null;
      this.upcomingDailyForecasts = response.forecast.filter(forecast => isAfter(startOfDay(forecast.date), startOfDay(now)));
    }).catch(error => {
      console.warn("Error fetching weather forecast", error);
    });
  }

  protected override getChannelAddresses(): ChannelAddress[] {
    if (this.component == null) {
      return [];
    }

    const id = this.component.id;
    const buildChannelAddress = (name: string) => new ChannelAddress(id, name);

    return WEATHER_CHANNEL_KEYS.map(key => buildChannelAddress(key));
  }

  protected override onCurrentData(currentData: CurrentData) {
    if (this.component == null || this.edge == null) {
      return;
    }

    const base = this.component.id;
    const getChannelData = (k: string) => currentData.allComponents[`${base}/${k}`];

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
