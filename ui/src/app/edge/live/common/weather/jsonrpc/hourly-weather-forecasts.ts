import { JsonrpcRequest, JsonrpcResponse } from "src/app/shared/jsonrpc/base";

export namespace HourlyWeatherForecasts {

    export interface Forecast {
        datetime: Date;
        weatherCode: number;
        temperature: number;
        isDay: boolean;
    }

    export const METHOD: string = "hourlyWeatherForecast";

    export class Request extends JsonrpcRequest {

        public constructor(
            public override readonly params: {
                forecastHours: number,
            }
        ) {
            super(HourlyWeatherForecasts.METHOD, params);
        }
    }

    export class Response extends JsonrpcResponse {

        public readonly forecast: Forecast[];

        public constructor(
            public override readonly id: string,
            public readonly result: { hourlyWeatherForecast: Array<Omit<Forecast, "date"> & { date: string }> }
        ) {
            super(id);
            this.forecast = result.hourlyWeatherForecast.map(item => ({
                ...item,
                datetime: new Date(item.datetime),
            }));
        }
    }
}
