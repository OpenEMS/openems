import { addDays, addHours, endOfToday, startOfToday, subHours, } from "date-fns";
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

    export type Data = {
        timestamp: Date;
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
    };

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

        public readonly lastHistoryIndex: number;

        public readonly data: Data[];
        public readonly data24h: Data[];
        public readonly data24hLastHistoryIndex: number;

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

            // Convert timestamp to Date
            this.data = this.result.data.map((e) => ({
                ...e,
                timestamp: new Date(e.timestamp),
            }));

            // Filter data within [-4 ; now ; +20] hours
            const now = new Date();
            const from = subHours(now, 4);
            const to = addHours(now, 20);

            this.data24h = this.data.filter((e) => {
                return e.timestamp >= from && e.timestamp < to;
            });

            // Provide index of last HISTORY data
            this.lastHistoryIndex = this.getLastHistoryIndex(this.data);
            this.data24hLastHistoryIndex = this.getLastHistoryIndex(
                this.data24h,
            );
        }

        public getLabels24h(): Date[] {
            return this.data24h.map((entry) => new Date(entry.timestamp));
        }

        public hasDataForChannel(channel: keyof Data["_sum"] | null): boolean {
            if (channel == null) {
                return false;
            }
            return this.data24h.some((e) => e._sum[channel] != null);
        }

        public summarizeData24hForChannel(channel: keyof Data["_sum"] | null) {
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

        /**
         * Calculates energy from power values over time intervals for today or
         * tomorrow. Energy (kWh) = Power (kW) × Time (hours)
         *
         * @param day Calculate for Today or Tomorrow
         * @param channel The power channel to convert to energy, or an ESH id
         * @returns Object with history energy (before now), prediction energy
         *   (after now), and total energy for the day (midnight to midnight)
         */
        public calculateEnergyFromPower(
            day: "today" | "tomorrow",
            channel: keyof Data["_sum"] | { eshsId: string },
        ): {
            history: number;
            prediction: number;
            total: number;
        } {
            const result = { history: 0, prediction: 0, total: 0 };
            const now = new Date();

            const startOfDayDate =
                day === "today" ? startOfToday() : addDays(startOfToday(), 1);
            const endOfDayDate =
                day === "today" ? endOfToday() : addDays(endOfToday(), 1);

            // Determine if this is an ESH id or a _sum channel
            const isEshsChannel = typeof channel === "object";
            const eshsId = isEshsChannel ? channel.eshsId : null;

            this.data.forEach((entry, index) => {
                // Only process entries within the requested day
                if (
                    entry.timestamp < startOfDayDate ||
                    entry.timestamp > endOfDayDate
                ) {
                    return;
                }

                if (index === 0) {
                    return; // Skip first entry (no previous interval)
                }

                const previousEntry = this.data[index - 1];

                // Skip if previous entry is before the requested day
                if (previousEntry.timestamp < startOfDayDate) {
                    return;
                }

                const timeDeltaMs =
                    entry.timestamp.getTime() -
                    previousEntry.timestamp.getTime();
                const timeDeltaHours = timeDeltaMs / (1000 * 60 * 60);

                // Get the current power value
                let currentPowerKw: number | null;
                if (isEshsChannel) {
                    // Extract managedConsumption from the matching ESHS
                    const eshs = entry.eshs.find((e) => e.id === eshsId);
                    currentPowerKw = this.convertByDataPoint(
                        null,
                        eshs?.managedConsumption ?? null,
                    );
                } else {
                    // Use _sum channel
                    currentPowerKw = this.convertByDataPoint(
                        channel,
                        entry._sum[channel as keyof Data["_sum"]] ?? null,
                    );
                }

                const energy = (currentPowerKw ?? 0) * timeDeltaHours;

                // Categorize as history (before now) or prediction (after now)
                if (entry.timestamp <= now) {
                    result.history += energy;
                } else {
                    result.prediction += energy;
                }
                result.total += energy;
            });

            return result;
        }

        private convertByDataPoint(
            key: keyof Data["_sum"] | null,
            value: number | null,
        ): number | null {
            switch (key) {
                case "EssSoc":
                    return value;
                case "GridBuyPrice":
                case "GridSellPrice":
                    return NumberUtils.divideSafely(value, 10);
                default:
                    return NumberUtils.divideSafely(value, 1000);
            }
        }

        private getLastHistoryIndex(data: Data[]): number {
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
