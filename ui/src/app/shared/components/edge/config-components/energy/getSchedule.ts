import { addHours, subHours } from "date-fns";
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

        public readonly data24h;
        public readonly data24hLastHistoryIndex;

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
                        mode?: number;
                        managedConsumption?: number;
                    }[];
                }[];
            },
        ) {
            super(id, result);

            // Filter data within [-4 ; now ; +20] hours
            const now = new Date();
            const from = subHours(now, 4);
            const to = addHours(now, 20);
            this.data24h = this.result.data.filter((entry) => {
                const timestamp = new Date(entry.timestamp);
                return timestamp >= from && timestamp < to;
            });

            // Provide index of last HISTORY data
            this.lastHistoryIndex = this.getLastHistoryIndex(this.result.data);
            this.data24hLastHistoryIndex = this.getLastHistoryIndex(
                this.data24h,
            );
        }

        public getLabels24h(): Date[] {
            return this.data24h.map((entry) => new Date(entry.timestamp));
        }

        public summarizeData24hForChannel(
            channel: keyof Response["result"]["data"][number]["_sum"] | null,
        ) {
            if (channel == null) {
                throw new Error("Key must not be null");
            }

            const entries = this.data24h.map((e) => ({
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
                index <= this.data24hLastHistoryIndex ? e.value : null,
            );
            const prediction = entries.map((e, index) =>
                index >= this.data24hLastHistoryIndex ? e.value : null,
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

        private getLastHistoryIndex(data: Response["result"]["data"]): number {
            const reversedIndex = [...data]
                .reverse()
                .findIndex((e) => e.type === "HISTORY");

            return reversedIndex === -1 ? -1 : data.length - 1 - reversedIndex;
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
