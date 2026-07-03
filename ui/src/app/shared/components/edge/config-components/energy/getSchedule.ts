import { JsonrpcRequest, JsonrpcResponseSuccess, } from "src/app/shared/jsonrpc/base";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { Edge, Websocket } from "src/app/shared/shared";
import { NumberUtils } from "src/app/shared/utils/number/number-utils";

/**
 * Gets a 24h Schedule.
 *
 * @typedef {{
 *     jsonrpc: "2.0";
 *     id: "UUID";
 *     method: "getSchedule";
 *     params: { from: ZonedDateTime };
 * }} Request
 *
 *
 * @typedef {{
 *     jsonrpc: "2.0";
 *     id: "UUID";
 *     result: {
 *         data: [{}];
 *     };
 * }} Response
 */
export namespace GetSchedule {
    export const METHOD: string = "getSchedule";

    export class Request extends JsonrpcRequest {
        public constructor(
            public override readonly params: {
                from: string;
            },
        ) {
            super(METHOD, params);
        }
    }

    export class Response extends JsonrpcResponseSuccess {
        public static readonly empty: Response = new Response("", { data: [] });

        public readonly lastHistoryIndex;

        public constructor(
            public override readonly id: string,
            public override readonly result: {
                data: {
                    timestamp: string;
                    type: string;
                    _sum: {
                        GridBuyPrice?: number;
                        GridSellPrice?: number;
                        ProductionActivePower?: number;
                        ConsumptionActivePower?: number;
                        UnmanagedConsumptionActivePower?: number;
                        EssDischargePower?: number;
                        GridActivePower?: number;
                        EssSoc?: number;
                    };
                    eshs: {
                        id: string;
                        mode?: string;
                        managedConsumption?: number;
                    }[];
                }[];
            },
        ) {
            super(id, result);
            this.lastHistoryIndex =
                this.result.data.length -
                1 -
                [...this.result.data]
                    .reverse()
                    .findIndex((e) => e.type === "HISTORY");
        }

        public getLabels(): Date[] {
            return this.result.data.map((entry) => new Date(entry.timestamp));
        }

        public summarizeDataForChannel(
            channel: keyof Response["result"]["data"][number]["_sum"] | null,
        ) {
            if (channel == null) {
                throw new Error("Key must not be null");
            }

            const entries = this.result.data.map((e) => ({
                entry: e,
                timestamp: new Date(e.timestamp),
                value: this.convertByDataPoint(
                    channel,
                    e._sum[channel] ?? null,
                ),
            }));
            const labels = entries.map((e) => e.timestamp);

            // Fill history and prediction arrays. Both share a value at lastHistoryIndex to avoid gaps in the chart line.
            const history = entries.map((e, index) =>
                index <= this.lastHistoryIndex ? e.value : null,
            );
            const prediction = entries.map((e, index) =>
                index >= this.lastHistoryIndex ? e.value : null,
            );

            return {
                labels,
                history,
                prediction,
            };
        }

        private convertByDataPoint(
            key: keyof Response["result"]["data"][number]["_sum"] | null,
            value: number | null,
        ): number | null {
            switch (key) {
                case "EssSoc":
                    return value;
                default:
                    return NumberUtils.divideSafely(value, 1000);
            }
        }
    }

    /**
     * Gets a 24h Schedule.
     *
     * @param edge The edge
     * @param websocket The websocket connection
     * @param from The from date. Schedule starts 4 hours before this; ends 20
     *   hours after this
     * @returns A Promise of GetSchedule.Response
     */
    export function getSchedule(
        edge: Edge,
        websocket: Websocket,
        from: Date,
    ): Promise<GetSchedule.Response> {
        // Round down to next full 15-minutes
        from.setMinutes(Math.floor(from.getMinutes() / 15) * 15, 0, 0);

        return new Promise<GetSchedule.Response>((resolve, reject) => {
            edge.sendStateFullRequest<GetSchedule.Response>(
                websocket,
                new ComponentJsonApiRequest({
                    componentId: "_energy",
                    payload: new GetSchedule.Request({
                        from: from.toISOString(),
                    }),
                }),
            )
                .then((response) =>
                    resolve(
                        new GetSchedule.Response(response.id, response.result),
                    ),
                )
                .catch((error) => reject(error));
        });
    }
}
