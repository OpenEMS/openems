import { Mode } from "src/app/edge/live/Controller/Evse/pages/chargemode/chargemode";
import { JsonrpcRequest } from "src/app/shared/jsonrpc/base";

/**
 * Wraps a JSON-RPC Request for an OpenEMS Component that implements JsonApi
 *
 * <pre>
 * {
 *   "method": "updateTask",
 *   "params": {
 *      "task": ,
 * }
 * </pre>
 */
export namespace UpdateTask {

    export const METHOD: string = "updateTask";

    export class Request extends JsonrpcRequest {

        public constructor(
            public override readonly params: {
                task: Task,
            },
        ) {
            super(METHOD, params);
        }
    }

    export interface Task {
        "@type": "Task",
        "uid": string,
        "start": string,
        "duration": string,
        "recurrenceRules": any[]
        "openems.io:payload": {
            "class": "Manual",
            "mode": Mode,
        },
    }
}
