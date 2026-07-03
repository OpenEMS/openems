import { JsonrpcRequest, JsonrpcResponseSuccess } from "src/app/shared/jsonrpc/base";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { Edge, Websocket } from "src/app/shared/shared";

/**
 * Gets a 24h Schedule.
 *
 * <p>
 * Request:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getSchedule",
 *   "params": {
 *     "from": ZonedDateTime
 *   }
 * }
 * </pre>
 *
 * <p>
 * Response:
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     'data': [{
 *       ...
 *     }]
 *   }
 * }
 * </pre>
 */
export namespace GetSchedule {

    export const METHOD: string = "getSchedule";

    export class Request extends JsonrpcRequest {

        public constructor(
            public override readonly params: {
                from: string
            },
        ) {
            super(METHOD, params);
        }
    }

    export class Response extends JsonrpcResponseSuccess {

        public constructor(
            public override readonly id: string,
            public override readonly result: {
                data: {
                    timestamp: string,
                    type: string,
                    _sum: {
                        GridBuyPrice?: number,
                        GridSellPrice?: number,
                        ProductionActivePower?: number,
                        ConsumptionActivePower?: number,
                        UnmanagedConsumptionActivePower?: number,
                        EssDischargePower?: number,
                        GridActivePower?: number
                    },
                    eshs: {
                        id: string,
                        mode?: string,
                        managedConsumption?: number,
                    }[]
                }[]
            },
        ) {
            super(id, result);
        }
    }

    /**
     * Gets a 24h Schedule.
     *
     * @param edge       the edge
     * @param websocket  the websocket connection
     * @param from       the from date. Schedule starts 4 hours before this; ends 20 hours after this
     * @returns a Promise of GetSchedule.Response
     */
    export function getSchedule(edge: Edge, websocket: Websocket, from: Date)
        : Promise<GetSchedule.Response> {

        // Round down to next full 15-minutes
        from.setMinutes(Math.floor(from.getMinutes() / 15) * 15, 0, 0);

        return new Promise<GetSchedule.Response>((resolve, reject) => {
            edge.sendStateFullRequest(websocket,
                new ComponentJsonApiRequest({
                    componentId: "_energy",
                    payload: new GetSchedule.Request({
                        from: from.toISOString(),
                    }),
                }))
                .then(response => resolve(response as GetSchedule.Response))
                .catch(error => reject(error));
        });
    }
}
