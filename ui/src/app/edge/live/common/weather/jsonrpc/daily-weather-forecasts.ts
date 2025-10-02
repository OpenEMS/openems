import { JsonrpcRequest, JsonrpcResponse } from "src/app/shared/jsonrpc/base";
import { EmptyObj } from "src/app/shared/type/utility";

export namespace DailyWeatherForecasts {

    export interface Forecast {
        date: Date;
        weatherCode: number;
        minTemperature: number;
        maxTemperature: number;
        sunshineDuration: number;
    }

    export const METHOD: string = "dailyWeatherForecast";

    export class Request extends JsonrpcRequest {

        public constructor(public override readonly params: EmptyObj) {
            super(DailyWeatherForecasts.METHOD, params);
        }
    }

    export class Response extends JsonrpcResponse {

        public readonly forecast: Forecast[];

        public constructor(
            public override readonly id: string,
            public readonly result: { dailyWeatherForecast: Array<Omit<Forecast, "date"> & { date: string }> },
        ) {
            super(id);
            this.forecast = result.dailyWeatherForecast.map(item => ({
                ...item,
                date: new Date(item.date),
            }));
        }
    }
}
