import { Component, inject, OnDestroy, OnInit } from "@angular/core";
import { isAfter, isSameDay, isSameHour, startOfDay, startOfHour } from "date-fns";
import { interval, startWith, Subscription } from "rxjs";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { Service } from "src/app/shared/shared";
import { DailyWeatherForecasts } from "../jsonrpc/daily-weather-forecasts";
import { HourlyWeatherForecasts } from "../jsonrpc/hourly-weather-forecasts";

@Component({
  selector: WeatherComponent.SELECTOR,
  templateUrl: "./flat.html",
  styleUrls: ["./flat.scss"],
  standalone: false,
})
export class WeatherComponent extends AbstractFlatWidget implements OnInit, OnDestroy {

  private static readonly SELECTOR = "weather";
  private static readonly FETCH_INTERVAL_MS = 15 /* minutes */ * 60 * 1000;

  protected placeName: string | null = null;
  protected isSmartphone: boolean = false;

  protected currentHourlyForecast: HourlyWeatherForecasts.Forecast | null = null;
  protected upcomingHourlyForecasts: HourlyWeatherForecasts.Forecast[] = [];

  protected currentDailyForecast: DailyWeatherForecasts.Forecast | null = null;
  protected upcomingDailyForecasts: DailyWeatherForecasts.Forecast[] = [];

  private resolutionService = inject(Service);
  private weatherForecastSubscription?: Subscription;

  public override ngOnDestroy(): void {
    this.weatherForecastSubscription?.unsubscribe();
    super.ngOnDestroy();
  }

  protected override afterIsInitialized(): void {
    const meta = this.edge.getConfig(this.websocket).value.getComponentsByFactory("Core.Meta")[0];
    this.placeName = meta.getPropertyFromComponent("placeName") ?? "";

    this.isSmartphone = this.resolutionService.isSmartphoneResolution;

    this.weatherForecastSubscription = interval(WeatherComponent.FETCH_INTERVAL_MS)
      .pipe(startWith(0))
      .subscribe(() => {
        this.fetchHourlyWeatherForecast();
        this.fetchDailyWeatherForecast();
      });
  }

  private fetchHourlyWeatherForecast() {
    this.edge.sendRequest<HourlyWeatherForecasts.Response>(this.websocket,
      new ComponentJsonApiRequest({
        componentId: this.componentId,
        payload:
          new HourlyWeatherForecasts.Request({
            forecastHours: 6,
          }),
      })
    ).then(rawResponse => {
      const response = new HourlyWeatherForecasts.Response(rawResponse.id, rawResponse.result);
      const now = new Date();
      this.currentHourlyForecast = response.forecast.find(forecast => isSameHour(forecast.datetime, now)) ?? null;
      this.upcomingHourlyForecasts = response.forecast.filter(forecast => isAfter(startOfHour(forecast.datetime), startOfHour(now)));
    }).catch(error => {
      console.warn("Error fetching weather forecast", error);
    });
  }

  private fetchDailyWeatherForecast() {
    this.edge.sendRequest<DailyWeatherForecasts.Response>(this.websocket,
      new ComponentJsonApiRequest({
        componentId: this.componentId,
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
}
